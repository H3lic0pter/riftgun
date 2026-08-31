package dev.riftgun.client.render;

/** Converts immutable world-space preview geometry to stable camera-relative coordinates. */
final class PortalPreviewCameraCoordinates {
    static float relativeTo(double cameraCoordinate, double pointCoordinate) {
        return (float) (pointCoordinate - cameraCoordinate);
    }

    private PortalPreviewCameraCoordinates() {}
}
