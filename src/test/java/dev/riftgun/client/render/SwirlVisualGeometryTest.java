package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class SwirlVisualGeometryTest {
    private static final double EPSILON = 1.0E-6;

    @Test
    void anchoredSurfaceMovesOnlyTheVisibleFaceToTheWall() {
        double entityCenterDistance = 0.062;

        assertEquals(0.001, SwirlVisualGeometry.outwardFaceDistance(entityCenterDistance), EPSILON);
        assertEquals(0.0, SwirlVisualGeometry.WALL_DEPTH, EPSILON);
    }

    @Test
    void horizontalPortalKeepsVisibleTextureInsideTheBlockFace() {
        assertEquals(0.95, SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE, EPSILON);
    }
}
