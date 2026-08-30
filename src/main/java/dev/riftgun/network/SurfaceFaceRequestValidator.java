package dev.riftgun.network;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.service.PortalPlacementIntent;

/** Validates explicit-face requests before their placement intent is resolved. */
public final class SurfaceFaceRequestValidator {
    public static void validate(PortalPlacementMode mode, PortalPlacementIntent intent) {
        if ((mode != PortalPlacementMode.SURFACE && mode != PortalPlacementMode.SMART)
            || intent == null || intent.route() != PortalPlacementIntent.Route.SURFACE) {
            throw PortalRequestFields.error("message.riftgun.surface_mode_required");
        }
    }

    private SurfaceFaceRequestValidator() {}
}
