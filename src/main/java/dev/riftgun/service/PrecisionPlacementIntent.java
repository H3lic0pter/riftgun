package dev.riftgun.service;

import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import org.jetbrains.annotations.Nullable;

/** Version-neutral domain intent for one explicit precision-placement choice. */
public record PrecisionPlacementIntent(Kind kind, @Nullable SurfaceFaceSelection surface,
                                       PortalOrientation orientation,
                                       @Nullable PortalPlacement previewPlacement) {
    public PrecisionPlacementIntent {
        if (kind == null || orientation == null || kind == Kind.SURFACE && surface == null
            || kind == Kind.FLOATING && surface != null
            || previewPlacement != null && (kind != Kind.FLOATING || previewPlacement.anchored()
                || previewPlacement.orientation() != orientation
                || !finite(previewPlacement))) {
            throw new IllegalArgumentException("invalid precision placement intent");
        }
    }

    public static PrecisionPlacementIntent surface(SurfaceFaceSelection selection) {
        return new PrecisionPlacementIntent(
            Kind.SURFACE, selection, PortalOrientation.VERTICAL, null);
    }

    public static PrecisionPlacementIntent floating(PortalOrientation orientation) {
        return new PrecisionPlacementIntent(Kind.FLOATING, null, orientation, null);
    }

    public PrecisionPlacementIntent withPreviewPlacement(@Nullable PortalPlacement placement) {
        return new PrecisionPlacementIntent(kind, surface, orientation, placement);
    }

    public enum Kind { SURFACE, FLOATING }

    private static boolean finite(PortalPlacement placement) {
        return Double.isFinite(placement.center().x)
            && Double.isFinite(placement.center().y)
            && Double.isFinite(placement.center().z)
            && Float.isFinite(placement.yaw());
    }
}
