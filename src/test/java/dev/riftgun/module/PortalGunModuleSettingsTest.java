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
            .withTransit(PortalModuleKind.PROJECTILE_TRANSIT, false)
            .withPortalDurationSeconds(12)
            .withExpandedApertureEnabled(false);

        assertEquals(12, changed.smartDistance());
        assertEquals(64, changed.desiredSurfaceRange());
        assertTrue(changed.passiveTransitEnabled());
        assertFalse(changed.hostileTransitEnabled());
        assertTrue(changed.bossTransitEnabled());
        assertFalse(changed.projectileTransitEnabled());
        assertEquals(12, changed.portalDurationSeconds());
        assertFalse(changed.expandedApertureEnabled());
    }

    @Test
    void disablingEntityRelocationPreservesSmartRoutingPreference() {
        PortalGunModuleSettings settings = PortalGunModuleSettings.defaults(8)
            .withEntityRelocationSmartRouting(true)
            .withEntityRelocationEnabled(false);

        assertFalse(settings.entityRelocation().enabled());
        assertTrue(settings.entityRelocation().smartRouting());
        assertTrue(settings.withEntityRelocationEnabled(true).entityRelocation().smartRouting());
    }

    @Test
    void clampsPersistedDistancesToPositiveValues() {
        PortalGunModuleSettings settings = new PortalGunModuleSettings(
            new PortalGunModuleSettings.Placement(-4, 0),
            new PortalGunModuleSettings.Transit(true, true, true, -1),
            new PortalGunModuleSettings.Duration(0), true,
            new PortalGunModuleSettings.PlayerTarget(true, PlayerExcludeMode.ENTRY_AND_EXIT),
            dev.riftgun.relocation.EntityRelocationSettings.defaults(),
            dev.riftgun.pairing.PortalPairingSettings.defaults(), true, false);

        assertEquals(1, settings.smartDistance());
        assertEquals(1, settings.desiredSurfaceRange());
        assertEquals(1, settings.portalDurationSeconds());
        assertEquals(0, settings.transitCooldownTenths());
        assertFalse(settings.fallGuardEntitiesEnabled());
        assertEquals(dev.riftgun.pairing.PortalFunctionMode.COORDINATE_TRAVEL,
            settings.portalPairing().functionMode());
        assertEquals(dev.riftgun.pairing.PortalFloatingFallback.FRONT,
            settings.portalPairing().coordinateSmartFallback());
        assertTrue(settings.projectileTransitEnabled());
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
        assertEquals(PlayerExcludeMode.ENTRY_AND_EXIT, settings.playerExcludeMode());
        assertFalse(settings.fallGuardEntitiesEnabled());
    }

    @Test
    void playerExcludeModeCyclesWithoutMagicNumbers() {
        assertEquals(PlayerExcludeMode.EXIT_ONLY, PlayerExcludeMode.ENTRY_AND_EXIT.step(1));
        assertEquals(PlayerExcludeMode.OFF, PlayerExcludeMode.EXIT_ONLY.step(1));
        assertEquals(PlayerExcludeMode.EXIT_ONLY, PlayerExcludeMode.OFF.step(-1));
        assertEquals(PlayerExcludeMode.ENTRY_AND_EXIT, PlayerExcludeMode.byId(99));
    }

    @Test
    void groupedModelKeepsTheOriginalFlatCodecSchema() {
        PortalGunModuleSettings settings = PortalGunModuleSettings.defaults(11)
            .withDesiredSurfaceRange(48)
            .withTransitCooldownTenths(7);

        var encoded = PortalGunModuleSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings)
            .result().orElseThrow().getAsJsonObject();
        assertEquals(11, encoded.get("smart_distance").getAsInt());
        assertEquals(48, encoded.get("desired_surface_range").getAsInt());
        assertEquals(7, encoded.get("transit_cooldown_tenths").getAsInt());
        assertFalse(encoded.has("projectile_transit_enabled"));
        assertFalse(encoded.has("placement"));
        assertFalse(encoded.has("transit"));
    }
}
