package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** Shared REMOTE candidate search used by server authority and client-only previews. */
public final class RemotePortalPlacementResolver {
    private static final double MINIMUM_DISTANCE = 1.5;
    private static final double HIT_OFFSET = 0.18;
    private static final double SEARCH_STEP = 0.25;
    private static final int MAXIMUM_COARSE_CANDIDATES = 1024;
    private static final double CLIENT_CHUNK_PROBE_STEP = 8.0;

    public static Optional<PortalPlacement> resolve(Level level, Entity viewer,
                                                    double maximumRange, PortalAperture aperture,
                                                    float horizontalPitchThreshold,
                                                    double minimumExposure) {
        return resolve(level, viewer, maximumRange, aperture, horizontalPitchThreshold,
            null, minimumExposure);
    }

    public static Optional<PortalPlacement> resolve(Level level, Entity viewer,
                                                    double maximumRange, PortalAperture aperture,
                                                    float horizontalPitchThreshold,
                                                    @Nullable PortalOrientation orientationOverride,
                                                    double minimumExposure) {
        Vec3 eye = viewer.getEyePosition();
        Vec3 look = viewer.getLookAngle().normalize();
        PortalOrientation orientation = orientationOverride == null
            ? VanillaPortalPlacementResolver.horizontalOrientation(
                viewer.getXRot(), horizontalPitchThreshold)
            : orientationOverride;
        PortalGeometry standard = orientation == PortalOrientation.VERTICAL
            ? PortalGeometry.FLOATING_VERTICAL : PortalGeometry.HORIZONTAL;
        PortalGeometry expanded = orientation == PortalOrientation.VERTICAL
            ? PortalAperturePolicy.floatingVertical() : PortalAperturePolicy.horizontal();
        PortalGeometry largest = PortalAperturePolicy.expanded(aperture) ? expanded : standard;
        double boundedRange = Math.max(MINIMUM_DISTANCE, maximumRange);
        boundedRange = worldHeightRange(level, eye, look, largest, viewer.getYRot(), boundedRange);
        boundedRange = loadedClientRange(level, eye, look, boundedRange);
        Vec3 rayEnd = eye.add(look.scale(boundedRange));
        HitResult hit = level.clip(new ClipContext(
            eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        double distance = hit.getType() == HitResult.Type.BLOCK
            ? Math.max(MINIMUM_DISTANCE, eye.distanceTo(hit.getLocation()) - HIT_OFFSET)
            : boundedRange;
        double coarseStep = coarseStep(distance);

        for (double candidateDistance = distance;
             candidateDistance >= MINIMUM_DISTANCE;
             candidateDistance -= coarseStep) {
            Optional<PortalPlacement> candidate = availableAt(level, eye, look, candidateDistance,
                orientation, standard, expanded, aperture, viewer.getYRot(), minimumExposure);
            if (candidate.isEmpty()) continue;
            if (coarseStep <= SEARCH_STEP || candidateDistance == distance) return candidate;

            // Recover the furthest valid quarter-block position inside the coarse interval.
            double upper = Math.min(distance, candidateDistance + coarseStep);
            for (double refined = upper; refined > candidateDistance; refined -= SEARCH_STEP) {
                Optional<PortalPlacement> precise = availableAt(level, eye, look, refined,
                    orientation, standard, expanded, aperture, viewer.getYRot(), minimumExposure);
                if (precise.isPresent()) return precise;
            }
            return candidate;
        }
        return Optional.empty();
    }

    static double coarseStep(double distance) {
        return Math.max(SEARCH_STEP,
            Math.max(0.0, distance - MINIMUM_DISTANCE) / MAXIMUM_COARSE_CANDIDATES);
    }

    private static Optional<PortalPlacement> availableAt(
        Level level, Vec3 eye, Vec3 look, double distance, PortalOrientation orientation,
        PortalGeometry standard, PortalGeometry expanded, PortalAperture aperture, float yaw,
        double minimumExposure
    ) {
        Vec3 center = eye.add(look.scale(distance));
        if (PortalAperturePolicy.expanded(aperture)) {
            PortalPlacement placement = placement(center, orientation, expanded, yaw);
            if (available(level, placement, PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE)) {
                return Optional.of(placement);
            }
        }
        PortalPlacement placement = placement(center, orientation, standard, yaw);
        return available(level, placement, minimumExposure)
            ? Optional.of(placement) : Optional.empty();
    }

    private static double loadedClientRange(Level level, Vec3 eye, Vec3 look, double maximumRange) {
        if (!level.isClientSide()) return maximumRange;
        double loaded = MINIMUM_DISTANCE;
        for (double distance = MINIMUM_DISTANCE; distance <= maximumRange;
             distance += CLIENT_CHUNK_PROBE_STEP) {
            if (!level.hasChunkAt(BlockPos.containing(eye.add(look.scale(distance))))) return loaded;
            loaded = distance;
        }
        return maximumRange;
    }

    private static double worldHeightRange(Level level, Vec3 eye, Vec3 look,
                                           PortalGeometry geometry, float yaw,
                                           double maximumRange) {
        PortalPlacement origin = placement(eye, geometry == PortalGeometry.HORIZONTAL
                || geometry == PortalGeometry.HORIZONTAL_EXPANDED
                ? PortalOrientation.TOP : PortalOrientation.VERTICAL,
            geometry, yaw);
        double lowerExtent = eye.y - origin.bounds().minY;
        double upperExtent = origin.bounds().maxY - eye.y;
//? if >=1.21.11 {
        /*double minimumY = level.dimensionType().minY();
        double maximumY = minimumY + level.dimensionType().height();
*///?} else {
        double minimumY = level.getMinBuildHeight();
        double maximumY = level.getMaxBuildHeight();
//?}
        if (look.y > 1.0E-7) {
            maximumRange = Math.min(maximumRange, (maximumY - upperExtent - eye.y) / look.y);
        } else if (look.y < -1.0E-7) {
            maximumRange = Math.min(maximumRange, (minimumY + lowerExtent - eye.y) / look.y);
        }
        return Math.max(MINIMUM_DISTANCE, maximumRange);
    }

    private static PortalPlacement placement(Vec3 center, PortalOrientation orientation,
                                             PortalGeometry geometry, float yaw) {
        return new PortalPlacement(center, orientation, geometry, yaw, null, null);
    }

    private static boolean available(Level level, PortalPlacement placement, double minimumExposure) {
        return !outsideWorld(level, placement)
            && PortalFaceExposure.hasMinimumExposure(level, placement, minimumExposure);
    }

    private static boolean outsideWorld(Level level, PortalPlacement placement) {
//? if >=1.21.11 {
        /*int minimumY = level.dimensionType().minY();
        int maximumY = minimumY + level.dimensionType().height();
*///?} else {
        int minimumY = level.getMinBuildHeight();
        int maximumY = level.getMaxBuildHeight();
//?}
        return placement.bounds().minY < minimumY || placement.bounds().maxY > maximumY;
    }

    private RemotePortalPlacementResolver() {}
}
