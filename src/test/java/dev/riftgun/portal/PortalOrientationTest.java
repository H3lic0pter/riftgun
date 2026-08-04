package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalOrientationTest {
    @Test
    void horizontalBasisIsWorldAlignedInsteadOfFollowingPlayerYaw() {
        assertVec(new Vec3(0.0, 0.0, 1.0), PortalOrientation.TOP.up(73.0F));
        assertVec(new Vec3(1.0, 0.0, 0.0), PortalOrientation.TOP.right(73.0F));
        assertVec(new Vec3(0.0, 0.0, 1.0), PortalOrientation.BOTTOM.up(73.0F));
        assertVec(new Vec3(1.0, 0.0, 0.0), PortalOrientation.BOTTOM.right(73.0F));
    }

    @Test
    void horizontalSurfacePairsUseOppositeFaces() {
        assertEquals(PortalOrientation.BOTTOM, PortalOrientation.TOP.oppositeSurface());
        assertEquals(PortalOrientation.TOP, PortalOrientation.BOTTOM.oppositeSurface());
        assertEquals(PortalOrientation.VERTICAL, PortalOrientation.VERTICAL.oppositeSurface());
    }

    @Test
    void horizontalPortalAlwaysOccupiesExactlyOneByOneBlocks() {
        PortalPlacement placement = new PortalPlacement(new Vec3(0.5, 1.062, 0.5),
            PortalOrientation.TOP, PortalGeometry.HORIZONTAL, 45.0F, null, null);
        AABB bounds = placement.bounds();

        assertEquals(1.0, bounds.getXsize(), 1.0E-8);
        assertEquals(1.0, bounds.getZsize(), 1.0E-8);
        assertEquals(PortalPlacement.DEPTH, bounds.getYsize(), 1.0E-8);
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1.0E-8);
        assertEquals(expected.y, actual.y, 1.0E-8);
        assertEquals(expected.z, actual.z, 1.0E-8);
    }
}
