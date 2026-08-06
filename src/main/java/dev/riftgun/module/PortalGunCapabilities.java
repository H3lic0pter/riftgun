package dev.riftgun.module;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public record PortalGunCapabilities(
    boolean coordinateOverride,
    int nominalCapacity,
    int maximumSurfaceRange,
    int configuredSurfaceRange,
    int smartDistance,
    PortalEntityAccessSnapshot entityAccess
) {
    public static PortalGunCapabilities resolve(ItemStack gun, int legacySmartDistance) {
        PortalModuleRules rules = PortalModuleRules.current();
        PortalGunModuleSettings settings = PortalGunModuleSettings.get(gun, legacySmartDistance);
        int reservoirCount = PortalGunModules.activeCount(gun, PortalModuleKind.RESERVOIR_EXPANSION, rules);
        int rangeCount = PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules);
        int maximumRange = rules.maximumSurfaceRangeFor(rangeCount);
        int configuredRange = Mth.clamp(settings.desiredSurfaceRange(), rules.baseSurfaceRange(), maximumRange);
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
            )
        );
    }
}
