package dev.riftgun.client;

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
import dev.riftgun.state.PortalGunViewState;
import java.util.UUID;
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
        PortalGunModuleSettings settings = PortalGunModuleSettings.get(gun, smartDistance);
        PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(gun, settings, rules);
        UUID gunId = PortalGunIdentity.existing(gun);
        PortalPairingPendingEndpoint pending = gunId == null ? null
            : PortalPairingPendingEndpoints.getValid(gun, ownerId, gunId, now);
        return new PortalPreviewGunState(gunId, capabilities.functionMode(),
            capabilities.effectivePlacementMode(data.settings().placementMode()),
            capabilities.activeSmartFallback(), capabilities.maximumSurfaceRange(),
            capabilities.smartDistance(), capabilities.remoteDistance(), capabilities.aperture(),
            capabilities.remote(),
            settings.remote().placementPreviewEnabled(), pending);
    }

    public static @Nullable PortalPreviewGunState fromSnapshot(
        PortalGunViewState snapshot, PortalPlayerData data, UUID ownerId, long now
    ) {
        if (snapshot.instanceId() == null) return null;
        UUID gunId = snapshot.instanceId();
        boolean remote = snapshot.remoteInstalled();
        PortalFunctionMode function = snapshot.functionMode();
        PortalFloatingFallback fallback = remote
            ? function == PortalFunctionMode.PORTAL_PAIRING
                ? snapshot.placement().pairingSmartFallback()
                : snapshot.placement().coordinateSmartFallback()
            : PortalFloatingFallback.FRONT;
        PortalPlacementMode preferred = data.settings().placementMode();
        PortalPlacementMode effective = preferred == PortalPlacementMode.REMOTE && !remote
            ? PortalPlacementMode.FRONT : preferred;
        PortalPairingPendingEndpoint pending = snapshot.pendingPairingEndpoint();
        if (pending != null && !pending.validFor(ownerId, gunId, now)) pending = null;
        int maximum = snapshot.maximumSurfaceRange();
        return new PortalPreviewGunState(gunId, function, effective, fallback, maximum,
            snapshot.smartDistance(), snapshot.remoteDistance(),
            snapshot.expandedApertureEnabled()
                ? PortalAperture.EXPANDED : PortalAperture.STANDARD,
            remote, snapshot.remotePreviewEnabled(), pending);
    }
}
