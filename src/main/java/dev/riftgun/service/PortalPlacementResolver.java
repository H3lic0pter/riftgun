package dev.riftgun.service;

import dev.riftgun.core.runtime.RiftRuntime;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalExitTarget;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface PortalPlacementResolver {
    PortalPlacementCapture capture(ServerPlayer player, PortalPlacementMode mode,
                                   PortalPlacementConstraints constraints);

    PortalPlacementCapture captureSurfaceFace(ServerPlayer player, SurfaceFaceSelection selection,
                                              PortalPlacementConstraints constraints);

    PortalEntryPlacementResult resolveEntry(ServerPlayer player, PortalPlacementIntent intent,
                                            PortalPlacementConstraints constraints);

    PortalPlacementResult resolveExitPrepared(ServerLevel targetLevel, PortalExitTarget target,
                                              PortalPlacement entry, PortalAperture aperture);

    default PortalPlacementResult resolveExitPrepared(ServerPlayer player, Destination destination,
                                                      PortalPlacement entry, PortalAperture aperture) {
        //? if >=1.21.11 {
        /*ServerLevel targetLevel = player.level().getServer() == null ? null : player.level().getServer().getLevel(destination.dimension());
        *///?} else {
        ServerLevel targetLevel = player.getServer() == null ? null : player.getServer().getLevel(destination.dimension());
        //?}
        return targetLevel == null
            ? PortalPlacementResult.failure("message.riftgun.dimension_unavailable")
            : resolveExitPrepared(targetLevel, PortalExitTarget.from(destination), entry, aperture);
    }

    default PortalPlacementResult resolvePrepared(ServerPlayer player, Destination destination,
                                                  PortalPlacementIntent intent) {
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            (int) PortalPlacementCapabilities.DEFAULT_MAXIMUM_SURFACE_RANGE,
            PortalPlacementCapabilities.DEFAULT_MAXIMUM_SURFACE_RANGE,
            PortalPredictionMode.OFF, PortalAperture.STANDARD);
        return resolvePrepared(player, destination, intent, constraints);
    }

    default PortalPlacementResult resolvePrepared(ServerPlayer player, Destination destination,
                                                  PortalPlacementIntent intent,
                                                  PortalPlacementConstraints constraints) {
        PortalEntryPlacementResult entry = resolveEntry(player, intent, constraints);
        return entry.successful()
            ? resolveExitPrepared(player, destination, entry.placement(), constraints.aperture())
            : PortalPlacementResult.failure(entry.errorKey());
    }

    default PortalPlacementResult resolve(ServerPlayer player, Destination destination,
                                          PortalPlacementMode mode, int smartDistance,
                                          PortalPredictionMode predictionMode) {
        PortalPlacementConstraints constraints = new PortalPlacementConstraints(
            smartDistance, RiftRuntime.current().placementCapabilities().maximumSurfaceRange(player), predictionMode,
            PortalAperture.STANDARD);
        PortalPlacementCapture capture = capture(player, mode, constraints);
        return capture.successful()
            ? resolvePrepared(player, destination, capture.intent(), constraints)
            : PortalPlacementResult.failure(capture.errorKey());
    }
}
