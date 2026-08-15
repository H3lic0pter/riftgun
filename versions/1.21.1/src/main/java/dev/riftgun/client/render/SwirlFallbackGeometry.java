package dev.riftgun.client.render;

final class SwirlFallbackGeometry {
    static FaceOffsets faceOffsets(float normalOffset, float depth) {
        float halfDepth = depth * 0.5F;
        return new FaceOffsets(normalOffset + halfDepth, normalOffset - halfDepth);
    }

    static RimPoint rimPoint(int segment, int segmentCount) {
        if (segmentCount < 3) throw new IllegalArgumentException("A portal disc needs at least three segments");
        double angle = Math.PI * 2.0 * segment / segmentCount;
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);
        return new RimPoint(x, y, 0.5F + x * 0.5F, 0.5F + y * 0.5F);
    }

    record FaceOffsets(float front, float back) {
        boolean hasDistinctBack() {
            return Float.compare(front, back) != 0;
        }
    }

    record RimPoint(float x, float y, float u, float v) {}

    private SwirlFallbackGeometry() {}
}
