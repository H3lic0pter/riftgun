package dev.riftgun.client.render;

final class SwirlVisualGeometry {
    static final float WALL_OFFSET = 0.001F;
    static final float DEPTH = 1.0F / 128.0F;
    static final float HORIZONTAL_VISIBLE_SIZE = 0.95F * 1.05F;
    static final float EDGE_RADIUS_SCALE = 0.80F;

    static float anchoredCenterOffset(double entityCenterDistance) {
        return (float) (WALL_OFFSET - entityCenterDistance);
    }

    static float outwardFaceDistance(double entityCenterDistance) {
        return (float) entityCenterDistance + anchoredCenterOffset(entityCenterDistance) + DEPTH * 0.5F;
    }

    private SwirlVisualGeometry() {}
}
