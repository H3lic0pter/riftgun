package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalExitClearanceTest {
    @Test
    void projectileBoundsStartCompletelyBeyondEveryExitPlane() {
        for (PortalOrientation orientation : PortalOrientation.values()) {
            assertClearance(orientation);
        }
    }

    private static void assertClearance(PortalOrientation orientation) {
        PortalPlacement exit = new PortalPlacement(Vec3.ZERO, orientation,
            orientation == PortalOrientation.VERTICAL
                ? PortalGeometry.SURFACE_VERTICAL : PortalGeometry.HORIZONTAL,
            0.0F, null, null);
        double width = 0.5;
        double height = 0.25;

        Vec3 position = PortalExitClearance.projectilePosition(exit, width, height);
        double nearestBound = switch (orientation) {
            case VERTICAL -> position.dot(exit.normal()) - width * 0.5;
            case TOP -> position.y;
            case BOTTOM -> -position.y - height;
        };

        assertEquals(PortalPlacement.DEPTH * 0.5 + PortalExitClearance.EPSILON,
            nearestBound, 1.0E-9, orientation.name());
    }
}
