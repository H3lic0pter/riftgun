package dev.riftgun.service;

import net.minecraft.server.level.ServerPlayer;

public interface PortalPlacementCapabilities {
    double DEFAULT_FRONT_DISTANCE = 2.0;
    double DEFAULT_DOWNSHOT_DISTANCE = 2.0;
    float DEFAULT_DOWNSHOT_MINIMUM_PITCH = 78.0F;
    double DEFAULT_MAXIMUM_HORIZONTAL_PREDICTION = 16.0;
    double DEFAULT_MOTION_HISTORY_TELEPORT_THRESHOLD = 8.0;
    int DEFAULT_MOTION_PREDICTION_CALIBRATION_TICKS = 0;
    double DEFAULT_MAXIMUM_SURFACE_RANGE = 32.0;
    double DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE = 0.40;
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

    default double motionHistoryTeleportThreshold(ServerPlayer player) {
        return DEFAULT_MOTION_HISTORY_TELEPORT_THRESHOLD;
    }

    default int motionPredictionCalibrationTicks(ServerPlayer player) {
        return DEFAULT_MOTION_PREDICTION_CALIBRATION_TICKS;
    }

    default double minimumFloatingPortalExposure(ServerPlayer player) {
        return DEFAULT_MINIMUM_FLOATING_PORTAL_EXPOSURE;
    }
}
