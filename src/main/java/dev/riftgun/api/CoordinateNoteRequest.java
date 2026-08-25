package dev.riftgun.api;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Request to materialize an external destination as Rift Gun's coordinate note item. */
public record CoordinateNoteRequest(
    ServerPlayer player,
    RiftResourceId sourceId,
    Component displayName,
    PortalDestination destination
) {
    public CoordinateNoteRequest {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(destination, "destination");
        if (displayName.getString().isBlank()) {
            throw new IllegalArgumentException("Coordinate note display name cannot be blank");
        }
    }
}
