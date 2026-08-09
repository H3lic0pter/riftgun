package dev.riftgun.service;

import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.portal.PortalPlacement;
import org.jetbrains.annotations.Nullable;

/** Stable entry-side choice shared by capture and final placement validation. */
public record PortalPlacementIntent(
    Route route,
    @Nullable PortalPlacement attachedPlacement,
    PortalPredictionMode predictionMode
) {
    public static PortalPlacementIntent front(PortalPredictionMode predictionMode) {
        return new PortalPlacementIntent(Route.FRONT, null, predictionMode);
    }

    public static PortalPlacementIntent surface(PortalPlacement placement) {
        return new PortalPlacementIntent(Route.SURFACE, placement, PortalPredictionMode.OFF);
    }

    public enum Route {
        FRONT,
        SURFACE
    }
}
