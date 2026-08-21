package dev.riftgun.client.render;

final class EndframeVisualGeometry {
    static final int STAR_SEGMENTS = 24;
    static final int STAR_VERTEX_COUNT = STAR_SEGMENTS * 2 * 4;
    static final int CUSTOM_VERTEX_COUNT = STAR_VERTEX_COUNT + 8;
    static final int FALLBACK_VERTEX_COUNT = 8 + 8;

    private static final float[] RIM_X = new float[STAR_SEGMENTS + 1];
    private static final float[] RIM_Y = new float[STAR_SEGMENTS + 1];

    static {
        for (int point = 0; point < STAR_SEGMENTS; point++) {
            double angle = Math.PI * 2.0 * point / STAR_SEGMENTS;
            RIM_X[point] = (float) Math.cos(angle);
            RIM_Y[point] = (float) Math.sin(angle);
        }
        RIM_X[STAR_SEGMENTS] = RIM_X[0];
        RIM_Y[STAR_SEGMENTS] = RIM_Y[0];
    }

    static float rimX(int point) {
        return RIM_X[point];
    }

    static float rimY(int point) {
        return RIM_Y[point];
    }

    static float rotatedU(float u, float v, double cosine, double sine) {
        return (float) (0.5 + (u - 0.5) * cosine - (v - 0.5) * sine);
    }

    static float rotatedV(float u, float v, double cosine, double sine) {
        return (float) (0.5 + (u - 0.5) * sine + (v - 0.5) * cosine);
    }

    static float alignedFaceU(float windingU, boolean mirrored) {
        return mirrored ? 1.0F - windingU : windingU;
    }

    private EndframeVisualGeometry() {}
}
