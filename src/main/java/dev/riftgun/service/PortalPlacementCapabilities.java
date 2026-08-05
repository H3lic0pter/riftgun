package dev.riftgun.service;

import net.minecraft.server.level.ServerPlayer;

public interface PortalPlacementCapabilities {
    double DEFAULT_FRONT_DISTANCE = 2.0;
    double DEFAULT_DOWNSHOT_DISTANCE = 2.0;
    float DEFAULT_DOWNSHOT_MINIMUM_PITCH = 78.0F;
    double DEFAULT_MAXIMUM_HORIZONTAL_PREDICTION = 16.0;
    double DEFAULT_MAXIMUM_SURFACE_RANGE = 32.0;
    PortalPlacementCapabilities DEFAULT = new PortalPlacementCapabilities() {};

    default double frontDistance(ServerPlayer player) {
        return DEFAULT_FRONT_DISTANCE;
    }

    default double maximumSurfaceRange(ServerPlayer player) {
        return DEFAULT_MAXIMUM_SURFACE_RANGE;
    }

    default double downshotDistance(ServerPlayer player) {
        return DEFAULT_DOWNSHOT_DISTANCE;
    }

    default float downshotMinimumPitch(ServerPlayer player) {
        return DEFAULT_DOWNSHOT_MINIMUM_PITCH;
    }

    default double maximumHorizontalPrediction(ServerPlayer player) {
        return DEFAULT_MAXIMUM_HORIZONTAL_PREDICTION;
    }
}
