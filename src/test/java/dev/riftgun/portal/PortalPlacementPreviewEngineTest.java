package dev.riftgun.portal;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlacementPreviewEngineTest {
    @Test
    void shiftRouteActivatesForPairingEntityMode() {
        assertTrue(PortalPlacementPreviewEngine.usesShiftRoutedPreview(gun(
            PortalFunctionMode.PORTAL_PAIRING, PortalPlacementMode.ENTITY_RELOCATION,
            PortalFloatingFallback.FRONT, false)));
    }

    @Test
    void shiftRouteActivatesForSmartRemoteFallback() {
        assertTrue(PortalPlacementPreviewEngine.usesShiftRoutedPreview(gun(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalPlacementMode.SMART,
            PortalFloatingFallback.REMOTE, true)));
    }

    @Test
    void ordinaryRemoteModeDoesNotUseShiftRoute() {
        assertFalse(PortalPlacementPreviewEngine.usesShiftRoutedPreview(gun(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalPlacementMode.REMOTE,
            PortalFloatingFallback.FRONT, true)));
    }

    @Test
    void emptyFrameReportsNoGeometry() {
        var frame = new PortalPlacementPreviewEngine.Frame(List.of(), List.of(), List.of());
        assertTrue(frame.isEmpty());
    }

    private static PortalPlacementPreviewEngine.Gun gun(PortalFunctionMode function,
                                                        PortalPlacementMode placement,
                                                        PortalFloatingFallback fallback,
                                                        boolean remote) {
        return new PortalPlacementPreviewEngine.Gun(function, placement, fallback,
            16, 16, 64, PortalAperture.STANDARD, remote, true, null);
    }
}
