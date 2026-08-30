package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
}
