package dev.riftgun.network;

import dev.riftgun.data.PortalPlacementMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalGunActionsTest {
    @Test
    void cyclesForwardAndBackward() {
        assertEquals(PortalPlacementMode.FRONT,
            PortalGunActions.adjacentAvailableMode(PortalPlacementMode.SMART, false, true));
        assertEquals(PortalPlacementMode.ENTITY_RELOCATION,
            PortalGunActions.adjacentAvailableMode(PortalPlacementMode.SMART, true, true));
    }

    @Test
    void skipsUnavailableEntityRelocationInBothDirections() {
        assertEquals(PortalPlacementMode.SMART,
            PortalGunActions.adjacentAvailableMode(PortalPlacementMode.SURFACE, false, false));
        assertEquals(PortalPlacementMode.SURFACE,
            PortalGunActions.adjacentAvailableMode(PortalPlacementMode.SMART, true, false));
    }

    @Test
    void remoteAvailabilityIsIndependentFromOtherOptionalModes() {
        assertEquals(PortalPlacementMode.SURFACE,
            PortalGunActions.adjacentAvailableMode(
                PortalPlacementMode.FRONT, false, true, false));
        assertEquals(PortalPlacementMode.REMOTE,
            PortalGunActions.adjacentAvailableMode(
                PortalPlacementMode.FRONT, false, false, true));
    }
}
