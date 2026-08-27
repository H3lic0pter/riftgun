package dev.riftgun.service;

import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.pairing.PortalFloatingFallback;

public record PortalPlacementConstraints(int smartDistance, double maximumSurfaceRange,
                                         PortalPredictionMode predictionMode, PortalAperture aperture,
                                         double frontProjectionFactor, double downshotProjectionFactor,
                                         PortalFloatingFallback smartFallback) {
    public PortalPlacementConstraints(int smartDistance, double maximumSurfaceRange,
                                      PortalPredictionMode predictionMode, PortalAperture aperture) {
        this(smartDistance, maximumSurfaceRange, predictionMode, aperture, 0.7, 1.0,
            PortalFloatingFallback.FRONT);
    }

    public PortalPlacementConstraints {
        smartDistance = Math.max(1, smartDistance);
        maximumSurfaceRange = Math.max(1.0, maximumSurfaceRange);
        if (aperture == null) aperture = PortalAperture.STANDARD;
        if (smartFallback == null) smartFallback = PortalFloatingFallback.FRONT;
        frontProjectionFactor = Math.max(0.0, frontProjectionFactor);
        downshotProjectionFactor = Math.max(0.0, downshotProjectionFactor);
    }
}
