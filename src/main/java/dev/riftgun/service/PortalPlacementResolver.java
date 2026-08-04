package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlacementMode;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PortalPlacementResolver {
    PortalPlacementResult resolve(ServerPlayer player, Destination destination,
                                  PortalPlacementMode mode, int smartDistance);
}
