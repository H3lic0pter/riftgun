package dev.riftgun.portal;

/** Converts immutable world-space preview geometry to stable view-relative coordinates. */
public final class PortalPreviewCoordinates {
    public static float relativeTo(double viewCoordinate, double pointCoordinate) {
        return (float) (pointCoordinate - viewCoordinate);
    }

    private PortalPreviewCoordinates() {}
}
