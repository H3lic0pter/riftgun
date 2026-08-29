package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalAperture;
import org.junit.jupiter.api.Test;

final class PortalPlacementConstraintsTest {
    @Test
    void keepsRemoteDistanceIndependentFromSurfaceMaximum() {
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            48, 80.0, 12.0, PortalPredictionMode.OFF, PortalAperture.STANDARD,
            0.7, 1.0, null);

        assertEquals(48, constraints.smartDistance());
        assertEquals(80.0, constraints.maximumSurfaceRange());
        assertEquals(12.0, constraints.remoteDistance());
    }

    @Test
    void clampsRemoteDistanceToHardwareMaximum() {
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            48, 80.0, 120.0, PortalPredictionMode.OFF, PortalAperture.STANDARD,
            0.7, 1.0, null);

        assertEquals(80.0, constraints.remoteDistance());
    }
}
