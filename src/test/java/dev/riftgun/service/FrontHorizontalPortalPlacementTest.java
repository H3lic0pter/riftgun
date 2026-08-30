package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class FrontHorizontalPortalPlacementTest {
    private static final AABB PLAYER = new AABB(-0.3, 10.0, -0.3, 0.3, 11.8, 0.3);

    @Test
    void bottomOrientationPlacesPortalImmediatelyAboveHead() {
        Vec3 center = FrontHorizontalPortalPlacement.center(
            PLAYER, Vec3.ZERO, PortalOrientation.BOTTOM);

        double clearance = center.y - PortalPlacement.DEPTH * 0.5 - PLAYER.maxY;
        assertEquals(0.5, clearance, 1.0E-9);
        org.junit.jupiter.api.Assertions.assertTrue(clearance < 1.0,
            "the above-head portal must remain reachable with a normal jump");
    }

    @Test
    void topOrientationPlacesPortalImmediatelyBelowFeet() {
        Vec3 center = FrontHorizontalPortalPlacement.center(
            PLAYER, Vec3.ZERO, PortalOrientation.TOP);

        assertEquals(10.0 - PortalPlacement.DEPTH * 0.5 - 0.002, center.y, 1.0E-9);
    }

    @Test
    void predictionMovesTheAdjacentPortalWithPredictedPlayerBounds() {
        Vec3 center = FrontHorizontalPortalPlacement.center(
            PLAYER, new Vec3(2.0, 3.0, 4.0), PortalOrientation.BOTTOM);

        assertEquals(2.0, center.x, 1.0E-9);
        assertEquals(4.0, center.z, 1.0E-9);
        assertEquals(14.8 + PortalPlacement.DEPTH * 0.5 + 0.5, center.y, 1.0E-9);
    }

    @Test
    void rejectsVerticalOrientation() {
        assertThrows(IllegalArgumentException.class, () ->
            FrontHorizontalPortalPlacement.center(PLAYER, Vec3.ZERO, PortalOrientation.VERTICAL));
    }
}
