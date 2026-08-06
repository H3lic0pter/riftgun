package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SwirlVisualGeometryTest {
    private static final double EPSILON = 1.0E-6;

    @Test
    void anchoredSurfaceCentersItsThicknessOnTheWall() {
        double entityCenterDistance = 0.062;

        assertEquals(SwirlVisualGeometry.WALL_OFFSET + SwirlVisualGeometry.DEPTH * 0.5,
            SwirlVisualGeometry.outwardFaceDistance(entityCenterDistance), EPSILON);
        assertEquals(0.0078125, SwirlVisualGeometry.DEPTH, EPSILON);
    }

    @Test
    void horizontalPortalUsesFivePercentVisualExpansionAndInsetEdge() {
        assertEquals(0.95 * 1.05, SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE, EPSILON);
        assertEquals(0.80, SwirlVisualGeometry.EDGE_RADIUS_SCALE, EPSILON);
    }
}
