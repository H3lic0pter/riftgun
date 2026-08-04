package dev.riftgun.service;

import net.minecraft.server.level.ServerPlayer;

public interface PortalPlacementCapabilities {
    double DEFAULT_FRONT_DISTANCE = 2.0;
    double DEFAULT_MAXIMUM_SURFACE_RANGE = 32.0;
    PortalPlacementCapabilities DEFAULT = new PortalPlacementCapabilities() {};

    default double frontDistance(ServerPlayer player) {
        return DEFAULT_FRONT_DISTANCE;
    }

    default double maximumSurfaceRange(ServerPlayer player) {
        return DEFAULT_MAXIMUM_SURFACE_RANGE;
    }
}
