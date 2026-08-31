package dev.riftgun.service;

import dev.riftgun.data.PortalPlacementMode;

/** Pure domain policy for accepting an explicitly selected surface placement. */
public final class SurfaceFacePlacementPolicy {
    public static boolean accepts(PortalPlacementMode mode, PortalPlacementIntent intent) {
        return (mode == PortalPlacementMode.SURFACE || mode == PortalPlacementMode.SMART)
            && intent != null && intent.route() == PortalPlacementIntent.Route.SURFACE;
    }

    private SurfaceFacePlacementPolicy() {}
}
