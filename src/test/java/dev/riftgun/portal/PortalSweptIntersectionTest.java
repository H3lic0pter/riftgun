package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalSweptIntersectionTest {
    private static final PortalPlacement PORTAL = new PortalPlacement(
        Vec3.ZERO, PortalOrientation.VERTICAL, PortalGeometry.FLOATING_VERTICAL,
        0.0F, null, null);

    @Test
    void detectsAHighSpeedCrossingBetweenTicks() {
        assertTrue(PortalSweptIntersection.crosses(
            PORTAL, new Vec3(0.0, 0.0, -8.0), new Vec3(0.0, 0.0, 8.0), 0.1));
    }

    @Test
    void rejectsAPathOutsideTheVisibleFace() {
        assertFalse(PortalSweptIntersection.crosses(
            PORTAL, new Vec3(4.0, 0.0, -8.0), new Vec3(4.0, 0.0, 8.0), 0.1));
    }

    @Test
    void projectileRadiusMayOverlapThePortalEdge() {
        double edge = PORTAL.geometry().width() * 0.5;
        assertTrue(PortalSweptIntersection.crosses(
            PORTAL, new Vec3(edge + 0.2, 0.0, -2.0),
            new Vec3(edge + 0.2, 0.0, 2.0), 0.25));
    }

    @Test
    void reportsTheCrossingPointForNearestPortalOrdering() {
        assertEquals(0.5, PortalSweptIntersection.crossingFraction(
            PORTAL, new Vec3(0.0, 0.0, -4.0), new Vec3(0.0, 0.0, 4.0), 0.1), 1.0E-9);
    }

    @Test
    void portalOnlyPreemptsAnImpactAtOrAfterItsCrossingPoint() {
        Vec3 start = new Vec3(0.0, 0.0, -1.0);
        Vec3 end = new Vec3(0.0, 0.0, 1.0);

        assertTrue(PortalSweptIntersection.crossesBeforeImpact(
            PORTAL, start, end, 0.1, new Vec3(0.0, 0.0, 0.01)));
        assertFalse(PortalSweptIntersection.crossesBeforeImpact(
            PORTAL, start, end, 0.1, new Vec3(0.0, 0.0, -0.01)));
    }

    @Test
    void nextTickPredictionIncludesGravityBeforeFallingBlockLanding() {
        PortalPlacement floorPortal = new PortalPlacement(
            Vec3.ZERO, PortalOrientation.BOTTOM, PortalGeometry.HORIZONTAL,
            0.0F, null, null);
        Vec3 start = new Vec3(0.0, 0.03, 0.0);
        Vec3 end = start.add(SweptPortalIndex.predictedMovement(Vec3.ZERO, 0.04));

        assertTrue(PortalSweptIntersection.crosses(floorPortal, start, end, 0.1));
    }

    @Test
    void collisionLimitedEndpointCannotReachAPortalBehindAnObstacle() {
        Vec3 start = new Vec3(0.0, 0.0, -2.0);

        assertTrue(PortalSweptIntersection.crosses(
            PORTAL, start, new Vec3(0.0, 0.0, 2.0), 0.1));
        assertFalse(PortalSweptIntersection.crosses(
            PORTAL, start, new Vec3(0.0, 0.0, -0.1), 0.1));
    }
}
