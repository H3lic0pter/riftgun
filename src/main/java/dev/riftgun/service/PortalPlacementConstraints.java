package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;

public record PortalPlacementConstraints(int smartDistance, double maximumSurfaceRange,
                                         boolean motionPrediction, PortalAperture aperture) {
    public PortalPlacementConstraints {
        smartDistance = Math.max(1, smartDistance);
        maximumSurfaceRange = Math.max(1.0, maximumSurfaceRange);
        if (aperture == null) aperture = PortalAperture.STANDARD;
    }
}
