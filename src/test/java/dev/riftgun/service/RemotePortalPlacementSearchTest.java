package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class RemotePortalPlacementSearchTest {
    @Test
    void findsNarrowValidIntervalInsideFineSearchWindow() {
        List<Double> probes = new ArrayList<>();

        Optional<String> result = RemotePortalPlacementResolver.findFurthest(9_248.0, distance -> {
            probes.add(distance);
            return distance <= 9_243.0 && distance >= 9_242.75
                ? Optional.of("valid") : Optional.empty();
        });

        assertEquals(Optional.of("valid"), result);
        assertEquals(9_243.0, probes.get(probes.size() - 1), 1.0E-9);
    }

    @Test
    void returnsTheFurthestQuarterBlockCandidate() {
        List<Double> probes = new ArrayList<>();

        Optional<Double> result = RemotePortalPlacementResolver.findFurthest(3.0, distance -> {
            probes.add(distance);
            return distance <= 2.5 ? Optional.of(distance) : Optional.empty();
        });

        assertEquals(Optional.of(2.5), result);
        assertEquals(List.of(3.0, 2.75, 2.5), probes);
    }

    @Test
    void boundsProbeCountWhenNoCandidateExistsAtMaximumConfiguredRange() {
        int[] probes = {0};

        Optional<String> result = RemotePortalPlacementResolver.findFurthest(9_248.0, distance -> {
            probes[0]++;
            return Optional.empty();
        });

        assertEquals(Optional.empty(), result);
        org.junit.jupiter.api.Assertions.assertTrue(probes[0] <= 1_536,
            "one placement request must not perform " + probes[0] + " collision probes");
    }

    @Test
    void loadedRangeChecksTheMaximumAndRefinesToQuarterBlockCandidates() {
        AtomicBoolean checkedMaximum = new AtomicBoolean();

        double boundaryLoaded = RemotePortalPlacementResolver.furthestContinuousLoaded(
            32.0, distance -> distance < 30.0);
        double maximumLoaded = RemotePortalPlacementResolver.furthestContinuousLoaded(
            32.0, distance -> {
                if (distance == 32.0) checkedMaximum.set(true);
                return distance < 32.0;
            });

        assertEquals(29.75, boundaryLoaded, 1.0E-9);
        assertEquals(31.75, maximumLoaded, 1.0E-9);
        org.junit.jupiter.api.Assertions.assertTrue(checkedMaximum.get());
    }

    @Test
    void loadedRangeVisitsOnlyNewChunkFootprints() {
        int[] probes = {0};
        AABB initial = new AABB(-1.0, 64.0, -0.5, 1.0, 67.0, 0.5);

        double loaded = RemotePortalPlacementResolver.furthestContinuousLoaded(
            9_248.0, new Vec3(1.0, 0.0, 0.0), initial, bounds -> {
                probes[0]++;
                return true;
            });

        assertEquals(9_248.0, loaded, 1.0E-9);
        org.junit.jupiter.api.Assertions.assertTrue(probes[0] < 600,
            "chunk-boundary traversal must not perform " + probes[0] + " per-block probes");
    }

    @Test
    void loadedRangeStopsBeforeTheFirstUnloadedChunkFootprint() {
        AABB initial = new AABB(-1.0, 64.0, -0.5, 1.0, 67.0, 0.5);

        double loaded = RemotePortalPlacementResolver.furthestContinuousLoaded(
            64.0, new Vec3(1.0, 0.0, 0.0), initial, bounds -> bounds.maxX <= 32.0);

        assertEquals(32.5, loaded, 1.0E-9);
    }

    @Test
    void chunkBoundaryTraversalMatchesQuarterBlockReferenceAcrossDirections() {
        AABB initial = new AABB(-1.2, 64.0, -0.4, 1.2, 67.0, 0.4);
        for (Vec3 direction : List.of(
            new Vec3(1.0, 0.0, 0.0), new Vec3(-1.0, 0.0, 0.0),
            new Vec3(1.0, 0.2, 1.0).normalize(), new Vec3(-0.3, -0.1, 1.0).normalize(),
            new Vec3(0.0, 1.0, 0.0))) {
            java.util.function.Predicate<AABB> loaded = bounds ->
                RemotePortalPlacementResolver.chunksLoaded(bounds,
                    (chunkX, chunkZ) -> Math.abs(chunkX) <= 1 && Math.abs(chunkZ) <= 1);
            double expected = 0.0;
            for (double distance = 1.5; distance <= 64.0; distance += 0.25) {
                AABB bounds = initial.move(direction.scale(distance - 1.5));
                if (!loaded.test(bounds)) break;
                expected = distance;
            }

            double actual = RemotePortalPlacementResolver.furthestContinuousLoaded(
                64.0, direction, initial, loaded);

            assertEquals(expected, actual, 1.0E-9, direction.toString());
        }
    }

    @Test
    void portalFootprintRequiresEveryTouchedChunkToBeLoaded() {
        AABB crossingBoundary = new AABB(15.5, 64.0, 2.0, 16.5, 66.0, 3.0);

        org.junit.jupiter.api.Assertions.assertFalse(
            RemotePortalPlacementResolver.chunksLoaded(crossingBoundary,
                (chunkX, chunkZ) -> chunkX == 0 && chunkZ == 0));
        org.junit.jupiter.api.Assertions.assertTrue(
            RemotePortalPlacementResolver.chunksLoaded(crossingBoundary,
                (chunkX, chunkZ) -> (chunkX == 0 || chunkX == 1) && chunkZ == 0));
    }
}
