package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import org.jetbrains.annotations.Nullable;

/** Stable entry-side choice shared by capture and final placement validation. */
public record PortalPlacementIntent(
    Route route,
    @Nullable PortalPlacement attachedPlacement,
    boolean motionPrediction
) {
    public static PortalPlacementIntent front(boolean motionPrediction) {
        return new PortalPlacementIntent(Route.FRONT, null, motionPrediction);
    }

    public static PortalPlacementIntent surface(PortalPlacement placement) {
        return new PortalPlacementIntent(Route.SURFACE, placement, false);
    }

    public enum Route {
        FRONT,
        SURFACE
    }
}
