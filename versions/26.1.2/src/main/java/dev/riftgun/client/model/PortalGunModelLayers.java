package dev.riftgun.client.model;

import dev.riftgun.core.visual.PortalGunVisualSnapshot;

/** Tint-index contract used to filter the canonical Portal Gun model into baked variants. */
public final class PortalGunModelLayers {
    public static final int VARIANT_COUNT = PortalGunVisualSnapshot.VARIANT_COUNT;
    public static final int INNER_CORE_TINT = PortalGunVisualSnapshot.INNER_CORE_TINT;

    public static boolean includesTint(int geometryKey, int tintIndex) {
        return PortalGunVisualSnapshot.includesTint(geometryKey, tintIndex);
    }

    private PortalGunModelLayers() {}
}
