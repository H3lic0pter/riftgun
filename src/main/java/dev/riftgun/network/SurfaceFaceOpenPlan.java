package dev.riftgun.network;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.service.PortalPlacementIntent;

/** Validated routing decision shared by coordinate and pairing explicit-face opens. */
public record SurfaceFaceOpenPlan(Route route, PortalPlacementIntent intent) {
    public static SurfaceFaceOpenPlan create(PortalPlacementMode mode,
                                             PortalFunctionMode functionMode,
                                             PortalPlacementIntent intent) {
        if ((mode != PortalPlacementMode.SURFACE && mode != PortalPlacementMode.SMART)
            || intent == null || intent.route() != PortalPlacementIntent.Route.SURFACE) {
            throw PortalRequestFields.error("message.riftgun.surface_mode_required");
        }
        return new SurfaceFaceOpenPlan(functionMode == PortalFunctionMode.PORTAL_PAIRING
            ? Route.PAIRING : Route.COORDINATE, intent);
    }

    public enum Route { COORDINATE, PAIRING }
}
