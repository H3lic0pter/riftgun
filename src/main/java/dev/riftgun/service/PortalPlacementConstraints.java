package dev.riftgun.service;

import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalAperture;

public record PortalPlacementConstraints(int smartDistance, double maximumSurfaceRange,
                                         PortalPredictionMode predictionMode, PortalAperture aperture) {
    public PortalPlacementConstraints {
        smartDistance = Math.max(1, smartDistance);
        maximumSurfaceRange = Math.max(1.0, maximumSurfaceRange);
        if (aperture == null) aperture = PortalAperture.STANDARD;
    }
}
