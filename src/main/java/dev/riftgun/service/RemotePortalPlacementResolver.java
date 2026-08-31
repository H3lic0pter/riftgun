package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

/** Shared REMOTE candidate search used by server authority and client-only previews. */
public final class RemotePortalPlacementResolver {
    private static final double MINIMUM_DISTANCE = 1.5;
    private static final double HIT_OFFSET = 0.18;
    private static final double SEARCH_STEP = 0.25;
    private static final double FINE_SEARCH_DISTANCE = 32.0;
    private static final double COARSE_SEARCH_STEP = 8.0;
    private static final double CHUNK_SIZE = 16.0;
    private static final double CHUNK_BOUNDS_EPSILON = 1.0E-7;
    private static final double CHUNK_TRANSITION_EPSILON = 1.0E-6;

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
        boundedRange = loadedClientRange(level, eye, look, boundedRange,
            orientation, largest, viewer.getYRot());
        if (boundedRange < MINIMUM_DISTANCE) return Optional.empty();
        Vec3 rayEnd = eye.add(look.scale(boundedRange));
        HitResult hit = level.clip(new ClipContext(
            eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        double distance = hit.getType() == HitResult.Type.BLOCK
            ? Math.max(MINIMUM_DISTANCE, eye.distanceTo(hit.getLocation()) - HIT_OFFSET)
            : boundedRange;
        return findFurthest(distance, candidateDistance -> availableAt(
            level, eye, look, candidateDistance, orientation, standard, expanded, aperture,
            viewer.getYRot(), minimumExposure));
    }

    static <T> Optional<T> findFurthest(double distance,
                                        DoubleFunction<Optional<T>> candidateAt) {
        double fineMinimum = Math.max(MINIMUM_DISTANCE, distance - FINE_SEARCH_DISTANCE);
        for (double candidateDistance = distance;
             candidateDistance >= fineMinimum;
             candidateDistance -= SEARCH_STEP) {
            Optional<T> candidate = candidateAt.apply(candidateDistance);
            if (candidate.isPresent()) return candidate;
        }

        // Exact quarter-block probing is retained near the requested endpoint, where a wall or
        // overhang normally forces the fallback. Beyond that window use bounded scouts, refining
        // the first interval whose near edge is usable. This keeps the configured 9248-block
        // maximum below ~1.4k collision probes instead of 36,987.
        double upper = fineMinimum;
        for (double coarse = fineMinimum - COARSE_SEARCH_STEP;
             coarse > MINIMUM_DISTANCE;
             coarse -= COARSE_SEARCH_STEP) {
            Optional<T> candidate = candidateAt.apply(coarse);
            if (candidate.isPresent()) {
                return refineInterval(upper, coarse, candidate, candidateAt);
            }
            upper = coarse;
        }
        Optional<T> minimum = candidateAt.apply(MINIMUM_DISTANCE);
        return minimum.isPresent()
            ? refineInterval(upper, MINIMUM_DISTANCE, minimum, candidateAt)
            : Optional.empty();
    }

    private static <T> Optional<T> refineInterval(double upper, double lower,
                                                   Optional<T> lowerCandidate,
                                                   DoubleFunction<Optional<T>> candidateAt) {
        for (double candidateDistance = upper - SEARCH_STEP;
             candidateDistance > lower;
             candidateDistance -= SEARCH_STEP) {
            Optional<T> candidate = candidateAt.apply(candidateDistance);
            if (candidate.isPresent()) return candidate;
        }
        return lowerCandidate;
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

    private static double loadedClientRange(Level level, Vec3 eye, Vec3 look, double maximumRange,
                                            PortalOrientation orientation, PortalGeometry geometry,
                                            float yaw) {
        if (!level.isClientSide()) return maximumRange;
        AABB initialBounds = placement(eye.add(look.scale(MINIMUM_DISTANCE)),
            orientation, geometry, yaw).bounds();
        return furthestContinuousLoaded(maximumRange, look, initialBounds,
            bounds -> chunksLoaded(level, bounds));
    }

    /**
     * Walks only the distances where the leading edge of the portal footprint enters a new chunk.
     * Trailing-edge transitions only remove required chunks, so they cannot discover an unloaded
     * chunk and do not need a probe.
     */
    static double furthestContinuousLoaded(double maximumRange, Vec3 direction, AABB initialBounds,
                                            Predicate<AABB> loadedAt) {
        double maximum = Math.max(MINIMUM_DISTANCE, maximumRange);
        if (!loadedAt.test(initialBounds)) return 0.0;
        if (maximum == MINIMUM_DISTANCE) return maximum;

        double current = MINIMUM_DISTANCE;
        AABB bounds = initialBounds;
        while (current < maximum) {
            double deltaX = nextLeadingChunkBoundary(bounds.minX, bounds.maxX, direction.x);
            double deltaZ = nextLeadingChunkBoundary(bounds.minZ, bounds.maxZ, direction.z);
            double delta = Math.min(deltaX, deltaZ);
            if (!Double.isFinite(delta)) return maximum;

            double transition = current + delta;
            if (transition >= maximum) {
                AABB maximumBounds = initialBounds.move(direction.scale(maximum - MINIMUM_DISTANCE));
                return loadedAt.test(maximumBounds) ? maximum : previousSearchDistance(maximum);
            }

            double probeDistance = Math.min(maximum, transition + CHUNK_TRANSITION_EPSILON);
            AABB probeBounds = initialBounds.move(direction.scale(probeDistance - MINIMUM_DISTANCE));
            if (!loadedAt.test(probeBounds)) return previousSearchDistance(transition);
            current = probeDistance;
            bounds = probeBounds;
        }
        return maximum;
    }

    private static double nextLeadingChunkBoundary(double minimum, double maximum,
                                                   double velocity) {
        if (Math.abs(velocity) < 1.0E-12) return Double.POSITIVE_INFINITY;
        double leading = velocity > 0.0 ? maximum - CHUNK_BOUNDS_EPSILON : minimum;
        double chunk = Math.floor(leading / CHUNK_SIZE);
        double boundary = velocity > 0.0 ? (chunk + 1.0) * CHUNK_SIZE : chunk * CHUNK_SIZE;
        return Math.max(0.0, (boundary - leading) / velocity);
    }

    private static double previousSearchDistance(double unavailableDistance) {
        double below = Math.nextDown(unavailableDistance);
        long steps = (long) Math.floor((below - MINIMUM_DISTANCE) / SEARCH_STEP);
        return steps < 0L ? 0.0 : MINIMUM_DISTANCE + steps * SEARCH_STEP;
    }

    /** Retained as the generic quarter-block continuity seam used by small-range unit tests. */
    static double furthestContinuousLoaded(double maximumRange, DoublePredicate loadedAt) {
        double maximum = Math.max(MINIMUM_DISTANCE, maximumRange);
        if (!loadedAt.test(MINIMUM_DISTANCE)) return 0.0;
        double loaded = MINIMUM_DISTANCE;
        for (double distance = MINIMUM_DISTANCE + 1.0;
             distance < maximum;
             distance += 1.0) {
            if (!loadedAt.test(distance)) return refineLoadedRange(loaded, distance, loadedAt);
            loaded = distance;
        }
        return loadedAt.test(maximum) ? maximum : refineLoadedRange(loaded, maximum, loadedAt);
    }

    private static double refineLoadedRange(double loaded, double unavailable,
                                            DoublePredicate loadedAt) {
        double result = loaded;
        for (double distance = loaded + SEARCH_STEP;
             distance < unavailable;
             distance += SEARCH_STEP) {
            if (!loadedAt.test(distance)) break;
            result = distance;
        }
        return result;
    }

    private static boolean chunksLoaded(Level level, AABB bounds) {
        int y = BlockPos.containing(bounds.getCenter()).getY();
        return chunksLoaded(bounds, (chunkX, chunkZ) ->
            level.hasChunkAt(new BlockPos(chunkX << 4, y, chunkZ << 4)));
    }

    static boolean chunksLoaded(AABB bounds, BiPredicate<Integer, Integer> loadedChunk) {
        int minChunkX = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).getX() >> 4;
        int maxChunkX = BlockPos.containing(bounds.maxX - 1.0E-7, bounds.minY,
            bounds.minZ).getX() >> 4;
        int minChunkZ = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ).getZ() >> 4;
        int maxChunkZ = BlockPos.containing(bounds.minX, bounds.minY,
            bounds.maxZ - 1.0E-7).getZ() >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!loadedChunk.test(chunkX, chunkZ)) return false;
            }
        }
        return true;
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
