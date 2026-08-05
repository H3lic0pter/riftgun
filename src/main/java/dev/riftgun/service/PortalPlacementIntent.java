package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import org.jetbrains.annotations.Nullable;

/** Entry-side choice captured before asynchronous destination preparation. */
public record PortalPlacementIntent(Route route, @Nullable PortalPlacement attachedPlacement) {
    public static PortalPlacementIntent front() {
        return new PortalPlacementIntent(Route.FRONT, null);
    }

    public static PortalPlacementIntent surface(PortalPlacement placement) {
        return new PortalPlacementIntent(Route.SURFACE, placement);
    }

    public enum Route {
        FRONT,
        SURFACE
    }
}
