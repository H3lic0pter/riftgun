package dev.riftgun.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalOpenDuration;
import java.util.function.Consumer;

public record PortalGunModuleSettings(
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
    boolean fallGuardEnabled
) {
    public static final int DEFAULT_SMART_DISTANCE = 8;
    public static final int MINIMUM_TRANSIT_COOLDOWN_TENTHS = 0;
    public static final int MAXIMUM_TRANSIT_COOLDOWN_TENTHS = 50;
    public static final int DEFAULT_TRANSIT_COOLDOWN_TENTHS = 10;
    public static final PlayerExcludeMode DEFAULT_PLAYER_EXCLUDE_MODE = PlayerExcludeMode.ENTRY_AND_EXIT;
    public static final Codec<PortalGunModuleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("smart_distance", DEFAULT_SMART_DISTANCE)
            .forGetter(PortalGunModuleSettings::smartDistance),
        Codec.INT.optionalFieldOf("desired_surface_range", PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE)
            .forGetter(PortalGunModuleSettings::desiredSurfaceRange),
        Codec.BOOL.optionalFieldOf("passive_transit_enabled", true)
            .forGetter(PortalGunModuleSettings::passiveTransitEnabled),
        Codec.BOOL.optionalFieldOf("hostile_transit_enabled", true)
            .forGetter(PortalGunModuleSettings::hostileTransitEnabled),
        Codec.BOOL.optionalFieldOf("boss_transit_enabled", true)
            .forGetter(PortalGunModuleSettings::bossTransitEnabled),
        Codec.INT.optionalFieldOf("portal_duration_seconds", PortalOpenDuration.DEFAULT_SECONDS)
            .forGetter(PortalGunModuleSettings::portalDurationSeconds),
        Codec.BOOL.optionalFieldOf("expanded_aperture_enabled", true)
            .forGetter(PortalGunModuleSettings::expandedApertureEnabled),
        Codec.BOOL.optionalFieldOf("player_target_enabled", true)
            .forGetter(PortalGunModuleSettings::playerTargetEnabled),
        Codec.INT.optionalFieldOf("player_exclude_mode", DEFAULT_PLAYER_EXCLUDE_MODE.id())
            .xmap(PlayerExcludeMode::byId, PlayerExcludeMode::id)
            .forGetter(PortalGunModuleSettings::playerExcludeMode),
        Codec.INT.optionalFieldOf("transit_cooldown_tenths", DEFAULT_TRANSIT_COOLDOWN_TENTHS)
            .forGetter(PortalGunModuleSettings::transitCooldownTenths),
        Codec.BOOL.optionalFieldOf("fall_guard_enabled", true)
            .forGetter(PortalGunModuleSettings::fallGuardEnabled)
    ).apply(instance, PortalGunModuleSettings::new));

    public PortalGunModuleSettings {
        smartDistance = Math.max(1, smartDistance);
        desiredSurfaceRange = Math.max(1, desiredSurfaceRange);
        portalDurationSeconds = Math.max(PortalOpenDuration.MINIMUM_SECONDS, portalDurationSeconds);
        transitCooldownTenths = Math.clamp(transitCooldownTenths,
            MINIMUM_TRANSIT_COOLDOWN_TENTHS, MAXIMUM_TRANSIT_COOLDOWN_TENTHS);
        if (playerExcludeMode == null) playerExcludeMode = DEFAULT_PLAYER_EXCLUDE_MODE;
    }

    public static PortalGunModuleSettings defaults(int legacySmartDistance) {
        return new PortalGunModuleSettings(Math.max(1, legacySmartDistance),
            PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE, true, true, true,
            PortalOpenDuration.DEFAULT_SECONDS, true, true, DEFAULT_PLAYER_EXCLUDE_MODE,
            DEFAULT_TRANSIT_COOLDOWN_TENTHS, true);
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

    public PortalGunModuleSettings withSmartDistance(int value) {
        return copy(settings -> settings.smartDistance = value);
    }

    public PortalGunModuleSettings withDesiredSurfaceRange(int value) {
        return copy(settings -> settings.desiredSurfaceRange = value);
    }

    public PortalGunModuleSettings withTransit(PortalModuleKind kind, boolean enabled) {
        return switch (kind) {
            case PASSIVE_TRANSIT -> copy(settings -> settings.passiveTransitEnabled = enabled);
            case HOSTILE_TRANSIT -> copy(settings -> settings.hostileTransitEnabled = enabled);
            case BOSS_TRANSIT -> copy(settings -> settings.bossTransitEnabled = enabled);
            default -> this;
        };
    }

    public PortalGunModuleSettings withPlayerTargetEnabled(boolean enabled) {
        return copy(settings -> settings.playerTargetEnabled = enabled);
    }

    public PortalGunModuleSettings withPlayerExcludeMode(PlayerExcludeMode mode) {
        return copy(settings -> settings.playerExcludeMode = mode);
    }

    public PortalGunModuleSettings withFallGuardEnabled(boolean enabled) {
        return copy(settings -> settings.fallGuardEnabled = enabled);
    }

    public PortalGunModuleSettings withPortalDurationSeconds(int value) {
        return copy(settings -> settings.portalDurationSeconds = value);
    }

    public PortalGunModuleSettings withTransitCooldownTenths(int value) {
        return copy(settings -> settings.transitCooldownTenths = value);
    }

    public PortalGunModuleSettings withExpandedApertureEnabled(boolean enabled) {
        return copy(settings -> settings.expandedApertureEnabled = enabled);
    }

    private PortalGunModuleSettings copy(Consumer<MutableSettings> mutation) {
        MutableSettings settings = new MutableSettings(this);
        mutation.accept(settings);
        return settings.build();
    }

    private static final class MutableSettings {
        private int smartDistance;
        private int desiredSurfaceRange;
        private boolean passiveTransitEnabled;
        private boolean hostileTransitEnabled;
        private boolean bossTransitEnabled;
        private int portalDurationSeconds;
        private boolean expandedApertureEnabled;
        private boolean playerTargetEnabled;
        private PlayerExcludeMode playerExcludeMode;
        private int transitCooldownTenths;
        private boolean fallGuardEnabled;

        private MutableSettings(PortalGunModuleSettings source) {
            smartDistance = source.smartDistance;
            desiredSurfaceRange = source.desiredSurfaceRange;
            passiveTransitEnabled = source.passiveTransitEnabled;
            hostileTransitEnabled = source.hostileTransitEnabled;
            bossTransitEnabled = source.bossTransitEnabled;
            portalDurationSeconds = source.portalDurationSeconds;
            expandedApertureEnabled = source.expandedApertureEnabled;
            playerTargetEnabled = source.playerTargetEnabled;
            playerExcludeMode = source.playerExcludeMode;
            transitCooldownTenths = source.transitCooldownTenths;
            fallGuardEnabled = source.fallGuardEnabled;
        }

        private PortalGunModuleSettings build() {
            return new PortalGunModuleSettings(smartDistance, desiredSurfaceRange,
                passiveTransitEnabled, hostileTransitEnabled, bossTransitEnabled,
                portalDurationSeconds, expandedApertureEnabled, playerTargetEnabled,
                playerExcludeMode, transitCooldownTenths, fallGuardEnabled);
        }
    }
}
