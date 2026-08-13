package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalTreeClearanceTest {
    @Test
    void correctionUsesThePassengerBoundNearestTheExitPlane() {
        PortalPlacement exit = new PortalPlacement(Vec3.ZERO, PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_VERTICAL, 0.0F, null, null);
        AABB vehicle = new AABB(-0.7, -0.3, 0.20, 0.7, 0.3, 1.60);
        AABB player = new AABB(-0.3, 0.0, 0.01, 0.3, 1.8, 0.61);

        double correction = PortalTreeClearance.outwardCorrection(
            exit, List.of(vehicle, player), 0.0);

        assertEquals(0.10, correction, 1.0E-9);
    }
}
