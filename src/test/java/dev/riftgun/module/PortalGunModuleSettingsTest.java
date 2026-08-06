package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalGunModuleSettingsTest {
    @Test
    void keepsIndependentPerCapabilityPreferences() {
        PortalGunModuleSettings original = PortalGunModuleSettings.defaults(8);
        PortalGunModuleSettings changed = original
            .withSmartDistance(12)
            .withDesiredSurfaceRange(64)
            .withTransit(PortalModuleKind.HOSTILE_TRANSIT, false);

        assertEquals(12, changed.smartDistance());
        assertEquals(64, changed.desiredSurfaceRange());
        assertTrue(changed.passiveTransitEnabled());
        assertFalse(changed.hostileTransitEnabled());
        assertTrue(changed.bossTransitEnabled());
    }

    @Test
    void clampsPersistedDistancesToPositiveValues() {
        PortalGunModuleSettings settings = new PortalGunModuleSettings(-4, 0, true, true, true);

        assertEquals(1, settings.smartDistance());
        assertEquals(1, settings.desiredSurfaceRange());
    }
}
