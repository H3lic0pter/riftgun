package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

final class PortalGunModuleSettingsTest {
    @Test
    void keepsIndependentPerCapabilityPreferences() {
        PortalGunModuleSettings original = PortalGunModuleSettings.defaults(8);
        PortalGunModuleSettings changed = original
            .withSmartDistance(12)
            .withDesiredSurfaceRange(64)
            .withTransit(PortalModuleKind.HOSTILE_TRANSIT, false)
            .withPortalDurationSeconds(12)
            .withExpandedApertureEnabled(false);

        assertEquals(12, changed.smartDistance());
        assertEquals(64, changed.desiredSurfaceRange());
        assertTrue(changed.passiveTransitEnabled());
        assertFalse(changed.hostileTransitEnabled());
        assertTrue(changed.bossTransitEnabled());
        assertEquals(12, changed.portalDurationSeconds());
        assertFalse(changed.expandedApertureEnabled());
    }

    @Test
    void clampsPersistedDistancesToPositiveValues() {
        PortalGunModuleSettings settings = new PortalGunModuleSettings(
            -4, 0, true, true, true, 0, true);

        assertEquals(1, settings.smartDistance());
        assertEquals(1, settings.desiredSurfaceRange());
        assertEquals(1, settings.portalDurationSeconds());
    }

    @Test
    void oldCodecPayloadReceivesNewSettingDefaults() {
        var json = JsonParser.parseString("""
            {
              "smart_distance": 9,
              "desired_surface_range": 48,
              "passive_transit_enabled": false,
              "hostile_transit_enabled": true,
              "boss_transit_enabled": false
            }
            """);

        PortalGunModuleSettings settings = PortalGunModuleSettings.CODEC.parse(JsonOps.INSTANCE, json)
            .result().orElseThrow();

        assertEquals(3, settings.portalDurationSeconds());
        assertTrue(settings.expandedApertureEnabled());
    }
}
