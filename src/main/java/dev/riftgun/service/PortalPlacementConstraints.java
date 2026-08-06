package dev.riftgun.service;

public record PortalPlacementConstraints(int smartDistance, double maximumSurfaceRange,
                                         boolean motionPrediction) {
    public PortalPlacementConstraints {
        smartDistance = Math.max(1, smartDistance);
        maximumSurfaceRange = Math.max(1.0, maximumSurfaceRange);
    }
}
