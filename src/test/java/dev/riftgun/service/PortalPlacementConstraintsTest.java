package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOrientation;
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

    @Test
    void precisionFloatingOrientationDisablesPredictionAndPreservesOtherConstraints() {
        PortalPlacementConstraints original = new PortalPlacementConstraints(
            48, 80.0, 12.0, PortalPredictionMode.PROJECTION, PortalAperture.EXPANDED,
            0.5, 1.25, null);

        PortalPlacementConstraints selected = original.forPrecisionFloating(PortalOrientation.TOP);

        assertEquals(PortalOrientation.TOP, selected.floatingOrientation());
        assertEquals(original.smartDistance(), selected.smartDistance());
        assertEquals(original.maximumSurfaceRange(), selected.maximumSurfaceRange());
        assertEquals(original.remoteDistance(), selected.remoteDistance());
        assertEquals(PortalPredictionMode.OFF, selected.predictionMode());
        assertEquals(original.aperture(), selected.aperture());
    }
}
