package dev.riftgun.client.render;

final class SwirlVisualGeometry {
    static final float WALL_OFFSET = 0.001F;
    static final float WALL_DEPTH = 0.0F;
    static final float HORIZONTAL_VISIBLE_SIZE = 0.95F;

    static float anchoredCenterOffset(double entityCenterDistance) {
        return (float) (WALL_OFFSET - entityCenterDistance - WALL_DEPTH * 0.5F);
    }

    static float outwardFaceDistance(double entityCenterDistance) {
        return (float) entityCenterDistance + anchoredCenterOffset(entityCenterDistance) + WALL_DEPTH * 0.5F;
    }

    private SwirlVisualGeometry() {}
}
