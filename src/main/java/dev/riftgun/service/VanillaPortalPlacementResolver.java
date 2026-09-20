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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
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
            case REMOTE -> remote(player, constraints.remoteDistance(), constraints.aperture(),
                constraints.floatingOrientation());
            case SURFACE -> surface(player, false, constraints.smartDistance(),
                constraints.maximumSurfaceRange(), constraints.aperture());
            case SMART -> surface(player, true, constraints.smartDistance(),
                constraints.maximumSurfaceRange(), constraints.remoteDistance(),
                constraints.aperture(), constraints.smartFallback());
            case ENTITY_RELOCATION -> EntryResult.failure("message.riftgun.entity_relocation_target_required");
        };
        if (entry.front) return PortalPlacementCapture.success(
            PortalPlacementIntent.front(constraints.predictionMode()));
        if (entry.placement == null) return PortalPlacementCapture.failure(entry.errorKey);
        return PortalPlacementCapture.success(entry.placement.anchored()
            ? PortalPlacementIntent.surface(entry.placement)
            : PortalPlacementIntent.remote(entry.placement));
    }

    @Override
    public PortalEntryPlacementResult resolveEntry(ServerPlayer player, PortalPlacementIntent intent,
                                                   PortalPlacementConstraints constraints) {
        EntryResult entry = switch (intent.route()) {
            case FRONT -> front(player, intent.predictionMode(), constraints);
            case REMOTE -> revalidateRemote(player, intent.attachedPlacement(), constraints.remoteDistance());
            case SURFACE -> revalidateSurface(player, intent.attachedPlacement(), constraints.maximumSurfaceRange());
        };
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
        PortalOrientation horizontal = constraints.floatingOrientation() == null
            ? horizontalOrientation(player.getXRot(),
                RiftRuntime.current().placementCapabilities().downshotMinimumPitch(player))
            : constraints.floatingOrientation();
        boolean horizontalDoor = horizontal != PortalOrientation.VERTICAL;
        PortalMotionPredictor.Purpose purpose = horizontalDoor
            ? PortalMotionPredictor.Purpose.HORIZONTAL : PortalMotionPredictor.Purpose.FRONT;
        boolean trajectory = mode == PortalPredictionMode.TRAJECTORY;
        Vec3 prediction = trajectory ? predictedDisplacement(player, purpose) : Vec3.ZERO;
        List<Vec3> positions = trajectory && prediction.lengthSqr() >= 1.0E-8
            ? List.of(prediction, Vec3.ZERO) : List.of(prediction);
        double frontDistance = RiftRuntime.current().placementCapabilities().frontDistance(player);
        if (mode == PortalPredictionMode.PROJECTION) {
            if (!horizontalDoor) {
                frontDistance += projectionExtra(player, frontProjectionAxis(player),
                    constraints.frontProjectionFactor());
            }
        }
        EntryResult last = null;
        ServerLevel level = serverLevel(player);
        double minimumExposure = RiftRuntime.current().placementCapabilities()
            .minimumFloatingPortalExposure(player);
        for (Vec3 displacement : positions) {
            FrontPortalPlacementPlanner.Result planned = FrontPortalPlacementPlanner.resolve(
                player.position(), player.getBoundingBox(), displacement, player.getYRot(),
                horizontal, aperture, frontDistance, minimumExposure,
                (placement, exposure) -> !floatingObstructed(level, placement, exposure));
            if (planned.successful()) return EntryResult.success(planned.placement());
            last = EntryResult.failure(planned.errorKey());
        }
        return last == null ? EntryResult.failure("message.riftgun.front_obstructed") : last;
    }

    /**
     * Distance added to the door when PROJECTION mode is active. Uses the sampled recent
     * velocity (blocks/tick scaled to per-second) so doors opened from the modal GUI still
     * see the player's movement right before opening. Falls back to instantaneous velocity.
     * Horizontal FRONT portals stay adjacent to the player and therefore do not use distance projection.
     */
    private static double projectionExtra(ServerPlayer player, Vec3 axis, double factor) {
        Vec3 velocity = RiftRuntime.current().motionHistory().recentVelocity(player)
            .orElse(player.getDeltaMovement());
        double projection = velocity.dot(axis) * TICKS_PER_SECOND;
        return Mth.clamp(projection * factor, 0.0, MAXIMUM_PROJECTION_EXTRA);
    }

    /** Projection axis for the front door: the view heading in the xz plane. */
    private static Vec3 frontProjectionAxis(ServerPlayer player) {
        return Vec3.directionFromRotation(0.0F, player.getYRot()).normalize();
    }

    @Override
    public PortalPlacementCapture captureSurfaceFace(ServerPlayer player, SurfaceFaceSelection selection,
                                                     PortalPlacementConstraints constraints) {
        ServerLevel level = serverLevel(player);
        Vec3 eye = player.getEyePosition();
        Vec3 faceCenter = Vec3.atCenterOf(selection.anchor())
            .add(new Vec3(selection.face().getStepX(), selection.face().getStepY(),
                selection.face().getStepZ()).scale(0.5));
        double rayRange = constraints.maximumSurfaceRange() + 16.0;
        HitResult raw = level.clip(new ClipContext(eye,
            eye.add(player.getLookAngle().scale(rayRange)), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player));
        boolean lineOfSight = raw instanceof BlockHitResult hit
            && raw.getType() == HitResult.Type.BLOCK
            && hit.getBlockPos().equals(selection.anchor());
        SurfaceFacePlacementPlanner.Result result = SurfaceFacePlacementPlanner.resolve(
            selection, constraints.aperture(), player.getYRot(), player.getBoundingBox(),
            surfaceProbe(level), new SurfaceFacePlacementPlanner.Validation(eye.distanceTo(faceCenter),
                constraints.maximumSurfaceRange(), lineOfSight));
        return result.successful()
            ? PortalPlacementCapture.success(PortalPlacementIntent.surface(result.placement()))
            : PortalPlacementCapture.failure(result.errorKey());
    }

    private Vec3 predictedDisplacement(ServerPlayer player, PortalMotionPredictor.Purpose purpose) {
        int ticks = PortalLifecycle.CHARGE_TICKS + PortalLifecycle.ANIMATION_TICKS;
        return RiftRuntime.current().motionPredictor().predictDisplacement(player, purpose, ticks,
            RiftRuntime.current().placementCapabilities().maximumHorizontalPrediction(player));
    }

    static PortalOrientation horizontalOrientation(float pitch, float minimumPitch) {
        if (pitch >= minimumPitch) return PortalOrientation.TOP;
        if (pitch <= -minimumPitch) return PortalOrientation.BOTTOM;
        return PortalOrientation.VERTICAL;
    }

    private EntryResult surface(ServerPlayer player, boolean smart, int requestedSmartDistance,
                                double maximumRange, PortalAperture aperture) {
        return surface(player, smart, requestedSmartDistance, maximumRange, maximumRange, aperture,
            dev.riftgun.pairing.PortalFloatingFallback.FRONT);
    }

    private EntryResult surface(ServerPlayer player, boolean smart, int requestedSmartDistance,
                                double maximumRange, double remoteDistance, PortalAperture aperture,
                                dev.riftgun.pairing.PortalFloatingFallback fallback) {
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
            return smart ? floatingFallback(player, remoteDistance, aperture, fallback)
                : EntryResult.failure("message.riftgun.surface_missing");
        }

        double distance = eye.distanceTo(hit.getLocation());
        if (smart && distance > Math.min(requestedSmartDistance, maximumRange)) {
            return floatingFallback(player, remoteDistance, aperture, fallback);
        }
        if (distance > maximumRange) return EntryResult.failure("message.riftgun.surface_out_of_range");
//? if >=1.21.11 {
        /*EntryResult attached = attached((ServerLevel) player.level(), player, hit, aperture);
*///?} else {
        EntryResult attached = attached(player.serverLevel(), player, hit, aperture);
//?}
        return shouldUseFloatingFallback(smart, attached.placement != null)
            ? floatingFallback(player, remoteDistance, aperture, fallback) : attached;
    }

    static boolean shouldUseFloatingFallback(boolean smart, boolean attachedPlacementSuccessful) {
        return smart && !attachedPlacementSuccessful;
    }

    private EntryResult floatingFallback(ServerPlayer player, double maximumRange,
                                         PortalAperture aperture,
                                         dev.riftgun.pairing.PortalFloatingFallback fallback) {
        return fallback == dev.riftgun.pairing.PortalFloatingFallback.REMOTE
            ? remote(player, maximumRange, aperture) : EntryResult.frontRoute();
    }

    private EntryResult remote(ServerPlayer player, double maximumRange, PortalAperture aperture) {
        return remote(player, maximumRange, aperture, null);
    }

    private EntryResult remote(ServerPlayer player, double maximumRange, PortalAperture aperture,
                               PortalOrientation orientation) {
        ServerLevel level = serverLevel(player);
        var capabilities = RiftRuntime.current().placementCapabilities();
        return RemotePortalPlacementResolver.resolve(level, player, maximumRange, aperture,
            capabilities.downshotMinimumPitch(player), orientation,
            capabilities.minimumFloatingPortalExposure(player))
            .map(EntryResult::success)
            .orElseGet(() -> EntryResult.failure("message.riftgun.remote_obstructed"));
    }

    private EntryResult revalidateRemote(ServerPlayer player, PortalPlacement placement,
                                         double maximumRange) {
        if (placement == null || placement.anchored()) {
            return EntryResult.failure("message.riftgun.remote_invalid");
        }
        ServerLevel level = serverLevel(player);
        if (player.getEyePosition().distanceTo(placement.center()) > maximumRange + 1.5) {
            return EntryResult.failure("message.riftgun.remote_out_of_range");
        }
        double exposure = placement.geometry().expanded()
            ? PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE
            : RiftRuntime.current().placementCapabilities().minimumFloatingPortalExposure(player);
        return !FloatingPortalBounds.allows(placement.bounds()) || floatingObstructed(level, placement, exposure)
            ? EntryResult.failure("message.riftgun.remote_obstructed") : EntryResult.success(placement);
    }

    private static ServerLevel serverLevel(ServerPlayer player) {
        //? if >=1.21.11 {
        /*return (ServerLevel) player.level();
        *///?} else {
        return player.serverLevel();
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
        if (placement.geometry().requiresFullSupport()
            && !PortalSupportArea.hasFullExpandedSupport(level, placement)) {
            return EntryResult.failure("message.riftgun.surface_invalid");
        }
        return blocked(level, placement.bounds())
            ? EntryResult.failure("message.riftgun.surface_obstructed")
            : EntryResult.success(placement);
    }

    private EntryResult attached(ServerLevel level, ServerPlayer player, BlockHitResult hit,
                                 PortalAperture aperture) {
        SurfaceFacePlacementPlanner.Result result = SurfaceFacePlacementPlanner.resolveAttached(
            new SurfaceFaceSelection(hit.getBlockPos(), hit.getDirection()), aperture,
            player.getYRot(), player.getBoundingBox(), surfaceProbe(level), hit.getLocation());
        return result.successful() ? EntryResult.success(result.placement()) : EntryResult.failure(result.errorKey());
    }

    private static SurfaceFacePlacementPlanner.Probe surfaceProbe(ServerLevel level) {
        return new SurfaceFacePlacementPlanner.Probe() {
            @Override
            public boolean anchorSolid(BlockPos position) {
                return backingBlock(level, position) > 0;
            }

            @Override
            public boolean blocked(PortalPlacement placement) {
                return VanillaPortalPlacementResolver.blocked(level, placement.bounds());
            }

            @Override
            public int backingBlocks(BlockPos position) {
                return backingBlock(level, position);
            }

            @Override
            public boolean expandedSupport(PortalPlacement placement) {
                return PortalSupportArea.hasFullExpandedSupport(level, placement);
            }
        };
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
                return FloatingPortalBounds.allows(placement.bounds()) && !blocked(level, placement.bounds());
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
            if (FloatingPortalBounds.allows(expanded.bounds())
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
