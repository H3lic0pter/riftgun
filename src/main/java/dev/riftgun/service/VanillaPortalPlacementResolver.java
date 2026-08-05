package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPairPlacement;
import dev.riftgun.portal.PortalPlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class VanillaPortalPlacementResolver implements PortalPlacementResolver {
    private static final double SURFACE_OFFSET = PortalPlacement.DEPTH * 0.5 + 0.002;

    @Override
    public PortalPlacementCapture capture(ServerPlayer player, PortalPlacementMode mode, int smartDistance) {
        EntryResult entry = switch (mode) {
            case FRONT -> EntryResult.frontRoute();
            case SURFACE -> surface(player, false, smartDistance);
            case SMART -> surface(player, true, smartDistance);
        };
        if (entry.front) return PortalPlacementCapture.success(PortalPlacementIntent.front());
        return entry.placement == null
            ? PortalPlacementCapture.failure(entry.errorKey)
            : PortalPlacementCapture.success(PortalPlacementIntent.surface(entry.placement));
    }

    @Override
    public PortalPlacementResult resolvePrepared(ServerPlayer player, Destination destination,
                                                 PortalPlacementIntent intent) {
        MinecraftServer server = player.getServer();
        if (server == null || server.getLevel(destination.dimension()) == null) {
            return PortalPlacementResult.failure("message.riftgun.dimension_unavailable");
        }

        EntryResult entry = intent.route() == PortalPlacementIntent.Route.FRONT
            ? front(player)
            : revalidateSurface(player, intent.attachedPlacement());
        if (entry.placement == null) return PortalPlacementResult.failure(entry.errorKey);

        ServerLevel targetLevel = server.getLevel(destination.dimension());
        PortalPlacement exit = resolveExit(targetLevel, destination, entry.placement);
        return PortalPlacementResult.success(new PortalPairPlacement(destination.dimension(), entry.placement, exit));
    }

    private EntryResult front(ServerPlayer player) {
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 normal = look.scale(-1.0);
        PortalGeometry geometry = PortalGeometry.FLOATING_VERTICAL;
        Vec3 center = player.position()
            .add(look.scale(PortalServices.PLACEMENT_CAPABILITIES.frontDistance(player)))
            .add(0.0, geometry.height() * 0.5, 0.0);
        PortalPlacement placement = new PortalPlacement(center, PortalOrientation.VERTICAL, geometry,
            yawFromNormal(normal), null, null);
        return blocked(player.serverLevel(), placement.bounds())
            ? EntryResult.failure("message.riftgun.front_obstructed") : EntryResult.success(placement);
    }

    private EntryResult surface(ServerPlayer player, boolean smart, int requestedSmartDistance) {
        double maximumRange = PortalServices.PLACEMENT_CAPABILITIES.maximumSurfaceRange(player);
        double rayRange = smart ? maximumRange : maximumRange + 16.0;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(rayRange));
        HitResult raw = player.serverLevel().clip(new ClipContext(
            eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (!(raw instanceof BlockHitResult hit) || raw.getType() != HitResult.Type.BLOCK) {
            return smart ? EntryResult.frontRoute() : EntryResult.failure("message.riftgun.surface_missing");
        }

        double distance = eye.distanceTo(hit.getLocation());
        if (smart && distance > Math.min(requestedSmartDistance, maximumRange)) return EntryResult.frontRoute();
        if (distance > maximumRange) return EntryResult.failure("message.riftgun.surface_out_of_range");
        return attached(player.serverLevel(), player, hit);
    }

    private EntryResult revalidateSurface(ServerPlayer player, PortalPlacement placement) {
        if (placement == null || placement.anchor() == null || placement.anchorFace() == null) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }
        ServerLevel level = player.serverLevel();
        BlockPos anchor = placement.anchor();
        if (level.getBlockState(anchor).getCollisionShape(level, anchor).isEmpty()) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }
        double range = PortalServices.PLACEMENT_CAPABILITIES.maximumSurfaceRange(player) + 1.5;
        if (player.getEyePosition().distanceTo(placement.center()) > range) {
            return EntryResult.failure("message.riftgun.surface_out_of_range");
        }
        return blocked(level, placement.bounds())
            ? EntryResult.failure("message.riftgun.surface_obstructed")
            : EntryResult.success(placement);
    }

    private EntryResult attached(ServerLevel level, ServerPlayer player, BlockHitResult hit) {
        BlockPos anchor = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockState state = level.getBlockState(anchor);
        if (state.getCollisionShape(level, anchor).isEmpty()) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }

        if (face.getAxis().isVertical()) {
            PortalOrientation orientation = face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM;
            Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
            Vec3 center = Vec3.atCenterOf(anchor).add(normal.scale(0.5 + SURFACE_OFFSET));
            PortalPlacement placement = new PortalPlacement(center, orientation, PortalGeometry.HORIZONTAL,
                player.getYRot(), anchor.immutable(), face);
            return blocked(level, placement.bounds())
                ? EntryResult.failure("message.riftgun.surface_obstructed") : EntryResult.success(placement);
        }

        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        double x = anchor.getX() + 0.5 + normal.x * (0.5 + SURFACE_OFFSET);
        double z = anchor.getZ() + 0.5 + normal.z * (0.5 + SURFACE_OFFSET);
        float yaw = yawFromNormal(normal);
        List<SidePortalCandidateSelector.Candidate> candidates = new ArrayList<>(2);
        PortalPlacement hitAndAbove = new PortalPlacement(new Vec3(x, anchor.getY() + 1.0, z),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL, yaw, anchor.immutable(), face);
        PortalPlacement belowAndHit = new PortalPlacement(new Vec3(x, anchor.getY(), z),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL, yaw, anchor.immutable(), face);
        if (!blocked(level, hitAndAbove.bounds())) {
            candidates.add(new SidePortalCandidateSelector.Candidate(hitAndAbove,
                1 + backingBlock(level, anchor.above())));
        }
        if (!blocked(level, belowAndHit.bounds())) {
            candidates.add(new SidePortalCandidateSelector.Candidate(belowAndHit,
                1 + backingBlock(level, anchor.below())));
        }
        if (!candidates.isEmpty()) {
            return EntryResult.success(SidePortalCandidateSelector.choose(candidates, player.getBoundingBox()));
        }

        PortalPlacement compact = new PortalPlacement(new Vec3(x, anchor.getY() + 0.5, z),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_COMPACT, yaw, anchor.immutable(), face);
        return blocked(level, compact.bounds())
            ? EntryResult.failure("message.riftgun.surface_obstructed") : EntryResult.success(compact);
    }

    private PortalPlacement resolveExit(ServerLevel level, Destination destination, PortalPlacement entry) {
        return switch (entry.orientation().oppositeSurface()) {
            case TOP -> horizontalTopExit(level, destination);
            case BOTTOM -> horizontalBottomExit(level, destination);
            case VERTICAL -> verticalExit(destination,
                entry.geometry() == PortalGeometry.FLOATING_VERTICAL
                    ? PortalGeometry.FLOATING_VERTICAL : PortalGeometry.SURFACE_VERTICAL);
        };
    }

    private PortalPlacement horizontalTopExit(ServerLevel level, Destination destination) {
        BlockPos support = BlockPos.containing(destination.x(), destination.y() - 0.01, destination.z());
        if (!level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) {
            return verticalExit(destination, PortalGeometry.SURFACE_VERTICAL);
        }
        Vec3 center = new Vec3(support.getX() + 0.5, support.getY() + 1.0 + SURFACE_OFFSET,
            support.getZ() + 0.5);
        PortalPlacement placement = new PortalPlacement(center, PortalOrientation.TOP, PortalGeometry.HORIZONTAL,
            destination.yaw(), null, null);
        return blocked(level, placement.bounds())
            ? verticalExit(destination, PortalGeometry.SURFACE_VERTICAL) : placement;
    }

    private PortalPlacement horizontalBottomExit(ServerLevel level, Destination destination) {
        Vec3 center = destination.position().add(0.0, 3.0, 0.0);
        PortalPlacement placement = new PortalPlacement(center, PortalOrientation.BOTTOM, PortalGeometry.HORIZONTAL,
            destination.yaw(), null, null);
        return blocked(level, placement.bounds())
            ? verticalExit(destination, PortalGeometry.SURFACE_VERTICAL) : placement;
    }

    private PortalPlacement verticalExit(Destination destination, PortalGeometry geometry) {
        Vec3 normal = Vec3.directionFromRotation(0.0F, destination.yaw()).normalize();
        Vec3 center = destination.position().subtract(normal.scale(0.85))
            .add(0.0, geometry.height() * 0.5, 0.0);
        return new PortalPlacement(center, PortalOrientation.VERTICAL, geometry,
            destination.yaw(), null, null);
    }

    private static boolean blocked(ServerLevel level, AABB bounds) {
        return level.getBlockCollisions(null, bounds.deflate(0.002)).iterator().hasNext();
    }

    private static int backingBlock(ServerLevel level, BlockPos position) {
        return level.getBlockState(position).getCollisionShape(level, position).isEmpty() ? 0 : 1;
    }

    private static float yawFromNormal(Vec3 normal) {
        return (float) Math.toDegrees(Math.atan2(-normal.x, normal.z));
    }

    private record EntryResult(PortalPlacement placement, String errorKey, boolean front) {
        static EntryResult success(PortalPlacement placement) {
            return new EntryResult(placement, null, false);
        }

        static EntryResult failure(String errorKey) {
            return new EntryResult(null, errorKey, false);
        }

        static EntryResult frontRoute() {
            return new EntryResult(null, null, true);
        }
    }
}
