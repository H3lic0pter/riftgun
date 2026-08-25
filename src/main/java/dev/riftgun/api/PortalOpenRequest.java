package dev.riftgun.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Complete input for opening a Rift Gun-owned portal pair. */
public record PortalOpenRequest(
    ServerPlayer opener,
    PortalDestination destination,
    RiftResourceId sourceId,
    Component displayName,
    PortalOpenIntent intent,
    Optional<PortalTransitAuthorization> transitAuthorization
) {
    public PortalOpenRequest {
        Objects.requireNonNull(opener, "opener");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(transitAuthorization, "transitAuthorization");
        transitAuthorization.ifPresent(authorization -> {
            if (!authorization.destinationDimension().equals(destination.dimensionId())) {
                throw new IllegalArgumentException(
                    "Transit authorization destination must match portal destination");
            }
        });
    }

    public PortalOpenRequest(
        ServerPlayer opener,
        PortalDestination destination,
        RiftResourceId sourceId,
        PortalOpenIntent intent,
        Optional<PortalTransitAuthorization> transitAuthorization
    ) {
        this(opener, destination, sourceId, Component.literal(sourceId.toString()), intent,
            transitAuthorization);
    }

    public PortalOpenRequest(
        ServerPlayer opener,
        PortalDestination destination,
        RiftResourceId sourceId,
        PortalOpenIntent intent
    ) {
        this(opener, destination, sourceId, Component.literal(sourceId.toString()), intent, Optional.empty());
    }
}
