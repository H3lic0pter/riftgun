package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

final class RemotePortalPlacementSearchTest {
    @Test
    void findsNarrowValidIntervalBetweenFormerCoarseSamples() {
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
