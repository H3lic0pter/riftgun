package dev.riftgun.module;

import com.mojang.serialization.Codec;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.relocation.EntityRelocationSettings;
import net.minecraft.world.item.ItemStack;

/** Persisted per-gun preferences grouped by the capability that owns each value. */
public record PortalGunModuleSettings(
    Placement placement,
    Transit transit,
    Duration duration,
    boolean expandedApertureEnabled,
    PlayerTarget playerTarget,
    EntityRelocationSettings entityRelocation,
    boolean fallGuardEnabled
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
    }

    public static PortalGunModuleSettings defaults(int legacySmartDistance) {
        return new PortalGunModuleSettings(
            Placement.defaults(legacySmartDistance), Transit.defaults(), Duration.defaults(),
            true, PlayerTarget.defaults(), EntityRelocationSettings.defaults(), true);
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

    public int desiredSurfaceRange() {
        return placement.desiredSurfaceRange();
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
            expandedApertureEnabled, playerTarget, entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withDesiredSurfaceRange(int value) {
        return new PortalGunModuleSettings(placement.withDesiredSurfaceRange(value), transit, duration,
            expandedApertureEnabled, playerTarget, entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withTransit(PortalModuleKind kind, boolean enabled) {
        Transit updated = transit.withEnabled(kind, enabled);
        return updated == transit ? this : new PortalGunModuleSettings(
            placement, updated, duration, expandedApertureEnabled, playerTarget, entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withTransitCooldownTenths(int value) {
        return new PortalGunModuleSettings(placement, transit.withCooldown(value), duration,
            expandedApertureEnabled, playerTarget, entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withPortalDurationSeconds(int value) {
        return new PortalGunModuleSettings(placement, transit, new Duration(value),
            expandedApertureEnabled, playerTarget, entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withExpandedApertureEnabled(boolean enabled) {
        return new PortalGunModuleSettings(
            placement, transit, duration, enabled, playerTarget, entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withPlayerTargetEnabled(boolean enabled) {
        return new PortalGunModuleSettings(placement, transit, duration,
            expandedApertureEnabled, playerTarget.withEnabled(enabled), entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withPlayerExcludeMode(PlayerExcludeMode mode) {
        return new PortalGunModuleSettings(placement, transit, duration,
            expandedApertureEnabled, playerTarget.withExcludeMode(mode), entityRelocation, fallGuardEnabled);
    }

    public PortalGunModuleSettings withEntityRelocationEnabled(boolean enabled) {
        return new PortalGunModuleSettings(placement, transit, duration, expandedApertureEnabled,
            playerTarget, entityRelocation.withEnabled(enabled), fallGuardEnabled);
    }

    public PortalGunModuleSettings withEntityRelocationSmartRouting(boolean enabled) {
        return new PortalGunModuleSettings(placement, transit, duration, expandedApertureEnabled,
            playerTarget, entityRelocation.withSmartRouting(enabled), fallGuardEnabled);
    }
    public PortalGunModuleSettings withFallGuardEnabled(boolean enabled) {
        return new PortalGunModuleSettings(
            placement, transit, duration, expandedApertureEnabled, playerTarget, entityRelocation, enabled);
    }

    public record Placement(int smartDistance, int desiredSurfaceRange) {
        public Placement {
            smartDistance = Math.max(1, smartDistance);
            desiredSurfaceRange = Math.max(1, desiredSurfaceRange);
        }

        static Placement defaults(int legacySmartDistance) {
            return new Placement(Math.max(1, legacySmartDistance),
                PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE);
        }

        Placement withSmartDistance(int value) {
            return new Placement(value, desiredSurfaceRange);
        }

        Placement withDesiredSurfaceRange(int value) {
            return new Placement(smartDistance, value);
        }
    }

    public record Transit(boolean passiveEnabled, boolean hostileEnabled,
                          boolean bossEnabled, int cooldownTenths) {
        public Transit {
            cooldownTenths = Math.clamp(cooldownTenths,
                MINIMUM_TRANSIT_COOLDOWN_TENTHS, MAXIMUM_TRANSIT_COOLDOWN_TENTHS);
        }

        static Transit defaults() {
            return new Transit(true, true, true, DEFAULT_TRANSIT_COOLDOWN_TENTHS);
        }

        Transit withEnabled(PortalModuleKind kind, boolean enabled) {
            return switch (kind) {
                case PASSIVE_TRANSIT -> new Transit(enabled, hostileEnabled, bossEnabled, cooldownTenths);
                case HOSTILE_TRANSIT -> new Transit(passiveEnabled, enabled, bossEnabled, cooldownTenths);
                case BOSS_TRANSIT -> new Transit(passiveEnabled, hostileEnabled, enabled, cooldownTenths);
                default -> this;
            };
        }

        Transit withCooldown(int value) {
            return new Transit(passiveEnabled, hostileEnabled, bossEnabled, value);
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
