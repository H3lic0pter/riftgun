package dev.riftgun.api;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** A provider-local choice displayed by Rift Gun and resolved to a portal target. */
public record ProvidedPortalDestination(
    RiftResourceId id,
    Component displayName,
    PortalDestination destination
) {
    public ProvidedPortalDestination {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(destination, "destination");
    }
}
