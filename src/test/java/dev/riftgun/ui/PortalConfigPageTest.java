package dev.riftgun.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalConfigPageTest {
    @Test
    void formAndConfirmationSemanticsStayCentralized() {
        assertTrue(PortalConfigPage.CREATE_COORDINATE.isDestinationForm());
        assertTrue(PortalConfigPage.CREATE_COORDINATE.hasInputs());
        assertTrue(PortalConfigPage.CONFIRM_DIRTY.isConfirmation());
        assertFalse(PortalConfigPage.SETTINGS.hasInputs());
    }

    @Test
    void everyNestedGunPageReturnsToGunSettings() {
        assertTrue(PortalConfigPage.REMOTE_SETTINGS.isGunSettingPage());
        assertTrue(PortalConfigPage.PORTAL_PAIRING_SETTINGS.isGunSettingPage());
        assertFalse(PortalConfigPage.GUN_SETTINGS.isGunSettingPage());
    }
}
