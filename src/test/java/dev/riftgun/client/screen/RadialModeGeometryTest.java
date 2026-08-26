package dev.riftgun.client.screen;

import dev.riftgun.math.RadialModeGeometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RadialModeGeometryTest {
    @Test
    void centerIsCancellationZone() {
        assertTrue(RadialModeGeometry.selectionIndex(3, 4, 4, 10).isEmpty());
    }

    @Test
    void fourOptionsStartAtTopAndProceedClockwise() {
        assertEquals(0, RadialModeGeometry.selectionIndex(0, -50, 4, 10).orElseThrow());
        assertEquals(1, RadialModeGeometry.selectionIndex(50, 0, 4, 10).orElseThrow());
        assertEquals(2, RadialModeGeometry.selectionIndex(0, 50, 4, 10).orElseThrow());
        assertEquals(3, RadialModeGeometry.selectionIndex(-50, 0, 4, 10).orElseThrow());
    }

    @Test
    void threeOptionsWrapAcrossNegativeAngles() {
        assertEquals(0, RadialModeGeometry.selectionIndex(0, -50, 3, 10).orElseThrow());
        assertEquals(1, RadialModeGeometry.selectionIndex(50, 30, 3, 10).orElseThrow());
        assertEquals(2, RadialModeGeometry.selectionIndex(-50, 30, 3, 10).orElseThrow());
    }
}
