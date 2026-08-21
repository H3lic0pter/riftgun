package dev.riftgun.client.render;

final class SwirlFallbackGeometry {
    static FaceOffsets faceOffsets(float normalOffset, float depth) {
        float halfDepth = depth * 0.5F;
        return new FaceOffsets(normalOffset + halfDepth, normalOffset - halfDepth);
    }

    static int vertexCount(boolean hasDistinctBack) {
        return hasDistinctBack ? 8 : 4;
    }

    static float rotationDirection(boolean backFace) {
        return backFace ? -1.0F : 1.0F;
    }

    record FaceOffsets(float front, float back) {
        boolean hasDistinctBack() {
            return Float.compare(front, back) != 0;
        }
    }

    private SwirlFallbackGeometry() {}
}
