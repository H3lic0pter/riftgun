package dev.riftgun.network;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.service.PortalPlacementIntent;
import dev.riftgun.service.SurfaceFacePlacementPolicy;

/** Validates explicit-face requests before their placement intent is resolved. */
public final class SurfaceFaceRequestValidator {
    public static void validate(PortalPlacementMode mode, PortalPlacementIntent intent) {
        if (!SurfaceFacePlacementPolicy.accepts(mode, intent)) {
            throw PortalRequestFields.error("message.riftgun.surface_mode_required");
        }
    }

    private SurfaceFaceRequestValidator() {}
}
