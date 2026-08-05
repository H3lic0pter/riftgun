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

    private static PortalPlacement placement(PortalOrientation orientation, PortalGeometry geometry) {
        return new PortalPlacement(Vec3.ZERO, orientation, geometry, 0.0F, null, null);
    }
}
