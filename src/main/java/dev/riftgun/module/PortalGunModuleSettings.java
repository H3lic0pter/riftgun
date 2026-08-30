package dev.riftgun.module;

import com.mojang.serialization.Codec;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.pairing.PortalPairingSettings;
import dev.riftgun.relocation.EntityRelocationSettings;
import dev.riftgun.remote.RemoteSettings;
import net.minecraft.world.item.ItemStack;

/** Persisted per-gun preferences grouped by the capability that owns each value. */
public record PortalGunModuleSettings(
    Placement placement,
    Transit transit,
    Duration duration,
    boolean expandedApertureEnabled,
    PlayerTarget playerTarget,
    EntityRelocationSettings entityRelocation,
    RemoteSettings remote,
    PortalPairingSettings portalPairing,
    boolean fallGuardEnabled,
    boolean fallGuardEntitiesEnabled
) {
    public static final int DEFAULT_SMART_DISTANCE = 8;
    public static final int MINIMUM_TRANSIT_COOLDOWN_TENTHS = 0;
    public static final int MAXIMUM_TRANSIT_COOLDOWN_TENTHS = 50;
    public static final int DEFAULT_TRANSIT_COOLDOWN_TENTHS = 10;
    public static final PlayerExcludeMode DEFAULT_PLAYER_EXCLUDE_MODE = PlayerExcludeMode.ENTRY_AND_EXIT;

    /** Flat persisted schema retained so existing Portal Guns decode without migration. */
    public static final Codec<PortalGunModuleSettings> CODEC = PortalGunModuleSettingsCodec.CODEC;

    public PortalGunModuleSettings {
        if (placement == null) placement = Placement.defaults(DEFAULT_SMART_DISTANCE);
        if (transit == null) transit = Transit.defaults();
        if (duration == null) duration = Duration.defaults();
        if (playerTarget == null) playerTarget = PlayerTarget.defaults();
        if (entityRelocation == null) entityRelocation = EntityRelocationSettings.defaults();
        if (remote == null) remote = RemoteSettings.defaults();
        if (portalPairing == null) portalPairing = PortalPairingSettings.defaults();
    }

    public static PortalGunModuleSettings defaults(int legacySmartDistance) {
        return new PortalGunModuleSettings(
            Placement.defaults(legacySmartDistance), Transit.defaults(), Duration.defaults(),
            true, PlayerTarget.defaults(), EntityRelocationSettings.defaults(),
            RemoteSettings.defaults(), PortalPairingSettings.defaults(), true, false);
    }

    public static PortalGunModuleSettings get(ItemStack gun, int legacySmartDistance) {
        return gun.getOrDefault(PortalGunComponents.MODULE_SETTINGS, defaults(legacySmartDistance));
    }

    public static PortalGunModuleSettings ensure(ItemStack gun, int legacySmartDistance) {
        PortalGunModuleSettings settings = gun.get(PortalGunComponents.MODULE_SETTINGS);
        if (settings != null) return settings;
        settings = defaults(legacySmartDistance);
        gun.set(PortalGunComponents.MODULE_SETTINGS, settings);
        return settings;
    }

    public void save(ItemStack gun) {
        gun.set(PortalGunComponents.MODULE_SETTINGS, this);
    }

    public int smartDistance() {
        return placement.smartDistance();
    }

    public int desiredRemoteDistance() {
        return placement.desiredRemoteDistance();
    }

    public boolean passiveTransitEnabled() {
        return transit.passiveEnabled();
    }

    public boolean hostileTransitEnabled() {
        return transit.hostileEnabled();
    }

    public boolean bossTransitEnabled() {
        return transit.bossEnabled();
    }

    public boolean projectileTransitEnabled() {
        return transit.projectileEnabled();
    }

    public int transitCooldownTenths() {
        return transit.cooldownTenths();
    }

    public int portalDurationSeconds() {
        return duration.seconds();
    }

    public boolean playerTargetEnabled() {
        return playerTarget.enabled();
    }

    public PlayerExcludeMode playerExcludeMode() {
        return playerTarget.excludeMode();
    }

    public PortalGunModuleSettings withSmartDistance(int value) {
        return new PortalGunModuleSettings(placement.withSmartDistance(value), transit, duration,
            expandedApertureEnabled, playerTarget, entityRelocation, remote, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withDesiredRemoteDistance(int value) {
        return new PortalGunModuleSettings(placement.withDesiredRemoteDistance(value), transit, duration,
            expandedApertureEnabled, playerTarget, entityRelocation, remote, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withTransit(PortalModuleKind kind, boolean enabled) {
        Transit updated = transit.withEnabled(kind, enabled);
        return updated == transit ? this : new PortalGunModuleSettings(
            placement, updated, duration, expandedApertureEnabled, playerTarget, entityRelocation,
            remote, portalPairing, fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withTransitCooldownTenths(int value) {
        return new PortalGunModuleSettings(placement, transit.withCooldown(value), duration,
            expandedApertureEnabled, playerTarget, entityRelocation, remote, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withPortalDurationSeconds(int value) {
        return new PortalGunModuleSettings(placement, transit, new Duration(value),
            expandedApertureEnabled, playerTarget, entityRelocation, remote, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withExpandedApertureEnabled(boolean enabled) {
        return new PortalGunModuleSettings(
            placement, transit, duration, enabled, playerTarget, entityRelocation,
            remote, portalPairing, fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withPlayerTargetEnabled(boolean enabled) {
        return new PortalGunModuleSettings(placement, transit, duration,
            expandedApertureEnabled, playerTarget.withEnabled(enabled), entityRelocation,
            remote, portalPairing, fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withPlayerExcludeMode(PlayerExcludeMode mode) {
        return new PortalGunModuleSettings(placement, transit, duration,
            expandedApertureEnabled, playerTarget.withExcludeMode(mode), entityRelocation,
            remote, portalPairing, fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withEntityRelocationEnabled(boolean enabled) {
        return new PortalGunModuleSettings(placement, transit, duration, expandedApertureEnabled,
            playerTarget, entityRelocation.withEnabled(enabled), remote, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withEntityRelocationSmartRouting(boolean enabled) {
        return new PortalGunModuleSettings(placement, transit, duration, expandedApertureEnabled,
            playerTarget, entityRelocation.withSmartRouting(enabled), remote, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withRemote(RemoteSettings value) {
        return new PortalGunModuleSettings(placement, transit, duration, expandedApertureEnabled,
            playerTarget, entityRelocation, value, portalPairing,
            fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withPortalPairing(PortalPairingSettings value) {
        return new PortalGunModuleSettings(placement, transit, duration, expandedApertureEnabled,
            playerTarget, entityRelocation, remote, value, fallGuardEnabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withFallGuardEnabled(boolean enabled) {
        return new PortalGunModuleSettings(
            placement, transit, duration, expandedApertureEnabled, playerTarget, entityRelocation,
            remote, portalPairing, enabled, fallGuardEntitiesEnabled);
    }

    public PortalGunModuleSettings withFallGuardEntitiesEnabled(boolean enabled) {
        return new PortalGunModuleSettings(
            placement, transit, duration, expandedApertureEnabled, playerTarget, entityRelocation,
            remote, portalPairing, fallGuardEnabled, enabled);
    }

    public record Placement(int smartDistance, int desiredRemoteDistance) {
        public Placement {
            smartDistance = Math.max(1, smartDistance);
            desiredRemoteDistance = Math.max(1, desiredRemoteDistance);
        }

        static Placement defaults(int legacySmartDistance) {
            return new Placement(Math.max(1, legacySmartDistance),
                PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE);
        }

        Placement withSmartDistance(int value) {
            return new Placement(value, desiredRemoteDistance);
        }

        Placement withDesiredRemoteDistance(int value) {
            return new Placement(smartDistance, value);
        }
    }

    public record Transit(boolean passiveEnabled, boolean hostileEnabled,
                          boolean bossEnabled, boolean projectileEnabled, int cooldownTenths) {
        public Transit(boolean passiveEnabled, boolean hostileEnabled,
                       boolean bossEnabled, int cooldownTenths) {
            this(passiveEnabled, hostileEnabled, bossEnabled, true, cooldownTenths);
        }

        public Transit {
            cooldownTenths = Math.clamp(cooldownTenths,
                MINIMUM_TRANSIT_COOLDOWN_TENTHS, MAXIMUM_TRANSIT_COOLDOWN_TENTHS);
        }

        static Transit defaults() {
            return new Transit(true, true, true, true, DEFAULT_TRANSIT_COOLDOWN_TENTHS);
        }

        Transit withEnabled(PortalModuleKind kind, boolean enabled) {
            return switch (kind) {
                case PASSIVE_TRANSIT -> new Transit(
                    enabled, hostileEnabled, bossEnabled, projectileEnabled, cooldownTenths);
                case HOSTILE_TRANSIT -> new Transit(
                    passiveEnabled, enabled, bossEnabled, projectileEnabled, cooldownTenths);
                case BOSS_TRANSIT -> new Transit(
                    passiveEnabled, hostileEnabled, enabled, projectileEnabled, cooldownTenths);
                case PROJECTILE_TRANSIT -> new Transit(
                    passiveEnabled, hostileEnabled, bossEnabled, enabled, cooldownTenths);
                default -> this;
            };
        }

        Transit withCooldown(int value) {
            return new Transit(passiveEnabled, hostileEnabled, bossEnabled, projectileEnabled, value);
        }
    }

    public record Duration(int seconds) {
        public Duration {
            seconds = Math.max(PortalOpenDuration.MINIMUM_SECONDS, seconds);
        }

        static Duration defaults() {
            return new Duration(PortalOpenDuration.DEFAULT_SECONDS);
        }
    }

    public record PlayerTarget(boolean enabled, PlayerExcludeMode excludeMode) {
        public PlayerTarget {
            if (excludeMode == null) excludeMode = DEFAULT_PLAYER_EXCLUDE_MODE;
        }

        static PlayerTarget defaults() {
            return new PlayerTarget(true, DEFAULT_PLAYER_EXCLUDE_MODE);
        }

        PlayerTarget withEnabled(boolean value) {
            return new PlayerTarget(value, excludeMode);
        }

        PlayerTarget withExcludeMode(PlayerExcludeMode value) {
            return new PlayerTarget(enabled, value);
        }
    }

}
