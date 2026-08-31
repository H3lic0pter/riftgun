package dev.riftgun.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.riftgun.service.PortalShortcutGunMode;
import org.junit.jupiter.api.Test;

final class PortalMissingGunFeedbackTest {
    @Test
    void everyExclusiveShortcutReportsTheConfiguredGunScope() {
        for (PortalAction action : PortalAction.values()) {
            if (!action.isExclusiveKeyboardShortcut()) continue;
            assertEquals("message.riftgun.portal_gun_must_be_held",
                PortalMissingGunFeedback.messageKey(
                    action, true, PortalShortcutGunMode.HELD_HANDS), action.name());
            assertEquals("message.riftgun.no_portal_gun",
                PortalMissingGunFeedback.messageKey(
                    action, true, PortalShortcutGunMode.REGISTERED_LOCATORS), action.name());
        }
    }

    @Test
    void guiAndNonShortcutRequestsPreserveTheirMessages() {
        assertEquals("message.riftgun.no_portal_gun",
            PortalMissingGunFeedback.messageKey(
                PortalAction.OPEN_GUI, false, PortalShortcutGunMode.HELD_HANDS));
        assertEquals("message.riftgun.no_portal_gun",
            PortalMissingGunFeedback.messageKey(
                PortalAction.OPEN_PORTAL, false, PortalShortcutGunMode.HELD_HANDS));
        assertNull(PortalMissingGunFeedback.messageKey(
            PortalAction.CYCLE_PLACEMENT_MODE, false, PortalShortcutGunMode.HELD_HANDS));
    }
}
