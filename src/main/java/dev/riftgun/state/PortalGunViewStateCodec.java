package dev.riftgun.state;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.module.PlayerExcludeMode;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import java.util.EnumMap;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** The only wire-schema owner for {@link PortalGunViewState}. */
public final class PortalGunViewStateCodec {
    public static CompoundTag encode(PortalGunViewState state) {
        CompoundTag tag = new CompoundTag();
        if (state.instanceId() != null) Nbt.putUUID(tag, "InstanceId", state.instanceId());
        if (state.pendingPairingEndpoint() != null) {
            tag.put("PendingPairingEndpoint", state.pendingPairingEndpoint().save());
        }
        var fuel = state.fuel();
        tag.putBoolean("BucketMode", fuel.bucketMode());
        tag.putInt("Amount", fuel.amount());
        tag.putInt("Capacity", fuel.capacity());
        tag.putBoolean("Overfilled", fuel.overfilled());
        tag.putBoolean("InfiniteFuel", fuel.infinite());
        tag.putBoolean("Unstable", fuel.unstable());
        if (!fuel.fluidId().isBlank()) {
            tag.putString("Fluid", fuel.fluidId());
            tag.putInt("Rgb", fuel.rgb());
            tag.putBoolean("CrossDimension", fuel.crossDimension());
        }

        var navigation = state.navigation();
        tag.putBoolean("CoordinateOverride", navigation.coordinateOverride());
        tag.putBoolean("DimensionalTraversalInstalled", navigation.dimensionalTraversalInstalled());
        tag.putBoolean("DimensionalTraversalEnabled", navigation.dimensionalTraversalEnabled());
        tag.putString("DimensionalTraversalDimension", navigation.targetDimension());
        tag.putString("DimensionalTraversalMode", navigation.mode().name());

        var placement = state.placement();
        tag.putInt("MaximumSurfaceRange", placement.maximumSurfaceRange());
        tag.putInt("RemoteDistance", placement.remoteDistance());
        tag.putInt("SmartDistance", placement.smartDistance());
        tag.putBoolean("RemoteInstalled", placement.remoteInstalled());
        tag.putBoolean("RemoteScrollAdjustmentEnabled", placement.remoteScrollAdjustmentEnabled());
        tag.putBoolean("RemoteRadialSliderEnabled", placement.remoteRadialSliderEnabled());
        tag.putBoolean("RemotePlacementPreviewEnabled", placement.remotePreviewEnabled());
        tag.putBoolean("PrecisionPlacementInstalled", placement.precisionInstalled());
        tag.putBoolean("PortalPairingInstalled", placement.pairingInstalled());
        tag.putString("FunctionMode", placement.functionMode().name());
        tag.putString("CoordinateSmartFallback", placement.coordinateSmartFallback().name());
        tag.putString("PairingSmartFallback", placement.pairingSmartFallback().name());

        var transit = state.transit();
        tag.putInt("EntityAccess", transit.entityAccessMask());
        tag.putBoolean("PassiveTransitEnabled", transit.passiveEnabled());
        tag.putBoolean("HostileTransitEnabled", transit.hostileEnabled());
        tag.putBoolean("BossTransitEnabled", transit.bossEnabled());
        tag.putBoolean("ProjectileTransitEnabled", transit.projectileEnabled());
        tag.putInt("PortalDurationSeconds", transit.portalDurationSeconds());
        tag.putInt("MaximumPortalDurationSeconds", transit.maximumPortalDurationSeconds());
        tag.putBoolean("EternalDurationInstalled", transit.eternalDurationInstalled());
        tag.putBoolean("ExpandedApertureEnabled", transit.expandedApertureEnabled());
        tag.putInt("TransitCooldownTenths", transit.transitCooldownTenths());
        tag.putInt("MaximumTransitCooldownTenths", transit.maximumTransitCooldownTenths());
        tag.putBoolean("PlayerTargetInstalled", transit.playerTargetInstalled());
        tag.putBoolean("PlayerTargetEnabled", transit.playerTargetEnabled());
        tag.putInt("PlayerExcludeMode", transit.playerExcludeMode().id());
        tag.putBoolean("FallGuardInstalled", transit.fallGuardInstalled());
        tag.putBoolean("FallGuardEnabled", transit.fallGuardEnabled());
        tag.putBoolean("FallGuardEntitiesEnabled", transit.entityFallGuardEnabled());
        tag.putBoolean("EntityRelocationInstalled", transit.entityRelocationInstalled());
        tag.putBoolean("EntityRelocationEnabled", transit.entityRelocationEnabled());
        tag.putBoolean("EntityRelocationSmartRouting", transit.entityRelocationSmartRouting());

        CompoundTag modules = new CompoundTag();
        state.modules().counts().forEach((kind, count) -> modules.putInt(kind.name(), count));
        tag.put("Modules", modules);
        tag.put("ModuleRules", state.modules().rules().save());
        return tag;
    }

    public static PortalGunViewState decode(CompoundTag tag) {
        UUID id = Nbt.hasUUID(tag, "InstanceId") ? Nbt.getUUID(tag, "InstanceId") : null;
        PortalPairingPendingEndpoint pending = Nbt.contains(tag, "PendingPairingEndpoint")
            ? PortalPairingPendingEndpoint.load(Nbt.getCompound(tag, "PendingPairingEndpoint")) : null;
        var fuel = new PortalGunViewState.Fuel(
            Nbt.getBoolean(tag, "BucketMode"), Nbt.getInt(tag, "Amount"),
            Math.max(1, Nbt.getInt(tag, "Capacity")), Nbt.getBoolean(tag, "Overfilled"),
            Nbt.getBoolean(tag, "InfiniteFuel"), Nbt.getBoolean(tag, "Unstable"),
            Nbt.getString(tag, "Fluid"), Nbt.getInt(tag, "Rgb"),
            Nbt.getBoolean(tag, "CrossDimension"));
        var navigation = new PortalGunViewState.Navigation(
            Nbt.getBoolean(tag, "CoordinateOverride"),
            Nbt.getBoolean(tag, "DimensionalTraversalInstalled"),
            Nbt.getBoolean(tag, "DimensionalTraversalEnabled"),
            Nbt.getString(tag, "DimensionalTraversalDimension"),
            DimensionalTraversalMode.parse(Nbt.getString(tag, "DimensionalTraversalMode")));
        var placement = new PortalGunViewState.Placement(
            Math.max(1, Nbt.getInt(tag, "MaximumSurfaceRange")),
            Nbt.getInt(tag, "RemoteDistance"), Nbt.getInt(tag, "SmartDistance"),
            Nbt.getBoolean(tag, "RemoteInstalled"),
            Nbt.getBoolean(tag, "RemoteScrollAdjustmentEnabled"),
            Nbt.getBoolean(tag, "RemoteRadialSliderEnabled"),
            Nbt.getBoolean(tag, "RemotePlacementPreviewEnabled"),
            Nbt.getBoolean(tag, "PrecisionPlacementInstalled"),
            Nbt.getBoolean(tag, "PortalPairingInstalled"),
            enumValue(PortalFunctionMode.class, Nbt.getString(tag, "FunctionMode"),
                PortalFunctionMode.COORDINATE_TRAVEL),
            enumValue(PortalFloatingFallback.class, Nbt.getString(tag, "CoordinateSmartFallback"),
                PortalFloatingFallback.FRONT),
            enumValue(PortalFloatingFallback.class, Nbt.getString(tag, "PairingSmartFallback"),
                PortalFloatingFallback.FRONT));
        var transit = new PortalGunViewState.Transit(
            Nbt.getInt(tag, "EntityAccess"), Nbt.getBoolean(tag, "PassiveTransitEnabled"),
            Nbt.getBoolean(tag, "HostileTransitEnabled"), Nbt.getBoolean(tag, "BossTransitEnabled"),
            Nbt.getBoolean(tag, "ProjectileTransitEnabled"),
            Nbt.getInt(tag, "PortalDurationSeconds"),
            Nbt.getInt(tag, "MaximumPortalDurationSeconds"),
            Nbt.getBoolean(tag, "EternalDurationInstalled"),
            Nbt.getBoolean(tag, "ExpandedApertureEnabled"),
            Nbt.getInt(tag, "TransitCooldownTenths"),
            Nbt.getInt(tag, "MaximumTransitCooldownTenths"),
            Nbt.getBoolean(tag, "PlayerTargetInstalled"),
            Nbt.getBoolean(tag, "PlayerTargetEnabled"),
            PlayerExcludeMode.byId(Nbt.getInt(tag, "PlayerExcludeMode")),
            Nbt.getBoolean(tag, "FallGuardInstalled"), Nbt.getBoolean(tag, "FallGuardEnabled"),
            Nbt.getBoolean(tag, "FallGuardEntitiesEnabled"),
            Nbt.getBoolean(tag, "EntityRelocationInstalled"),
            Nbt.getBoolean(tag, "EntityRelocationEnabled"),
            Nbt.getBoolean(tag, "EntityRelocationSmartRouting"));
        CompoundTag moduleTag = Nbt.getCompound(tag, "Modules");
        EnumMap<PortalModuleKind, Integer> counts = new EnumMap<>(PortalModuleKind.class);
        for (PortalModuleKind kind : PortalModuleKind.values()) {
            int count = Nbt.getInt(moduleTag, kind.name());
            if (count > 0) counts.put(kind, count);
        }
        PortalModuleRules rules = Nbt.contains(tag, "ModuleRules")
            ? PortalModuleRules.load(Nbt.getCompound(tag, "ModuleRules"))
            : PortalModuleRules.defaults();
        return new PortalGunViewState(id, pending, fuel, navigation, placement, transit,
            new PortalGunViewState.Modules(counts, rules));
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private PortalGunViewStateCodec() {}
}
