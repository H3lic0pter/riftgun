package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.service.PortalPlacementIntent;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class SurfaceFaceOpenPlanTest {
    @Test
    void coordinateAndPairingRoutesPreserveTheSameExplicitIntent() {
        PortalPlacementIntent intent = intent();

        SurfaceFaceOpenPlan coordinate = SurfaceFaceOpenPlan.create(
            PortalPlacementMode.SURFACE, PortalFunctionMode.COORDINATE_TRAVEL, intent);
        SurfaceFaceOpenPlan pairing = SurfaceFaceOpenPlan.create(
            PortalPlacementMode.SMART, PortalFunctionMode.PORTAL_PAIRING, intent);

        assertEquals(SurfaceFaceOpenPlan.Route.COORDINATE, coordinate.route());
        assertEquals(SurfaceFaceOpenPlan.Route.PAIRING, pairing.route());
        assertSame(intent, coordinate.intent());
        assertSame(intent, pairing.intent());
    }

    @Test
    void rejectsModesWithoutSurfaceSemantics() {
        assertThrows(PortalRequestException.class, () -> SurfaceFaceOpenPlan.create(
            PortalPlacementMode.FRONT, PortalFunctionMode.COORDINATE_TRAVEL, intent()));
    }

    private static PortalPlacementIntent intent() {
        return PortalPlacementIntent.surface(new PortalPlacement(new Vec3(1, 2, 3),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL,
            0.0F, null, null));
    }
}
