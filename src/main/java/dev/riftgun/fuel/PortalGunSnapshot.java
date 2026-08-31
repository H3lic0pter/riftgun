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
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.state.PortalGunViewState;
import dev.riftgun.state.PortalGunViewStateCodec;
import java.util.EnumMap;

public final class PortalGunSnapshot {
    public static CompoundTag create(ItemStack gun, int legacySmartDistance) {
        return PortalGunViewStateCodec.encode(createState(gun, legacySmartDistance));
    }

    public static PortalGunViewState createState(ItemStack gun, int legacySmartDistance) {
        var instanceId = PortalGunIdentity.ensure(gun);
        var pending = PortalPairingPendingEndpoints.get(gun);
        PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun, legacySmartDistance);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, legacySmartDistance);
        PortalModuleRules rules = PortalModuleRules.current();
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack fluid = tank.getFluid();
        boolean infiniteFuel = PortalFuelManager.hasInfiniteFuel(gun);
        boolean eternalInstalled = PortalGunCapabilities.hasEternalDuration(gun, rules);
        int maximumDuration = PortalGunCapabilities.maximumDurationSeconds(gun, rules);
        boolean fallGuardInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.FALL_GUARD, rules) > 0;
        boolean playerTargetInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.PLAYER_TARGET, rules) > 0;
        boolean entityRelocationInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.ENTITY_RELOCATION, rules) > 0;
        EnumMap<PortalModuleKind, Integer> modules = new EnumMap<>(PortalModuleKind.class);
        for (PortalModuleKind kind : PortalModuleKind.values()) {
            modules.put(kind, PortalGunModules.activeCount(gun, kind, rules));
        }
        PortalFuelProfile profile = PortalFuelProfiles.resolve(fluid.getFluid()).orElse(null);
        String fluidId = profile == null ? ""
            : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
        if (fluid.isEmpty() && infiniteFuel) {
            profile = PortalFuelProfiles.dimensional();
            fluidId = profile.id().toString();
        }
        var fuel = new PortalGunViewState.Fuel(
            PortalGunMode.bucketMode(gun), fluid.getAmount(), tank.nominalCapacity(),
            fluid.getAmount() > tank.nominalCapacity(), infiniteFuel,
            PortalFluidInstability.isUnstable(fluid.getFluid()), fluidId,
            profile == null ? 0 : profile.rgb(), profile != null && profile.crossDimension());
        var navigation = new PortalGunViewState.Navigation(
            capabilities.coordinateOverride(), capabilities.dimensionalTraversal(),
            dev.riftgun.core.config.RiftConfigs.server().dimensionalTraversal().enabled(),
            settings.dimensionalTraversal().targetDimension(), settings.dimensionalTraversal().mode());
        var placement = new PortalGunViewState.Placement(
            capabilities.maximumSurfaceRange(), capabilities.remoteDistance(), capabilities.smartDistance(),
            capabilities.remote(), settings.remote().scrollAdjustmentEnabled(),
            settings.remote().radialSliderEnabled(), settings.remote().placementPreviewEnabled(),
            capabilities.precisionPlacement(), capabilities.portalPairing(), capabilities.functionMode(),
            settings.remote().coordinateSmartFallback(), settings.portalPairing().smartFallback());
        var transit = new PortalGunViewState.Transit(
            capabilities.entityAccess().mask(), settings.passiveTransitEnabled(),
            settings.hostileTransitEnabled(), settings.bossTransitEnabled(),
            settings.projectileTransitEnabled(), PortalGunCapabilities.configuredDurationSeconds(
                gun, settings.portalDurationSeconds()), maximumDuration, eternalInstalled,
            settings.expandedApertureEnabled(), settings.transitCooldownTenths(),
            PortalGunModuleSettings.MAXIMUM_TRANSIT_COOLDOWN_TENTHS,
            playerTargetInstalled, capabilities.playerTarget(), capabilities.playerExcludeMode(),
            fallGuardInstalled, capabilities.fallGuard(), capabilities.entityFallGuard(),
            entityRelocationInstalled, capabilities.entityRelocation(),
            settings.entityRelocation().smartRouting());
        return new PortalGunViewState(instanceId, pending, fuel, navigation, placement, transit,
            new PortalGunViewState.Modules(modules, rules));
    }

    private PortalGunSnapshot() {}
}
