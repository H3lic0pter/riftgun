package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

final class RandomRiftGeometryTest {
    @Test
    void samplesWithinConfiguredAnnulus() {
        ArrayDeque<Double> values = new ArrayDeque<>();
        values.add(0.25);
        values.add(0.0);
        RandomRiftGeometry.Offset inner = RandomRiftGeometry.sample(256, 4096, values::remove);
        assertEquals(256.0, Math.hypot(inner.x(), inner.z()), 0.0001);

        values.add(0.75);
        values.add(Math.nextDown(1.0));
        RandomRiftGeometry.Offset outer = RandomRiftGeometry.sample(256, 4096, values::remove);
        double radius = Math.hypot(outer.x(), outer.z());
        assertTrue(radius >= 256.0 && radius < 4096.0);
    }

    @Test
    void normalizesReversedRadii() {
        RandomRiftGeometry.Offset offset = RandomRiftGeometry.sample(4096, 256,
            new ArrayDeque<>(java.util.List.of(0.0, 0.0))::remove);
        assertEquals(256.0, Math.hypot(offset.x(), offset.z()), 0.0001);
    }
}
