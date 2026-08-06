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
    PortalAperture aperture
) {
    public static PortalGunCapabilities resolve(ItemStack gun, int legacySmartDistance) {
        PortalModuleRules rules = PortalModuleRules.current();
        PortalGunModuleSettings settings = PortalGunModuleSettings.get(gun, legacySmartDistance);
        int reservoirCount = PortalGunModules.activeCount(gun, PortalModuleKind.RESERVOIR_EXPANSION, rules);
        int rangeCount = PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules);
        int maximumRange = rules.maximumSurfaceRangeFor(rangeCount);
        int configuredRange = Mth.clamp(settings.desiredSurfaceRange(), rules.baseSurfaceRange(), maximumRange);
        int durationSeconds = PortalOpenDuration.effectiveSeconds(settings.portalDurationSeconds(),
            ServerConfig.VALUES.maximumPortalDurationSeconds.get());
        boolean apertureInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.APERTURE_EXPANSION, rules) > 0;
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
                ? PortalAperture.EXPANDED : PortalAperture.STANDARD
        );
    }
}
