package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.input.SurfaceFacePreviewState;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.service.PrecisionPlacementIntent;
import dev.riftgun.service.SurfaceFaceSelection;
import dev.riftgun.state.PortalGunViewState;
import dev.riftgun.state.PortalGunViewStateFixtures;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

final class ModeRadialControllerTest {
    @Test
    void ordinaryRadialOwnsPageFunctionAndRangeState() {
        PortalGunViewState gun = PortalGunViewStateFixtures.representative();
        var controller = new ModeRadialController(null, Direction.NORTH, List.of());
        controller.refresh(gun);

        assertEquals(List.of(PortalPlacementMode.values()), controller.options(gun));
        assertEquals(PortalFunctionMode.COORDINATE_TRAVEL,
            controller.toggleFunctionMode());
        controller.switchPage();
        assertEquals(ModeRadialController.Page.PREDICTION, controller.page());

        assertEquals(96, controller.updateRange(1.0));
        assertFalse(controller.rangeSendDue(false, true, true, 99_999_999L));
        assertTrue(controller.rangeSendDue(false, true, true, 100_000_000L));
        controller.rangeSent(100_000_000L);
        assertFalse(controller.rangeSendDue(true, true, true, 100_000_001L));
    }

    @Test
    void surfacePreviewProducesAValidatedDomainIntent() {
        var intent = PrecisionPlacementIntent.surface(
            new SurfaceFaceSelection(new BlockPos(4, 70, -3), Direction.NORTH));
        var controller = new ModeRadialController(intent, Direction.EAST, List.of());
        controller.refresh(PortalGunViewStateFixtures.representative());

        assertEquals(ModeRadialController.Page.SURFACE_FACE, controller.page());
        assertTrue(controller.select(0, PortalGunViewStateFixtures.representative()));
        assertEquals(Direction.UP,
            controller.selectedPrecisionIntent(null, false).surface().face());
        assertEquals(SurfaceFacePreviewState.Frame.ABSOLUTE, controller.toggleFaceFrame());
    }

    @Test
    void floatingPreviewAndSmartFallbackUseTheSameSharedPolicyInBothVersions() {
        var controller = new ModeRadialController(
            PrecisionPlacementIntent.floating(PortalOrientation.TOP),
            Direction.NORTH, List.of());
        PortalGunViewState gun = PortalGunViewStateFixtures.representative();
        controller.refresh(gun);

        assertEquals(PortalPlacementMode.REMOTE,
            controller.floatingPlacementMode(PortalPlacementMode.SMART, gun));
        assertEquals(PortalOrientation.TOP,
            controller.selectedPrecisionIntent(null, false).orientation());
    }
}
