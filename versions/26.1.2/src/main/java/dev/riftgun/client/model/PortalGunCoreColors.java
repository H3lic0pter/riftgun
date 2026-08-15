package dev.riftgun.client.model;

import dev.riftgun.core.visual.PortalGunVisualSnapshot;

/** Derives the two zero-point core highlights from the active fuel theme. */
public final class PortalGunCoreColors {
    public static final int OUTER_TINT = PortalGunVisualSnapshot.OUTER_CORE_TINT;
    public static final int INNER_TINT = PortalGunVisualSnapshot.INNER_CORE_TINT;
    public static int outer(int rgb) {
        return PortalGunVisualSnapshot.outerCoreArgb(rgb);
    }

    public static int inner(int rgb) {
        return PortalGunVisualSnapshot.innerCoreArgb(rgb);
    }

    private PortalGunCoreColors() {}
}
