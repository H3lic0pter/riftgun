package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class EndframeVisualGeometryTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void starUsesTwentyFourQuadSegmentsOnTwoFaces() {
        assertEquals(24, EndframeVisualGeometry.STAR_SEGMENTS);
        assertEquals(192, EndframeVisualGeometry.STAR_VERTEX_COUNT);
        assertEquals(200, EndframeVisualGeometry.CUSTOM_VERTEX_COUNT);
        assertEquals(208, EndframeVisualGeometry.FALLBACK_VERTEX_COUNT);
    }

    @Test
    void precomputedRimClosesOnTheUnitCircle() {
        for (int point = 0; point <= EndframeVisualGeometry.STAR_SEGMENTS; point++) {
            float x = EndframeVisualGeometry.rimX(point);
            float y = EndframeVisualGeometry.rimY(point);
            assertEquals(1.0F, x * x + y * y, EPSILON);
        }
        assertEquals(EndframeVisualGeometry.rimX(0),
            EndframeVisualGeometry.rimX(EndframeVisualGeometry.STAR_SEGMENTS), EPSILON);
        assertEquals(EndframeVisualGeometry.rimY(0),
            EndframeVisualGeometry.rimY(EndframeVisualGeometry.STAR_SEGMENTS), EPSILON);
    }

    @Test
    void fallbackUvRotationKeepsTheTextureCentered() {
        assertEquals(0.5F, EndframeVisualGeometry.rotatedU(0.5F, 0.5F, 0.0, 1.0), EPSILON);
        assertEquals(0.5F, EndframeVisualGeometry.rotatedV(0.5F, 0.5F, 0.0, 1.0), EPSILON);
        assertEquals(1.0F, EndframeVisualGeometry.rotatedU(0.5F, 0.0F, 0.0, 1.0), EPSILON);
        assertEquals(0.5F, EndframeVisualGeometry.rotatedV(0.5F, 0.0F, 0.0, 1.0), EPSILON);
    }
}
