package dev.riftgun.client.render;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;

final class SwirlVisualGeometry {
    static final float WALL_OFFSET = 0.001F;
    static final float DEPTH = 1.0F / 128.0F;
    static final float HORIZONTAL_VISIBLE_SIZE = 0.95F * 1.05F;
    static final float SURFACE_VISIBLE_SCALE = 1.05F;
    static final float EDGE_RADIUS_SCALE = 0.75F;

    static float anchoredCenterOffset(double entityCenterDistance) {
        return (float) (WALL_OFFSET - entityCenterDistance);
    }

    static float outwardFaceDistance(double entityCenterDistance) {
        return (float) entityCenterDistance + anchoredCenterOffset(entityCenterDistance) + DEPTH * 0.5F;
    }

    static float visibleWidthScale(PortalPlacement placement) {
        return visibleSizeScale(placement);
    }

    static float visibleHeightScale(PortalPlacement placement) {
        return visibleSizeScale(placement);
    }

    private static float visibleSizeScale(PortalPlacement placement) {
        PortalGeometry geometry = placement.geometry();
        if (placement.orientation() != PortalOrientation.VERTICAL) {
            return HORIZONTAL_VISIBLE_SIZE;
        }
        if (geometry == PortalGeometry.SURFACE_COMPACT) {
            return HORIZONTAL_VISIBLE_SIZE;
        }
        if (geometry == PortalGeometry.SURFACE_EXPANDED || geometry == PortalGeometry.SURFACE_VERTICAL) {
            return SURFACE_VISIBLE_SCALE;
        }
        return 1.0F;
    }

    private SwirlVisualGeometry() {}
}
