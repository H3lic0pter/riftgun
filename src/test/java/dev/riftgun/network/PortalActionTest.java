package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalActionTest {
    @Test
    void privacyAndPortalClosingWorkWithoutAGun() {
        assertFalse(PortalAction.SET_PRIVACY.requiresPortalGun());
        assertFalse(PortalAction.SET_PRIVACY_OVERRIDE.requiresPortalGun());
        assertFalse(PortalAction.REQUEST_PRIVACY_PLAYERS.requiresPortalGun());
        assertFalse(PortalAction.CLOSE_PORTALS.requiresPortalGun());
        assertTrue(PortalAction.OPEN_GUI.requiresPortalGun());
        assertTrue(PortalAction.OPEN_PORTAL.requiresPortalGun());
    }

    @Test
    void forcedOpenAndPlacementCycleAreExclusiveKeyboardShortcuts() {
        assertTrue(PortalAction.OPEN_SELECTED.isExclusiveKeyboardShortcut());
        assertTrue(PortalAction.CYCLE_PLACEMENT_MODE.isExclusiveKeyboardShortcut());
        assertFalse(PortalAction.OPEN_GUI.isExclusiveKeyboardShortcut());
        assertFalse(PortalAction.OPEN_PORTAL.isExclusiveKeyboardShortcut());
    }
}
