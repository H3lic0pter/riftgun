package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalActionTest {
    @Test
    void onlyPrivacyTerminalActionsWorkWithoutAGun() {
        assertFalse(PortalAction.SET_PRIVACY.requiresPortalGun());
        assertFalse(PortalAction.SET_PRIVACY_OVERRIDE.requiresPortalGun());
        assertFalse(PortalAction.REQUEST_PRIVACY_PLAYERS.requiresPortalGun());
        assertTrue(PortalAction.OPEN_GUI.requiresPortalGun());
        assertTrue(PortalAction.OPEN_PORTAL.requiresPortalGun());
    }
}
