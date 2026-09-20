package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class FrontPortalPlacementPlannerTest {
    private static final AABB PLAYER_BOUNDS = new AABB(-0.3, 64.0, -0.3, 0.3, 65.8, 0.3);

    @Test
    void verticalPlacementUsesTheSharedFixedFrontRule() {
        var result = resolve(PortalOrientation.VERTICAL, PortalAperture.STANDARD,
            (placement, exposure) -> true);

        assertTrue(result.successful());
        assertEquals(PortalGeometry.FLOATING_VERTICAL, result.placement().geometry());
        assertEquals(2.0, result.placement().center().z, 1.0E-9);
        assertEquals(65.1, result.placement().center().y, 1.0E-6);
    }

    @Test
    void expandedFailureFallsBackToStandardGeometryWithoutMovingThePortal() {
        var result = resolve(PortalOrientation.TOP, PortalAperture.EXPANDED,
            (placement, exposure) -> !placement.geometry().expanded());

        assertTrue(result.successful());
        assertEquals(PortalGeometry.HORIZONTAL, result.placement().geometry());
        assertEquals(63.938, result.placement().center().y, 1.0E-6);
    }

    @Test
    void obstructionIsReportedByTheSamePlannerUsedForPreviewAndServerPlacement() {
        var result = resolve(PortalOrientation.BOTTOM, PortalAperture.STANDARD,
            (placement, exposure) -> false);

        assertFalse(result.successful());
        assertEquals("message.riftgun.front_obstructed", result.errorKey());
    }

    @Test
    void allOrientationsCanOpenDeepInTheVoidWithoutBypassingObstruction() {
        Vec3 position = new Vec3(0.0, -200.0, 0.0);
        AABB bounds = PLAYER_BOUNDS.move(0.0, -264.0, 0.0);
        for (PortalOrientation orientation : PortalOrientation.values()) {
            for (PortalAperture aperture : PortalAperture.values()) {
                var result = FrontPortalPlacementPlanner.resolve(position, bounds, Vec3.ZERO,
                    0.0F, orientation, aperture, 2.0, 0.5, (placement, exposure) -> true);
                assertTrue(result.successful(), orientation + " " + aperture);
                assertTrue(result.placement().bounds().maxY < -128.0);

                var blocked = FrontPortalPlacementPlanner.resolve(position, bounds, Vec3.ZERO,
                    0.0F, orientation, aperture, 2.0, 0.5, (placement, exposure) -> false);
                assertEquals("message.riftgun.front_obstructed", blocked.errorKey());
            }
        }
    }

    private static FrontPortalPlacementPlanner.Result resolve(
        PortalOrientation orientation, PortalAperture aperture,
        FrontPortalPlacementPlanner.Probe probe
    ) {
        return FrontPortalPlacementPlanner.resolve(new Vec3(0.0, 64.0, 0.0), PLAYER_BOUNDS,
            Vec3.ZERO, 0.0F, orientation, aperture, 2.0,
            PortalPlacementCapabilities.DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE, probe);
    }
}
