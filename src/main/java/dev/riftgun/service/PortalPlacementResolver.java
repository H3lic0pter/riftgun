package dev.riftgun.service;

import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface PortalPlacementResolver {
    PortalPlacementCapture capture(ServerPlayer player, PortalPlacementMode mode, int smartDistance,
                                   boolean motionPrediction);

    PortalEntryPlacementResult resolveEntry(ServerPlayer player, PortalPlacementIntent intent);

    PortalPlacementResult resolveExitPrepared(ServerLevel targetLevel, PortalExitTarget target,
                                              PortalPlacement entry);

    default PortalPlacementResult resolveExitPrepared(ServerPlayer player, Destination destination,
                                                      PortalPlacement entry) {
        ServerLevel targetLevel = player.getServer() == null
            ? null : player.getServer().getLevel(destination.dimension());
        return targetLevel == null
            ? PortalPlacementResult.failure("message.riftgun.dimension_unavailable")
            : resolveExitPrepared(targetLevel, PortalExitTarget.from(destination), entry);
    }

    default PortalPlacementResult resolvePrepared(ServerPlayer player, Destination destination,
                                                  PortalPlacementIntent intent) {
        PortalEntryPlacementResult entry = resolveEntry(player, intent);
        return entry.successful()
            ? resolveExitPrepared(player, destination, entry.placement())
            : PortalPlacementResult.failure(entry.errorKey());
    }

    default PortalPlacementResult resolve(ServerPlayer player, Destination destination,
                                          PortalPlacementMode mode, int smartDistance,
                                          boolean motionPrediction) {
        PortalPlacementCapture capture = capture(player, mode, smartDistance, motionPrediction);
        return capture.successful()
            ? resolvePrepared(player, destination, capture.intent())
            : PortalPlacementResult.failure(capture.errorKey());
    }
}
