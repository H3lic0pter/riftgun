package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalTriggerShapeTest {
    @Test
    void verticalPortalRequiresTheEntityToTouchItsThinPlane() {
        PortalPlacement placement = placement(PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL);

        assertFalse(PortalTriggerShape.intersects(placement,
            new AABB(-0.3, -0.9, 0.08, 0.3, 0.9, 0.68)));
        assertTrue(PortalTriggerShape.intersects(placement,
            new AABB(-0.3, -0.9, 0.05, 0.3, 0.9, 0.65)));
    }

    @Test
    void horizontalPortalDoesNotTriggerAtTheSupportingBlockEdge() {
        PortalPlacement placement = placement(PortalOrientation.TOP, PortalGeometry.HORIZONTAL);

        assertFalse(PortalTriggerShape.intersects(placement,
            new AABB(0.39, 0.05, -0.2, 0.99, 1.85, 0.2)));
        assertTrue(PortalTriggerShape.intersects(placement,
            new AABB(0.0, 0.05, -0.2, 0.60, 1.85, 0.2)));
    }

    @Test
    void expandedHorizontalPortalUsesItsLargerGeometryButKeepsEdgeInset() {
        PortalPlacement placement = placement(PortalOrientation.TOP, PortalGeometry.HORIZONTAL_EXPANDED);

        assertTrue(PortalTriggerShape.intersects(placement,
            new AABB(0.70, 0.05, -0.05, 0.80, 1.85, 0.05)));
        assertFalse(PortalTriggerShape.intersects(placement,
            new AABB(0.90, 0.05, -0.05, 1.00, 1.85, 0.05)));
    }

    @Test
    void configuredHorizontalReachIsAnExplicitInput() {
        PortalPlacement placement = placement(PortalOrientation.TOP, PortalGeometry.HORIZONTAL);
        AABB fallingPlayer = new AABB(-0.2, 0.5, -0.2, 0.2, 2.3, 0.2);

        assertFalse(PortalTriggerShape.intersects(placement, fallingPlayer, 0.0));
        assertTrue(PortalTriggerShape.intersects(placement, fallingPlayer, 0.45));
    }

    private static PortalPlacement placement(PortalOrientation orientation, PortalGeometry geometry) {
        return new PortalPlacement(Vec3.ZERO, orientation, geometry, 0.0F, null, null);
    }
}
