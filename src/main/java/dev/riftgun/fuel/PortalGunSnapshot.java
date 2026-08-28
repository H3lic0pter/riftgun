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
import dev.riftgun.crisis.PortalFluidInstability;

public final class PortalGunSnapshot {
    public static CompoundTag create(ItemStack gun, int legacySmartDistance) {
        CompoundTag tag = new CompoundTag();
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, legacySmartDistance);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, legacySmartDistance);
        PortalModuleRules rules = PortalModuleRules.current();
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack fluid = tank.getFluid();
        boolean infiniteFuel = PortalFuelManager.hasInfiniteFuel(gun);
        tag.putBoolean("BucketMode", PortalGunMode.bucketMode(gun));
        tag.putInt("Amount", fluid.getAmount());
        tag.putInt("Capacity", tank.nominalCapacity());
        tag.putBoolean("Overfilled", fluid.getAmount() > tank.nominalCapacity());
        tag.putBoolean("InfiniteFuel", infiniteFuel);
        tag.putBoolean("Unstable", PortalFluidInstability.isUnstable(fluid.getFluid()));
        tag.putBoolean("CoordinateOverride", capabilities.coordinateOverride());
        tag.putInt("MaximumSurfaceRange", capabilities.maximumSurfaceRange());
        tag.putInt("SurfaceRange", capabilities.configuredSurfaceRange());
        tag.putInt("SmartDistance", capabilities.smartDistance());
        tag.putInt("EntityAccess", capabilities.entityAccess().mask());
        tag.putBoolean("PassiveTransitEnabled", settings.passiveTransitEnabled());
        tag.putBoolean("HostileTransitEnabled", settings.hostileTransitEnabled());
        tag.putBoolean("BossTransitEnabled", settings.bossTransitEnabled());
        tag.putBoolean("ProjectileTransitEnabled", settings.projectileTransitEnabled());
        boolean eternalInstalled = PortalGunCapabilities.hasEternalDuration(gun, rules);
        int maximumDuration = PortalGunCapabilities.maximumDurationSeconds(gun, rules);
        tag.putInt("PortalDurationSeconds", PortalGunCapabilities.configuredDurationSeconds(
            gun, settings.portalDurationSeconds()));
        tag.putInt("MaximumPortalDurationSeconds", maximumDuration);
        tag.putBoolean("EternalDurationInstalled", eternalInstalled);
        tag.putBoolean("ExpandedApertureEnabled", settings.expandedApertureEnabled());
        tag.putInt("TransitCooldownTenths", settings.transitCooldownTenths());
        tag.putInt("MaximumTransitCooldownTenths", PortalGunModuleSettings.MAXIMUM_TRANSIT_COOLDOWN_TENTHS);
        tag.putBoolean("PlayerTargetEnabled", capabilities.playerTarget());
        tag.putInt("PlayerExcludeMode", capabilities.playerExcludeMode().id());
        boolean fallGuardInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.FALL_GUARD, rules) > 0;
        tag.putBoolean("FallGuardInstalled", fallGuardInstalled);
        tag.putBoolean("FallGuardEnabled", capabilities.fallGuard());
        tag.putBoolean("FallGuardEntitiesEnabled", capabilities.entityFallGuard());
        tag.putBoolean("PlayerTargetInstalled", PortalGunModules.activeCount(
            gun, PortalModuleKind.PLAYER_TARGET, rules) > 0);
        tag.putBoolean("EntityRelocationInstalled", PortalGunModules.activeCount(
            gun, PortalModuleKind.ENTITY_RELOCATION, rules) > 0);
        tag.putBoolean("EntityRelocationEnabled", capabilities.entityRelocation());
        tag.putBoolean("EntityRelocationSmartRouting", settings.entityRelocation().smartRouting());
        tag.putBoolean("RemoteInstalled", capabilities.remote());
        tag.putBoolean("RemoteScrollAdjustmentEnabled",
            settings.portalPairing().remote().scrollAdjustmentEnabled());
        tag.putBoolean("RemoteRadialSliderEnabled",
            settings.portalPairing().remote().radialSliderEnabled());
        tag.putBoolean("PortalPairingInstalled", capabilities.portalPairing());
        tag.putString("FunctionMode", capabilities.functionMode().name());
        tag.putString("CoordinateSmartFallback", settings.portalPairing().coordinateSmartFallback().name());
        tag.putString("PairingSmartFallback", settings.portalPairing().pairingSmartFallback().name());
        CompoundTag modules = new CompoundTag();
        for (PortalModuleKind kind : PortalModuleKind.values()) {
            modules.putInt(kind.name(), PortalGunModules.activeCount(gun, kind, rules));
        }
        tag.put("Modules", modules);
        tag.put("ModuleRules", rules.save());
        PortalFuelProfiles.resolve(fluid.getFluid()).ifPresent(profile -> {
            tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            tag.putInt("Rgb", profile.rgb());
            tag.putBoolean("CrossDimension", profile.crossDimension());
        });
        if (fluid.isEmpty() && infiniteFuel) {
            PortalFuelProfile profile = PortalFuelProfiles.dimensional();
            tag.putString("Fluid", profile.id().toString());
            tag.putInt("Rgb", profile.rgb());
            tag.putBoolean("CrossDimension", true);
        }
        return tag;
    }

    private PortalGunSnapshot() {}
}
