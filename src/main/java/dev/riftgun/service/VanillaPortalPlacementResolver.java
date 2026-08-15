package dev.riftgun.service;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPairPlacement;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalLifecycle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public final class VanillaPortalPlacementResolver implements PortalPlacementResolver {
    private static final double SURFACE_OFFSET = PortalPlacement.DEPTH * 0.5 + 0.002;
    /** Blocks of extra door distance per block/second of velocity projected on the door axis. */
    private static final double MAXIMUM_PROJECTION_EXTRA = 55.0;
    private static final double TICKS_PER_SECOND = 20.0;

    @Override
    public PortalPlacementCapture capture(ServerPlayer player, PortalPlacementMode mode,
                                          PortalPlacementConstraints constraints) {
        EntryResult entry = switch (mode) {
            case FRONT -> EntryResult.frontRoute();
            case SURFACE -> surface(player, false, constraints.smartDistance(),
                constraints.maximumSurfaceRange(), constraints.aperture());
            case SMART -> surface(player, true, constraints.smartDistance(),
                constraints.maximumSurfaceRange(), constraints.aperture());
            case ENTITY_RELOCATION -> EntryResult.failure("message.riftgun.entity_relocation_target_required");
        };
        if (entry.front) return PortalPlacementCapture.success(
            PortalPlacementIntent.front(constraints.predictionMode()));
        return entry.placement == null
            ? PortalPlacementCapture.failure(entry.errorKey)
            : PortalPlacementCapture.success(PortalPlacementIntent.surface(entry.placement));
    }

    @Override
    public PortalEntryPlacementResult resolveEntry(ServerPlayer player, PortalPlacementIntent intent,
                                                   PortalPlacementConstraints constraints) {
        EntryResult entry = intent.route() == PortalPlacementIntent.Route.FRONT
            ? front(player, intent.predictionMode(), constraints)
            : revalidateSurface(player, intent.attachedPlacement(), constraints.maximumSurfaceRange());
        return entry.placement == null
            ? PortalEntryPlacementResult.failure(entry.errorKey)
            : PortalEntryPlacementResult.success(entry.placement);
    }

    @Override
    public PortalPlacementResult resolveExitPrepared(ServerLevel targetLevel, PortalExitTarget target,
                                                     PortalPlacement entry, PortalAperture aperture) {
        PortalPlacement exit = resolveExit(targetLevel, target, entry, aperture);
        return PortalPlacementResult.success(new PortalPairPlacement(target.dimension(), entry, exit));
    }

    private EntryResult front(ServerPlayer player, PortalPredictionMode mode,
                              PortalPlacementConstraints constraints) {
        PortalAperture aperture = constraints.aperture();
        boolean downShot = usesDownshot(player.getXRot(),
            RiftRuntime.current().placementCapabilities().downshotMinimumPitch(player));
        PortalMotionPredictor.Purpose purpose = downShot
            ? PortalMotionPredictor.Purpose.DOWN_SHOT : PortalMotionPredictor.Purpose.FRONT;
        boolean trajectory = mode == PortalPredictionMode.TRAJECTORY;
        Vec3 prediction = trajectory ? predictedDisplacement(player, purpose) : Vec3.ZERO;
        List<Vec3> positions = trajectory && prediction.lengthSqr() >= 1.0E-8
            ? List.of(prediction, Vec3.ZERO) : List.of(prediction);
        double frontDistance = RiftRuntime.current().placementCapabilities().frontDistance(player);
        double downshotDistance = RiftRuntime.current().placementCapabilities().downshotDistance(player);
        if (mode == PortalPredictionMode.PROJECTION) {
            double extra = downShot
                ? projectionExtra(player, downshotProjectionAxis(),
                    constraints.downshotProjectionFactor())
                : projectionExtra(player, frontProjectionAxis(player),
                    constraints.frontProjectionFactor());
            frontDistance += extra;
            downshotDistance += extra;
        }
        EntryResult last = null;
        for (Vec3 displacement : positions) {
            if (PortalAperturePolicy.expanded(aperture)) {
                EntryResult expanded = downShot
                    ? downshot(player, displacement, downshotDistance, PortalAperturePolicy.horizontal(),
                        PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE)
                    : verticalFront(player, displacement, frontDistance, PortalAperturePolicy.floatingVertical(),
                        PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE);
                if (expanded.placement != null) return expanded;
                last = expanded;
            }
            EntryResult standard = downShot
                ? downshot(player, displacement, downshotDistance, PortalGeometry.HORIZONTAL,
                    RiftRuntime.current().placementCapabilities().minimumFloatingPortalExposure(player))
                : verticalFront(player, displacement, frontDistance, PortalGeometry.FLOATING_VERTICAL,
                    RiftRuntime.current().placementCapabilities().minimumFloatingPortalExposure(player));
            if (standard.placement != null) return standard;
            last = standard;
        }
        return last == null ? EntryResult.failure("message.riftgun.front_obstructed") : last;
    }

    /**
     * Distance added to the door when PROJECTION mode is active. Uses the sampled recent
     * velocity (blocks/tick scaled to per-second) so doors opened from the modal GUI still
     * see the player's movement right before opening. Falls back to instantaneous velocity.
     * The factor is per door type: front uses the view axis factor, downshot the vertical one.
     */
    private static double projectionExtra(ServerPlayer player, Vec3 axis, double factor) {
        Vec3 velocity = RiftRuntime.current().motionHistory().recentVelocity(player)
            .orElse(player.getDeltaMovement());
        double projection = velocity.dot(axis) * TICKS_PER_SECOND;
        return Mth.clamp(projection * factor, 0.0, MAXIMUM_PROJECTION_EXTRA);
    }

    /** Projection axis for the downshot door: straight down in world coordinates. */
    private static Vec3 downshotProjectionAxis() {
        return new Vec3(0.0, -1.0, 0.0);
    }

    /** Projection axis for the front door: the view heading in the xz plane. */
    private static Vec3 frontProjectionAxis(ServerPlayer player) {
        return Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
    }

    private EntryResult verticalFront(ServerPlayer player, Vec3 prediction, double frontDistance,
                                      PortalGeometry geometry, double minimumExposure) {
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
        Vec3 normal = look.scale(-1.0);
        Vec3 center = player.position()
            .add(prediction)
            .add(look.scale(frontDistance))
            .add(0.0, geometry.height() * 0.5, 0.0);
        PortalPlacement placement = new PortalPlacement(center, PortalOrientation.VERTICAL, geometry,
            yawFromNormal(normal), null, null);
//? if >=1.21.11 {
        /*return !FloatingPortalBounds.allows(placement.bounds(), ((ServerLevel) player.level()).dimensionType().minY())
        *///?} else {
        return !FloatingPortalBounds.allows(placement.bounds(), player.serverLevel().getMinBuildHeight())
        //?}
            ? EntryResult.failure("message.riftgun.void_portal_too_late")
//? if >=1.21.11 {
            /*: floatingObstructed((ServerLevel) player.level(), placement, minimumExposure)
*///?} else {
            : floatingObstructed(player.serverLevel(), placement, minimumExposure)
//?}
            ? EntryResult.failure("message.riftgun.front_obstructed") : EntryResult.success(placement);
    }

    private EntryResult downshot(ServerPlayer player, Vec3 prediction, double downshotDistance,
                                 PortalGeometry geometry, double minimumExposure) {
        Vec3 center = player.position().add(prediction)
            .add(0.0, -downshotDistance, 0.0);
        PortalPlacement placement = new PortalPlacement(center, PortalOrientation.TOP,
            geometry, player.getYRot(), null, null);
//? if >=1.21.11 {
        /*return !FloatingPortalBounds.allows(placement.bounds(), ((ServerLevel) player.level()).dimensionType().minY())
        *///?} else {
        return !FloatingPortalBounds.allows(placement.bounds(), player.serverLevel().getMinBuildHeight())
        //?}
            ? EntryResult.failure("message.riftgun.void_portal_too_late")
//? if >=1.21.11 {
            /*: floatingObstructed((ServerLevel) player.level(), placement, minimumExposure)
*///?} else {
            : floatingObstructed(player.serverLevel(), placement, minimumExposure)
//?}
            ? EntryResult.failure("message.riftgun.front_obstructed") : EntryResult.success(placement);
    }

    private Vec3 predictedDisplacement(ServerPlayer player, PortalMotionPredictor.Purpose purpose) {
        int ticks = PortalLifecycle.CHARGE_TICKS + PortalLifecycle.ANIMATION_TICKS;
        return RiftRuntime.current().motionPredictor().predictDisplacement(player, purpose, ticks,
            RiftRuntime.current().placementCapabilities().maximumHorizontalPrediction(player));
    }

    static boolean usesDownshot(float pitch, float minimumPitch) {
        return pitch >= minimumPitch;
    }

    private EntryResult surface(ServerPlayer player, boolean smart, int requestedSmartDistance,
                                double maximumRange, PortalAperture aperture) {
        double rayRange = smart ? maximumRange : maximumRange + 16.0;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(rayRange));
//? if >=1.21.11 {
        /*HitResult raw = ((ServerLevel) player.level()).clip(new ClipContext(
*///?} else {
        HitResult raw = player.serverLevel().clip(new ClipContext(
//?}
            eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (!(raw instanceof BlockHitResult hit) || raw.getType() != HitResult.Type.BLOCK) {
            return smart ? EntryResult.frontRoute() : EntryResult.failure("message.riftgun.surface_missing");
        }

        double distance = eye.distanceTo(hit.getLocation());
        if (smart && distance > Math.min(requestedSmartDistance, maximumRange)) return EntryResult.frontRoute();
        if (distance > maximumRange) return EntryResult.failure("message.riftgun.surface_out_of_range");
//? if >=1.21.11 {
        /*return attached((ServerLevel) player.level(), player, hit, aperture);
*///?} else {
        return attached(player.serverLevel(), player, hit, aperture);
//?}
    }

    private EntryResult revalidateSurface(ServerPlayer player, PortalPlacement placement, double maximumRange) {
        if (placement == null || placement.anchor() == null || placement.anchorFace() == null) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }
//? if >=1.21.11 {
        /*ServerLevel level = (ServerLevel) player.level();
*///?} else {
        ServerLevel level = player.serverLevel();
//?}
        BlockPos anchor = placement.anchor();
        if (level.getBlockState(anchor).getCollisionShape(level, anchor).isEmpty()) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }
        double range = maximumRange + 1.5;
        if (player.getEyePosition().distanceTo(placement.center()) > range) {
            return EntryResult.failure("message.riftgun.surface_out_of_range");
        }
        if (placement.geometry().expanded()
            && !PortalSupportArea.hasFullExpandedSupport(level, placement)) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }
        return blocked(level, placement.bounds())
            ? EntryResult.failure("message.riftgun.surface_obstructed")
            : EntryResult.success(placement);
    }

    private EntryResult attached(ServerLevel level, ServerPlayer player, BlockHitResult hit,
                                 PortalAperture aperture) {
        BlockPos anchor = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockState state = level.getBlockState(anchor);
        if (state.getCollisionShape(level, anchor).isEmpty()) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }

        if (PortalAperturePolicy.expanded(aperture)) {
            PortalPlacement expanded = face.getAxis().isVertical()
                ? expandedHorizontalAttached(level, player, hit)
                : expandedVerticalAttached(level, player, hit);
            if (expanded != null) return EntryResult.success(expanded);
        }

        if (face.getAxis().isVertical()) {
            PortalOrientation orientation = face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM;
//? if >=1.21.11 {
            /*Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
*///?} else {
            Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
//?}
            Vec3 center = Vec3.atCenterOf(anchor).add(normal.scale(0.5 + SURFACE_OFFSET));
            PortalPlacement placement = new PortalPlacement(center, orientation, PortalGeometry.HORIZONTAL,
                player.getYRot(), anchor.immutable(), face);
            return blocked(level, placement.bounds())
                ? EntryResult.failure("message.riftgun.surface_obstructed") : EntryResult.success(placement);
        }

//? if >=1.21.11 {
        /*Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
*///?} else {
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
//?}
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

    private PortalPlacement expandedVerticalAttached(ServerLevel level, ServerPlayer player,
                                                      BlockHitResult hit) {
        BlockPos hitBlock = hit.getBlockPos();
        Direction face = hit.getDirection();
        Direction lateral = face.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
//? if >=1.21.11 {
        /*Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
*///?} else {
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
//?}
//? if >=1.21.11 {
        /*Vec3 lateralVector = new Vec3(lateral.getStepX(), lateral.getStepY(), lateral.getStepZ());
*///?} else {
        Vec3 lateralVector = new Vec3(lateral.getStepX(), lateral.getStepY(), lateral.getStepZ());
//?}
        float yaw = yawFromNormal(normal);
        List<PortalPlacement> candidates = new ArrayList<>(4);
        for (int lateralOffset = -1; lateralOffset <= 0; lateralOffset++) {
            for (int verticalOffset = -1; verticalOffset <= 0; verticalOffset++) {
                BlockPos origin = hitBlock.relative(lateral, lateralOffset).offset(0, verticalOffset, 0);
                Vec3 center = Vec3.atCenterOf(origin)
                    .add(lateralVector.scale(0.5))
                    .add(0.0, 0.5, 0.0)
                    .add(normal.scale(0.5 + SURFACE_OFFSET));
                PortalPlacement placement = new PortalPlacement(center, PortalOrientation.VERTICAL,
                    PortalAperturePolicy.attachedVertical(), yaw, origin.immutable(), face);
                if (PortalSupportArea.hasFullExpandedSupport(level, placement)
                    && !blocked(level, placement.bounds())) {
                    candidates.add(placement);
                }
            }
        }
        return candidates.isEmpty() ? null : ExpandedPortalCandidateSelector.choose(
            candidates, hit.getLocation(), player.getBoundingBox().getCenter());
    }

    private PortalPlacement expandedHorizontalAttached(ServerLevel level, ServerPlayer player,
                                                        BlockHitResult hit) {
        List<PortalPlacement> candidates = expandedHorizontalCandidates(level, hit.getBlockPos(),
            hit.getDirection(), player.getYRot());
        return candidates.isEmpty() ? null : ExpandedPortalCandidateSelector.choose(
            candidates, hit.getLocation(), player.getBoundingBox().getCenter());
    }

    private List<PortalPlacement> expandedHorizontalCandidates(ServerLevel level, BlockPos hitBlock,
                                                               Direction face, float yaw) {
        List<PortalPlacement> candidates = new ArrayList<>(4);
        PortalOrientation orientation = face == Direction.UP ? PortalOrientation.TOP : PortalOrientation.BOTTOM;
//? if >=1.21.11 {
        /*Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
*///?} else {
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
//?}
        for (int xOffset = -1; xOffset <= 0; xOffset++) {
            for (int zOffset = -1; zOffset <= 0; zOffset++) {
                BlockPos origin = hitBlock.offset(xOffset, 0, zOffset);
                Vec3 center = Vec3.atCenterOf(origin)
                    .add(0.5, 0.0, 0.5)
                    .add(normal.scale(0.5 + SURFACE_OFFSET));
                PortalPlacement placement = new PortalPlacement(center, orientation,
                    PortalAperturePolicy.horizontal(), yaw, origin.immutable(), face);
                if (PortalSupportArea.hasFullExpandedSupport(level, placement)
                    && !blocked(level, placement.bounds())) {
                    candidates.add(placement);
                }
            }
        }
        return candidates;
    }

    private PortalPlacement resolveExit(ServerLevel level, PortalExitTarget destination,
                                        PortalPlacement entry, PortalAperture aperture) {
        return switch (entry.orientation().oppositeSurface()) {
            case TOP -> PortalExitPlacementPolicy.resolveHorizontal(
                destination, PortalOrientation.TOP, aperture, exitSpace(level));
            case BOTTOM -> PortalExitPlacementPolicy.resolveHorizontal(
                destination, PortalOrientation.BOTTOM, aperture, exitSpace(level));
            case VERTICAL -> verticalExit(level, destination, entry, aperture);
        };
    }

    private PortalExitPlacementPolicy.SpaceProbe exitSpace(ServerLevel level) {
        return new PortalExitPlacementPolicy.SpaceProbe() {
            @Override
            public boolean available(PortalPlacement placement) {
                return !outsideWorld(level, placement.bounds()) && !blocked(level, placement.bounds());
            }

            @Override
            public boolean hasTopSupport(BlockPos support) {
                return level.getBlockState(support).isFaceSturdy(level, support, Direction.UP);
            }
        };
    }

    private PortalPlacement verticalExit(PortalExitTarget destination, PortalGeometry geometry) {
        Vec3 normal = Vec3.directionFromRotation(0.0F, destination.yaw()).normalize();
        Vec3 center = destination.position().subtract(normal.scale(0.85))
            .add(0.0, geometry.height() * 0.5, 0.0);
        return new PortalPlacement(center, PortalOrientation.VERTICAL, geometry,
            destination.yaw(), null, null);
    }

    private PortalPlacement verticalExit(ServerLevel level, PortalExitTarget destination,
                                         PortalPlacement entry, PortalAperture aperture) {
        boolean floating = entry.geometry() == PortalGeometry.FLOATING_VERTICAL
            || entry.geometry() == PortalGeometry.FLOATING_EXPANDED;
        if (PortalAperturePolicy.expanded(aperture)) {
            PortalGeometry expandedGeometry = floating
                ? PortalGeometry.FLOATING_EXPANDED : PortalGeometry.SURFACE_EXPANDED;
            PortalPlacement expanded = verticalExit(destination, expandedGeometry);
            if (!outsideWorld(level, expanded.bounds())
                && !floatingObstructed(level, expanded, PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE)) {
                return expanded;
            }
        }
        return verticalExit(destination,
            floating ? PortalGeometry.FLOATING_VERTICAL : PortalGeometry.SURFACE_VERTICAL);
    }

    private static boolean blocked(ServerLevel level, AABB bounds) {
        return level.getBlockCollisions(null, bounds.deflate(0.002)).iterator().hasNext();
    }

    private static boolean floatingObstructed(ServerLevel level, PortalPlacement placement,
                                              double minimumExposure) {
        return !PortalFaceExposure.hasMinimumExposure(level, placement, minimumExposure);
    }

    private static boolean outsideWorld(ServerLevel level, AABB bounds) {
        //? if >=1.21.11 {
        /*return bounds.minY < level.dimensionType().minY() || bounds.maxY > level.dimensionType().minY() + level.dimensionType().height();
        *///?} else {
        return bounds.minY < level.getMinBuildHeight() || bounds.maxY > level.getMaxBuildHeight();
        //?}
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
