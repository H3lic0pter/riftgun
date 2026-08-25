package dev.riftgun.api;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Supplies external destinations for a viewer. Providers describe targets only;
 * they never create portals or perform teleportation.
 */
public interface RiftGunDestinationProvider {
    RiftResourceId id();

    List<ProvidedPortalDestination> destinations(ServerPlayer viewer);
}
