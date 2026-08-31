package dev.riftgun.client;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/** Shared wire command for version-specific Portal Gun quick-action key bindings. */
public record PortalShortcutCommand(PortalAction action,
                                    @Nullable PortalPlacementMode placementMode,
                                    @Nullable Boolean endpointA) {
    public static PortalShortcutCommand forcedOpen(PortalPlacementMode mode) {
        return new PortalShortcutCommand(PortalAction.OPEN_SELECTED, mode, null);
    }

    public static PortalShortcutCommand pairingEndpoint(boolean endpointA) {
        return new PortalShortcutCommand(PortalAction.PLACE_PAIRING_ENDPOINT, null, endpointA);
    }

    public void send() {
        PortalNetworking.sendShortcutRequest(action, this::writeTo);
    }

    public void writeTo(CompoundTag tag) {
        if (placementMode != null) tag.putString("PlacementMode", placementMode.name());
        if (endpointA != null) tag.putBoolean("EndpointA", endpointA);
    }
}
