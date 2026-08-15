package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwirlFallbackGeometryTest {
    private static final float EPSILON = 1.0E-6F;

    @Test
    void sidePortalFacesBracketItsThinEdge() {
        SwirlFallbackGeometry.FaceOffsets offsets =
            SwirlFallbackGeometry.faceOffsets(0.125F, 1.0F / 128.0F);

        assertEquals(0.125F + 1.0F / 256.0F, offsets.front(), EPSILON);
        assertEquals(0.125F - 1.0F / 256.0F, offsets.back(), EPSILON);
        assertEquals(true, offsets.hasDistinctBack());
    }

    @Test
    void flatPortalDoesNotDuplicateACoplanarFace() {
        SwirlFallbackGeometry.FaceOffsets offsets =
            SwirlFallbackGeometry.faceOffsets(0.125F, 0.0F);

        assertEquals(0.125F, offsets.front(), EPSILON);
        assertEquals(0.125F, offsets.back(), EPSILON);
        assertEquals(false, offsets.hasDistinctBack());
    }

    @Test
    void fallbackRimNeverSamplesQuadCorners() {
        for (int segment = 0; segment < 48; segment++) {
            SwirlFallbackGeometry.RimPoint point =
                SwirlFallbackGeometry.rimPoint(segment, 48);
            double uvRadius = Math.hypot(point.u() - 0.5F, point.v() - 0.5F);

            assertEquals(0.5, uvRadius, EPSILON);
        }
    }
}
