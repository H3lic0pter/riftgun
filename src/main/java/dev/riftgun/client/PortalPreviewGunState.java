package dev.riftgun.client;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.service.PortalGunIdentity;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Immutable client projection shared by held-gun and server-focused radial previews. */
public record PortalPreviewGunState(
    @Nullable UUID gunId,
    PortalFunctionMode functionMode,
    PortalPlacementMode placementMode,
    PortalFloatingFallback smartFallback,
    int maximumSurfaceRange,
    int smartDistance,
    int remoteDistance,
    PortalAperture aperture,
    boolean remote,
    boolean remotePlacementPreview,
    @Nullable PortalPairingPendingEndpoint pending
) {
    public static @Nullable PortalPreviewGunState fromStack(
        ItemStack gun, PortalPlayerData data, PortalModuleRules rules, UUID ownerId, long now
    ) {
        if (gun.isEmpty()) return null;
        int smartDistance = data.settings().smartDistance();
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, smartDistance, rules);
        UUID gunId = PortalGunIdentity.existing(gun);
        PortalPairingPendingEndpoint pending = gunId == null ? null
            : PortalPairingPendingEndpoints.getValid(gun, ownerId, gunId, now);
        return new PortalPreviewGunState(gunId, capabilities.functionMode(),
            capabilities.effectivePlacementMode(data.settings().placementMode()),
            capabilities.activeSmartFallback(), capabilities.maximumSurfaceRange(),
            capabilities.smartDistance(), capabilities.remoteDistance(), capabilities.aperture(),
            capabilities.remote(),
            PortalGunModuleSettings.get(gun, smartDistance).remote().placementPreviewEnabled(), pending);
    }

    public static @Nullable PortalPreviewGunState fromSnapshot(
        CompoundTag snapshot, PortalPlayerData data, UUID ownerId, long now
    ) {
        if (snapshot.isEmpty() || !Nbt.hasUUID(snapshot, "InstanceId")) return null;
        UUID gunId = Nbt.getUUID(snapshot, "InstanceId");
        boolean remote = Nbt.getBoolean(snapshot, "RemoteInstalled");
        PortalFunctionMode function = parse(
            PortalFunctionMode.class, Nbt.getString(snapshot, "FunctionMode"),
            PortalFunctionMode.COORDINATE_TRAVEL);
        String fallbackKey = function == PortalFunctionMode.PORTAL_PAIRING
            ? "PairingSmartFallback" : "CoordinateSmartFallback";
        PortalFloatingFallback fallback = remote
            ? parse(PortalFloatingFallback.class, Nbt.getString(snapshot, fallbackKey),
                PortalFloatingFallback.FRONT)
            : PortalFloatingFallback.FRONT;
        PortalPlacementMode preferred = data.settings().placementMode();
        PortalPlacementMode effective = preferred == PortalPlacementMode.REMOTE && !remote
            ? PortalPlacementMode.FRONT : preferred;
        PortalPairingPendingEndpoint pending = snapshot.contains("PendingPairingEndpoint")
            ? PortalPairingPendingEndpoint.load(Nbt.getCompound(snapshot, "PendingPairingEndpoint"))
            : null;
        if (pending != null && !pending.validFor(ownerId, gunId, now)) pending = null;
        int maximum = Math.max(1, Nbt.getInt(snapshot, "MaximumSurfaceRange"));
        return new PortalPreviewGunState(gunId, function, effective, fallback, maximum,
            Math.clamp(Nbt.getInt(snapshot, "SmartDistance"), 1, maximum),
            Math.clamp(Nbt.getInt(snapshot, "RemoteDistance"), 1, maximum),
            Nbt.getBoolean(snapshot, "ExpandedApertureEnabled")
                ? PortalAperture.EXPANDED : PortalAperture.STANDARD,
            remote, Nbt.getBoolean(snapshot, "RemotePlacementPreviewEnabled"), pending);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
