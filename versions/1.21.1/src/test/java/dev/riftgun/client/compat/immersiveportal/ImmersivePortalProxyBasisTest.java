package dev.riftgun.client.compat.immersiveportal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.portal.PortalOrientation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class ImmersivePortalProxyBasisTest {
    @Test
    void horizontalAxesPreserveTopAndBottomNormals() {
        assertNormal(PortalOrientation.TOP);
        assertNormal(PortalOrientation.BOTTOM);
    }

    @Test
    void verticalAxesRemainUnchanged() {
        float yaw = 37.0F;
        ImmersivePortalProxyBasis basis = ImmersivePortalProxyBasis.orient(
            PortalOrientation.VERTICAL.right(yaw), PortalOrientation.VERTICAL.up(yaw),
            PortalOrientation.VERTICAL.normal(yaw));

        assertVec(PortalOrientation.VERTICAL.right(yaw), basis.right());
        assertVec(PortalOrientation.VERTICAL.up(yaw), basis.up());
    }

    @Test
    void horizontalEntryViewPointsOutOfTheOppositeFace() {
        assertViewDirection(PortalOrientation.TOP, PortalOrientation.BOTTOM);
        assertViewDirection(PortalOrientation.BOTTOM, PortalOrientation.TOP);
    }

    private static void assertNormal(PortalOrientation orientation) {
        ImmersivePortalProxyBasis basis = ImmersivePortalProxyBasis.orient(
            orientation.right(0.0F), orientation.up(0.0F), orientation.normal(0.0F));
        assertVec(orientation.normal(0.0F), basis.right().cross(basis.up()));
    }

    private static void assertViewDirection(PortalOrientation sourceOrientation,
                                            PortalOrientation targetOrientation) {
        ImmersivePortalProxyBasis source = ImmersivePortalProxyBasis.orient(
            sourceOrientation.right(0.0F), sourceOrientation.up(0.0F),
            sourceOrientation.normal(0.0F));
        ImmersivePortalProxyBasis target = ImmersivePortalProxyBasis.orient(
            targetOrientation.right(0.0F), targetOrientation.up(0.0F),
            targetOrientation.normal(0.0F));
        Vec3 transformedView = ImmersivePortalProxyBasis.transformView(
            source, target, sourceOrientation.normal(0.0F).scale(-1.0));

        assertVec(targetOrientation.normal(0.0F), transformedView);
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, 1.0E-8);
        assertEquals(expected.y, actual.y, 1.0E-8);
        assertEquals(expected.z, actual.z, 1.0E-8);
    }
}
