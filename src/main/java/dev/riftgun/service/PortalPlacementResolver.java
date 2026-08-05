package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlacementMode;
import net.minecraft.server.level.ServerPlayer;

public interface PortalPlacementResolver {
    PortalPlacementCapture capture(ServerPlayer player, PortalPlacementMode mode, int smartDistance);

    PortalPlacementResult resolvePrepared(ServerPlayer player, Destination destination,
                                          PortalPlacementIntent intent);

    default PortalPlacementResult resolve(ServerPlayer player, Destination destination,
                                          PortalPlacementMode mode, int smartDistance) {
        PortalPlacementCapture capture = capture(player, mode, smartDistance);
        return capture.successful()
            ? resolvePrepared(player, destination, capture.intent())
            : PortalPlacementResult.failure(capture.errorKey());
    }
}
