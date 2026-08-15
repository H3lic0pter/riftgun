package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
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
        assertEquals(0.75, SwirlVisualGeometry.EDGE_RADIUS_SCALE, EPSILON);
    }

    @Test
    void expandedSurfaceMatchesHorizontalVisibleSize() {
        PortalPlacement expanded = placement(PortalOrientation.VERTICAL, PortalGeometry.SURFACE_EXPANDED);

        assertEquals(SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE,
            SwirlVisualGeometry.visibleWidthScale(expanded), EPSILON);
        assertEquals(SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE,
            SwirlVisualGeometry.visibleHeightScale(expanded), EPSILON);
        assertEquals(1.995,
            PortalGeometry.SURFACE_EXPANDED.width() * SwirlVisualGeometry.visibleWidthScale(expanded), EPSILON);
        assertEquals(1.995,
            PortalGeometry.SURFACE_EXPANDED.height() * SwirlVisualGeometry.visibleHeightScale(expanded), EPSILON);
    }

    @Test
    void standardSurfaceRendersAsTwoPointOneByOnePointZeroFive() {
        PortalPlacement standard = placement(PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL);

        assertEquals(1.05, SwirlVisualGeometry.visibleWidthScale(standard), EPSILON);
        assertEquals(1.05, SwirlVisualGeometry.visibleHeightScale(standard), EPSILON);
        assertEquals(1.05, PortalGeometry.SURFACE_VERTICAL.width() * SwirlVisualGeometry.visibleWidthScale(standard), EPSILON);
        assertEquals(2.1, PortalGeometry.SURFACE_VERTICAL.height() * SwirlVisualGeometry.visibleHeightScale(standard), EPSILON);
    }

    @Test
    void compactSurfaceMatchesHorizontalVisibleSize() {
        PortalPlacement compact = placement(PortalOrientation.VERTICAL, PortalGeometry.SURFACE_COMPACT);
        PortalPlacement top = placement(PortalOrientation.TOP, PortalGeometry.HORIZONTAL);

        assertEquals(SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE, SwirlVisualGeometry.visibleWidthScale(compact), EPSILON);
        assertEquals(SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE, SwirlVisualGeometry.visibleHeightScale(compact), EPSILON);
        assertEquals(SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE, SwirlVisualGeometry.visibleWidthScale(top), EPSILON);
        assertEquals(SwirlVisualGeometry.HORIZONTAL_VISIBLE_SIZE, SwirlVisualGeometry.visibleHeightScale(top), EPSILON);
    }

    @Test
    void floatingPortalKeepsItsAgreedVisibleSize() {
        PortalPlacement floating = placement(PortalOrientation.VERTICAL, PortalGeometry.FLOATING_EXPANDED);

        assertEquals(1.0, SwirlVisualGeometry.visibleWidthScale(floating), EPSILON);
        assertEquals(1.0, SwirlVisualGeometry.visibleHeightScale(floating), EPSILON);
    }

    private static PortalPlacement placement(PortalOrientation orientation, PortalGeometry geometry) {
        return new PortalPlacement(Vec3.ZERO, orientation, geometry, 0.0F, BlockPos.ZERO, Direction.NORTH);
    }
}
