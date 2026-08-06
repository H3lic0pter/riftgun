package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;

/** Maps a frozen aperture capability to geometry and clearance policy. */
final class PortalAperturePolicy {
    static final double EXPANDED_MINIMUM_EXPOSURE = 0.85;

    static boolean expanded(PortalAperture aperture) {
        return aperture == PortalAperture.EXPANDED;
    }

    static PortalGeometry floatingVertical() {
        return PortalGeometry.FLOATING_EXPANDED;
    }

    static PortalGeometry attachedVertical() {
        return PortalGeometry.SURFACE_EXPANDED;
    }

    static PortalGeometry horizontal() {
        return PortalGeometry.HORIZONTAL_EXPANDED;
    }

    private PortalAperturePolicy() {}
}
