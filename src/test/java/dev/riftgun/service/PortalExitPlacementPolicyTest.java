package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class PortalExitPlacementPolicyTest {
    @Test
    void topExitCanExpandWithoutFullSupport() {
        PortalExitTarget target = target(new Vec3(10.2, 64.0, 20.8));

        PortalPlacement result = PortalExitPlacementPolicy.resolveHorizontal(
            target, PortalOrientation.TOP, PortalAperture.EXPANDED, new PortalExitPlacementPolicy.SpaceProbe() {
                @Override
                public boolean available(PortalPlacement placement) {
                    return true;
                }

                @Override
                public boolean hasTopSupport(BlockPos support) {
                    return false;
                }
            });

        assertEquals(PortalOrientation.TOP, result.orientation());
        assertEquals(PortalGeometry.HORIZONTAL_EXPANDED, result.geometry());
        assertFalse(result.anchored(), "Exit portals must not inherit entry support requirements");
    }

    @Test
    void bottomExitCanExpandWithoutCeilingSupport() {
        PortalExitTarget target = target(new Vec3(10.2, 64.0, 20.8));

        PortalPlacement result = PortalExitPlacementPolicy.resolveHorizontal(
            target, PortalOrientation.BOTTOM, PortalAperture.EXPANDED, openSpace(false));

        assertEquals(PortalOrientation.BOTTOM, result.orientation());
        assertEquals(PortalGeometry.HORIZONTAL_EXPANDED, result.geometry());
        assertFalse(result.anchored());
    }

    @Test
    void obstructedExpandedExitFallsBackToStandardGeometry() {
        PortalExitTarget target = target(new Vec3(10.2, 64.0, 20.8));

        PortalPlacement result = PortalExitPlacementPolicy.resolveHorizontal(
            target, PortalOrientation.BOTTOM, PortalAperture.EXPANDED, new PortalExitPlacementPolicy.SpaceProbe() {
                @Override
                public boolean available(PortalPlacement placement) {
                    return !placement.geometry().expanded();
                }

                @Override
                public boolean hasTopSupport(BlockPos support) {
                    return false;
                }
            });

        assertEquals(PortalOrientation.BOTTOM, result.orientation());
        assertEquals(PortalGeometry.HORIZONTAL, result.geometry());
    }

    @Test
    void standardTopExitWithoutFloorStillFallsBackSideways() {
        PortalExitTarget target = target(new Vec3(10.2, 64.0, 20.8));

        PortalPlacement result = PortalExitPlacementPolicy.resolveHorizontal(
            target, PortalOrientation.TOP, PortalAperture.STANDARD, openSpace(false));

        assertEquals(PortalOrientation.VERTICAL, result.orientation());
        assertEquals(PortalGeometry.SURFACE_VERTICAL, result.geometry());
    }

    private static PortalExitTarget target(Vec3 position) {
        return new PortalExitTarget(UUID.randomUUID(), Level.OVERWORLD, position, 0.0F);
    }

    private static PortalExitPlacementPolicy.SpaceProbe openSpace(boolean topSupported) {
        return new PortalExitPlacementPolicy.SpaceProbe() {
            @Override
            public boolean available(PortalPlacement placement) {
                return true;
            }

            @Override
            public boolean hasTopSupport(BlockPos support) {
                return topSupported;
            }
        };
    }
}
