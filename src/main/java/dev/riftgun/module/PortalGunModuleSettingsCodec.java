package dev.riftgun.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.relocation.EntityRelocationSettings;

/** Compatibility adapter between the grouped domain model and the original flat data schema. */
final class PortalGunModuleSettingsCodec {
    static final Codec<PortalGunModuleSettings> CODEC = Stored.CODEC.xmap(
        Stored::toSettings, Stored::fromSettings);

    private record Stored(
        int smartDistance,
        int desiredSurfaceRange,
        boolean passiveTransitEnabled,
        boolean hostileTransitEnabled,
        boolean bossTransitEnabled,
        int portalDurationSeconds,
        boolean expandedApertureEnabled,
        boolean playerTargetEnabled,
        PlayerExcludeMode playerExcludeMode,
        int transitCooldownTenths,
        boolean entityRelocationEnabled,
        boolean entityRelocationSmartRouting,
        boolean fallGuardEnabled
    ) {
        private static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("smart_distance", PortalGunModuleSettings.DEFAULT_SMART_DISTANCE)
                .forGetter(Stored::smartDistance),
            Codec.INT.optionalFieldOf("desired_surface_range", PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE)
                .forGetter(Stored::desiredSurfaceRange),
            Codec.BOOL.optionalFieldOf("passive_transit_enabled", true).forGetter(Stored::passiveTransitEnabled),
            Codec.BOOL.optionalFieldOf("hostile_transit_enabled", true).forGetter(Stored::hostileTransitEnabled),
            Codec.BOOL.optionalFieldOf("boss_transit_enabled", true).forGetter(Stored::bossTransitEnabled),
            Codec.INT.optionalFieldOf("portal_duration_seconds", PortalOpenDuration.DEFAULT_SECONDS)
                .forGetter(Stored::portalDurationSeconds),
            Codec.BOOL.optionalFieldOf("expanded_aperture_enabled", true)
                .forGetter(Stored::expandedApertureEnabled),
            Codec.BOOL.optionalFieldOf("player_target_enabled", true).forGetter(Stored::playerTargetEnabled),
            Codec.INT.optionalFieldOf("player_exclude_mode",
                    PortalGunModuleSettings.DEFAULT_PLAYER_EXCLUDE_MODE.id())
                .xmap(PlayerExcludeMode::byId, PlayerExcludeMode::id).forGetter(Stored::playerExcludeMode),
            Codec.INT.optionalFieldOf("transit_cooldown_tenths",
                    PortalGunModuleSettings.DEFAULT_TRANSIT_COOLDOWN_TENTHS)
                .forGetter(Stored::transitCooldownTenths),
            Codec.BOOL.optionalFieldOf("entity_relocation_enabled", true)
                .forGetter(Stored::entityRelocationEnabled),
            Codec.BOOL.optionalFieldOf("entity_relocation_smart_routing", false)
                .forGetter(Stored::entityRelocationSmartRouting),
            Codec.BOOL.optionalFieldOf("fall_guard_enabled", true).forGetter(Stored::fallGuardEnabled)
        ).apply(instance, Stored::new));

        PortalGunModuleSettings toSettings() {
            return new PortalGunModuleSettings(
                new PortalGunModuleSettings.Placement(smartDistance, desiredSurfaceRange),
                new PortalGunModuleSettings.Transit(passiveTransitEnabled, hostileTransitEnabled,
                    bossTransitEnabled, transitCooldownTenths),
                new PortalGunModuleSettings.Duration(portalDurationSeconds), expandedApertureEnabled,
                new PortalGunModuleSettings.PlayerTarget(playerTargetEnabled, playerExcludeMode),
                new EntityRelocationSettings(entityRelocationEnabled, entityRelocationSmartRouting),
                fallGuardEnabled);
        }

        static Stored fromSettings(PortalGunModuleSettings settings) {
            return new Stored(settings.smartDistance(), settings.desiredSurfaceRange(),
                settings.passiveTransitEnabled(), settings.hostileTransitEnabled(),
                settings.bossTransitEnabled(), settings.portalDurationSeconds(),
                settings.expandedApertureEnabled(), settings.playerTargetEnabled(),
                settings.playerExcludeMode(), settings.transitCooldownTenths(),
                settings.entityRelocation().enabled(), settings.entityRelocation().smartRouting(),
                settings.fallGuardEnabled());
        }
    }

    private PortalGunModuleSettingsCodec() {}
}
