package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.service.PortalPlacementIntent;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class SurfaceFaceRequestValidatorTest {
    @Test
    void acceptsSurfaceIntentForSurfaceCapableModes() {
        PortalPlacementIntent intent = intent();

        assertDoesNotThrow(() -> SurfaceFaceRequestValidator.validate(
            PortalPlacementMode.SURFACE, intent));
        assertDoesNotThrow(() -> SurfaceFaceRequestValidator.validate(
            PortalPlacementMode.SMART, intent));
    }

    @Test
    void rejectsModesWithoutSurfaceSemantics() {
        assertThrows(PortalRequestException.class, () -> SurfaceFaceRequestValidator.validate(
            PortalPlacementMode.FRONT, intent()));
    }

    private static PortalPlacementIntent intent() {
        return PortalPlacementIntent.surface(new PortalPlacement(new Vec3(1, 2, 3),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL,
            0.0F, null, null));
    }
}
