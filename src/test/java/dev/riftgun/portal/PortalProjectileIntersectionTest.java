package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalProjectileIntersectionTest {
    private static final PortalPlacement PORTAL = new PortalPlacement(
        Vec3.ZERO, PortalOrientation.VERTICAL, PortalGeometry.FLOATING_VERTICAL,
        0.0F, null, null);

    @Test
    void detectsAHighSpeedCrossingBetweenTicks() {
        assertTrue(PortalProjectileIntersection.crosses(
            PORTAL, new Vec3(0.0, 0.0, -8.0), new Vec3(0.0, 0.0, 8.0), 0.1));
    }

    @Test
    void rejectsAPathOutsideTheVisibleFace() {
        assertFalse(PortalProjectileIntersection.crosses(
            PORTAL, new Vec3(4.0, 0.0, -8.0), new Vec3(4.0, 0.0, 8.0), 0.1));
    }

    @Test
    void projectileRadiusMayOverlapThePortalEdge() {
        double edge = PORTAL.geometry().width() * 0.5;
        assertTrue(PortalProjectileIntersection.crosses(
            PORTAL, new Vec3(edge + 0.2, 0.0, -2.0),
            new Vec3(edge + 0.2, 0.0, 2.0), 0.25));
    }

    @Test
    void reportsTheCrossingPointForNearestPortalOrdering() {
        assertEquals(0.5, PortalProjectileIntersection.crossingFraction(
            PORTAL, new Vec3(0.0, 0.0, -4.0), new Vec3(0.0, 0.0, 4.0), 0.1), 1.0E-9);
    }

    @Test
    void portalOnlyPreemptsAnImpactAtOrAfterItsCrossingPoint() {
        Vec3 start = new Vec3(0.0, 0.0, -1.0);
        Vec3 end = new Vec3(0.0, 0.0, 1.0);

        assertTrue(PortalProjectileIntersection.crossesBeforeImpact(
            PORTAL, start, end, 0.1, new Vec3(0.0, 0.0, 0.01)));
        assertFalse(PortalProjectileIntersection.crossesBeforeImpact(
            PORTAL, start, end, 0.1, new Vec3(0.0, 0.0, -0.01)));
    }
}
