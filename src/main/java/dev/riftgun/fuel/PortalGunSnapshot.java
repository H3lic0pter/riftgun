package dev.riftgun.fuel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.config.ServerConfig;
import dev.riftgun.portal.PortalOpenDuration;

public final class PortalGunSnapshot {
    public static CompoundTag create(ItemStack gun, int legacySmartDistance) {
        CompoundTag tag = new CompoundTag();
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, legacySmartDistance);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, legacySmartDistance);
        PortalModuleRules rules = PortalModuleRules.current();
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack fluid = tank.getFluid();
        tag.putBoolean("BucketMode", PortalGunMode.bucketMode(gun));
        tag.putInt("Amount", fluid.getAmount());
        tag.putInt("Capacity", tank.nominalCapacity());
        tag.putBoolean("Overfilled", fluid.getAmount() > tank.nominalCapacity());
        tag.putBoolean("CoordinateOverride", capabilities.coordinateOverride());
        tag.putInt("MaximumSurfaceRange", capabilities.maximumSurfaceRange());
        tag.putInt("SurfaceRange", capabilities.configuredSurfaceRange());
        tag.putInt("SmartDistance", capabilities.smartDistance());
        tag.putInt("EntityAccess", capabilities.entityAccess().mask());
        tag.putBoolean("PassiveTransitEnabled", settings.passiveTransitEnabled());
        tag.putBoolean("HostileTransitEnabled", settings.hostileTransitEnabled());
        tag.putBoolean("BossTransitEnabled", settings.bossTransitEnabled());
        boolean eternalInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.DURATION_ETERNAL, rules) > 0;
        int extensionCount = PortalGunModules.activeCount(gun, PortalModuleKind.DURATION_EXTENSION, rules);
        int maximumDuration = eternalInstalled ? PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS
            : rules.maximumPortalDurationSeconds(extensionCount);
        tag.putInt("PortalDurationSeconds", PortalOpenDuration.effectiveSeconds(
            settings.portalDurationSeconds(), maximumDuration));
        tag.putInt("MaximumPortalDurationSeconds", maximumDuration);
        tag.putBoolean("EternalDurationInstalled", eternalInstalled);
        tag.putBoolean("ExpandedApertureEnabled", settings.expandedApertureEnabled());
        tag.putInt("TransitCooldownTenths", settings.transitCooldownTenths());
        tag.putInt("MaximumTransitCooldownTenths", PortalGunModuleSettings.MAXIMUM_TRANSIT_COOLDOWN_TENTHS);
        tag.putBoolean("PlayerTargetEnabled", capabilities.playerTarget());
        tag.putInt("PlayerExcludeMode", capabilities.playerExcludeMode());
        tag.putBoolean("PlayerTargetInstalled", PortalGunModules.activeCount(
            gun, PortalModuleKind.PLAYER_TARGET, rules) > 0);
        CompoundTag modules = new CompoundTag();
        for (PortalModuleKind kind : PortalModuleKind.values()) {
            modules.putInt(kind.name(), PortalGunModules.activeCount(gun, kind, rules));
        }
        tag.put("Modules", modules);
        tag.put("ModuleRules", rules.save());
        PortalFuelProfiles.resolve(fluid).ifPresent(profile -> {
            tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            tag.putInt("Rgb", profile.rgb());
            tag.putBoolean("CrossDimension", profile.crossDimension());
        });
        return tag;
    }

    private PortalGunSnapshot() {}
}
