package dev.riftgun.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingSettings;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.relocation.EntityRelocationSettings;
import dev.riftgun.remote.RemoteSettings;
import dev.riftgun.navigation.DimensionalTraversalSettings;
import java.util.Optional;

/** Compatibility adapter between the grouped domain model and the original flat data schema. */
final class PortalGunModuleSettingsCodec {
    static final Codec<PortalGunModuleSettings> CODEC = Stored.CODEC.xmap(
        Stored::toSettings, Stored::fromSettings);

    private record Stored(
        int smartDistance,
        // Legacy field name retained on disk; its value now configures Remote placement distance.
        int desiredSurfaceRange,
        boolean passiveTransitEnabled,
        boolean hostileTransitEnabled,
        boolean bossTransitEnabled,
        boolean projectileTransitEnabled,
        int portalDurationSeconds,
        boolean expandedApertureEnabled,
        boolean playerTargetEnabled,
        PlayerExcludeMode playerExcludeMode,
        int transitCooldownTenths,
        boolean entityRelocationEnabled,
        boolean entityRelocationSmartRouting,
        PairingAndRemote pairingAndRemote,
        boolean fallGuardEnabled,
        boolean fallGuardEntitiesEnabled
    ) {
        private static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("smart_distance", PortalGunModuleSettings.DEFAULT_SMART_DISTANCE)
                .forGetter(Stored::smartDistance),
            Codec.INT.optionalFieldOf("desired_surface_range", PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE)
                .forGetter(Stored::desiredSurfaceRange),
            Codec.BOOL.optionalFieldOf("passive_transit_enabled", true).forGetter(Stored::passiveTransitEnabled),
            Codec.BOOL.optionalFieldOf("hostile_transit_enabled", true).forGetter(Stored::hostileTransitEnabled),
            Codec.BOOL.optionalFieldOf("boss_transit_enabled", true).forGetter(Stored::bossTransitEnabled),
            Codec.BOOL.optionalFieldOf("projectile_transit_enabled", true)
                .forGetter(Stored::projectileTransitEnabled),
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
            PairingAndRemote.MAP_CODEC.forGetter(Stored::pairingAndRemote),
            Codec.BOOL.optionalFieldOf("fall_guard_enabled", true).forGetter(Stored::fallGuardEnabled),
            Codec.BOOL.optionalFieldOf("fall_guard_entities_enabled", false)
                .forGetter(Stored::fallGuardEntitiesEnabled)
        ).apply(instance, Stored::new));

        PortalGunModuleSettings toSettings() {
            return new PortalGunModuleSettings(
                new PortalGunModuleSettings.Placement(smartDistance, desiredSurfaceRange),
                new PortalGunModuleSettings.Transit(passiveTransitEnabled, hostileTransitEnabled,
                    bossTransitEnabled, projectileTransitEnabled, transitCooldownTenths),
                new PortalGunModuleSettings.Duration(portalDurationSeconds), expandedApertureEnabled,
                new PortalGunModuleSettings.PlayerTarget(playerTargetEnabled, playerExcludeMode),
                new EntityRelocationSettings(entityRelocationEnabled, entityRelocationSmartRouting),
                pairingAndRemote.remoteSettings(), pairingAndRemote.pairing().settings(),
                pairingAndRemote.dimensionalTraversal(),
                fallGuardEnabled, fallGuardEntitiesEnabled);
        }

        static Stored fromSettings(PortalGunModuleSettings settings) {
            return new Stored(settings.smartDistance(), settings.desiredRemoteDistance(),
                settings.passiveTransitEnabled(), settings.hostileTransitEnabled(),
                settings.bossTransitEnabled(), settings.projectileTransitEnabled(),
                settings.portalDurationSeconds(),
                settings.expandedApertureEnabled(), settings.playerTargetEnabled(),
                settings.playerExcludeMode(), settings.transitCooldownTenths(),
                settings.entityRelocation().enabled(), settings.entityRelocation().smartRouting(),
                PairingAndRemote.fromSettings(settings),
                settings.fallGuardEnabled(), settings.fallGuardEntitiesEnabled());
        }
    }

    /** Reads Remote's old nested location while writing it at module-settings scope. */
    private record PairingAndRemote(PersistedPairing pairing, Optional<RemoteSettings> remote,
                                    DimensionalTraversalSettings dimensionalTraversal) {
        private static final MapCodec<PairingAndRemote> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                PersistedPairing.CODEC.optionalFieldOf("portal_pairing", PersistedPairing.defaults())
                    .forGetter(PairingAndRemote::pairing),
                RemoteSettings.CODEC.optionalFieldOf("remote").forGetter(PairingAndRemote::remote),
                DimensionalTraversalSettings.CODEC.optionalFieldOf(
                        "dimensional_traversal", DimensionalTraversalSettings.defaults())
                    .forGetter(PairingAndRemote::dimensionalTraversal)
            ).apply(instance, PairingAndRemote::new));

        RemoteSettings remoteSettings() {
            return remote.orElseGet(pairing::legacyRemote);
        }

        static PairingAndRemote fromSettings(PortalGunModuleSettings settings) {
            return new PairingAndRemote(PersistedPairing.fromSettings(settings.portalPairing()),
                Optional.of(settings.remote()), settings.dimensionalTraversal());
        }
    }

    /** Current Pairing fields plus Remote's legacy nested read path. */
    private record PersistedPairing(
        PortalFunctionMode functionMode,
        PortalFloatingFallback coordinateSmartFallback,
        PortalFloatingFallback pairingSmartFallback,
        Optional<RemoteSettings> remote
    ) {
        private static final Codec<PersistedPairing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PortalFunctionMode.CODEC.optionalFieldOf(
                    "function_mode", PortalFunctionMode.COORDINATE_TRAVEL)
                .forGetter(PersistedPairing::functionMode),
            PortalFloatingFallback.CODEC.optionalFieldOf(
                    "coordinate_smart_fallback", PortalFloatingFallback.FRONT)
                .forGetter(PersistedPairing::coordinateSmartFallback),
            PortalFloatingFallback.CODEC.optionalFieldOf(
                    "pairing_smart_fallback", PortalFloatingFallback.FRONT)
                .forGetter(PersistedPairing::pairingSmartFallback),
            RemoteSettings.CODEC.optionalFieldOf("remote").forGetter(PersistedPairing::remote)
        ).apply(instance, PersistedPairing::new));

        static PersistedPairing defaults() {
            return new PersistedPairing(PortalFunctionMode.COORDINATE_TRAVEL,
                PortalFloatingFallback.FRONT, PortalFloatingFallback.FRONT, Optional.empty());
        }

        PortalPairingSettings settings() {
            return new PortalPairingSettings(functionMode, pairingSmartFallback);
        }

        RemoteSettings legacyRemote() {
            return remote.orElseGet(() -> new RemoteSettings(
                coordinateSmartFallback, true, true, true));
        }

        static PersistedPairing fromSettings(PortalPairingSettings settings) {
            return new PersistedPairing(settings.functionMode(), PortalFloatingFallback.FRONT,
                settings.smartFallback(), Optional.empty());
        }

    }

    private PortalGunModuleSettingsCodec() {}
}
