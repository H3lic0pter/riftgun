package dev.riftgun.module;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import dev.riftgun.config.ServerConfig;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOpenDuration;

public record PortalGunCapabilities(
    boolean coordinateOverride,
    int nominalCapacity,
    int maximumSurfaceRange,
    int configuredSurfaceRange,
    int smartDistance,
    PortalEntityAccessSnapshot entityAccess,
    int openDurationTicks,
    PortalAperture aperture,
    boolean playerTarget,
    PlayerExcludeMode playerExcludeMode,
    int transitCooldownTicks,
    boolean fallGuard
) {
    public static PortalGunCapabilities resolve(ItemStack gun, int legacySmartDistance) {
        PortalModuleRules rules = PortalModuleRules.current();
        PortalGunModuleSettings settings = PortalGunModuleSettings.get(gun, legacySmartDistance);
        int reservoirCount = PortalGunModules.activeCount(gun, PortalModuleKind.RESERVOIR_EXPANSION, rules);
        int rangeCount = PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules);
        int maximumRange = rules.maximumSurfaceRangeFor(rangeCount);
        int configuredRange = Mth.clamp(settings.desiredSurfaceRange(), rules.baseSurfaceRange(), maximumRange);
        int durationSeconds = configuredDurationSeconds(gun, settings.portalDurationSeconds(), rules);
        boolean apertureInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.APERTURE_EXPANSION, rules) > 0;
        boolean playerTargetInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.PLAYER_TARGET, rules) > 0;
        return new PortalGunCapabilities(
            PortalGunModules.activeCount(gun, PortalModuleKind.COORDINATE_OVERRIDE, rules) > 0,
            rules.capacityFor(reservoirCount),
            maximumRange,
            configuredRange,
            Mth.clamp(settings.smartDistance(), 1, configuredRange),
            new PortalEntityAccessSnapshot(
                PortalGunModules.activeCount(gun, PortalModuleKind.PASSIVE_TRANSIT, rules) > 0
                    && settings.passiveTransitEnabled(),
                PortalGunModules.activeCount(gun, PortalModuleKind.HOSTILE_TRANSIT, rules) > 0
                    && settings.hostileTransitEnabled(),
                PortalGunModules.activeCount(gun, PortalModuleKind.BOSS_TRANSIT, rules) > 0
                    && settings.bossTransitEnabled()
            ),
            PortalOpenDuration.ticks(durationSeconds),
            apertureInstalled && settings.expandedApertureEnabled()
                ? PortalAperture.EXPANDED : PortalAperture.STANDARD,
            playerTargetInstalled && settings.playerTargetEnabled(),
            settings.playerExcludeMode(),
            settings.transitCooldownTenths() * 2,
            PortalGunModules.activeCount(gun, PortalModuleKind.FALL_GUARD, rules) > 0
                && settings.fallGuardEnabled()
        );
    }

    public static int configuredDurationSeconds(ItemStack gun, int requestedSeconds) {
        return configuredDurationSeconds(gun, requestedSeconds, PortalModuleRules.current());
    }

    public static int maximumDurationSeconds(ItemStack gun, PortalModuleRules rules) {
        if (hasEternalDuration(gun, rules)) return PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS;
        int extensionCount = PortalGunModules.activeCount(gun, PortalModuleKind.DURATION_EXTENSION, rules);
        return rules.maximumPortalDurationSeconds(extensionCount);
    }

    public static boolean hasEternalDuration(ItemStack gun, PortalModuleRules rules) {
        return PortalGunModules.activeCount(gun, PortalModuleKind.DURATION_ETERNAL, rules) > 0;
    }

    private static int configuredDurationSeconds(ItemStack gun, int requestedSeconds,
                                                 PortalModuleRules rules) {
        boolean eternalInstalled = hasEternalDuration(gun, rules);
        return PortalOpenDuration.authorizedSeconds(requestedSeconds,
            maximumDurationSeconds(gun, rules), eternalInstalled);
    }
}
