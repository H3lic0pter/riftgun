package dev.riftgun.client.model;

import dev.riftgun.fuel.PortalGunVisualState;

/** Tint-index contract used to filter the canonical Portal Gun model into baked variants. */
public final class PortalGunModelLayers {
    public static final int VARIANT_COUNT = 16;

    public static boolean includesTint(int geometryKey, int tintIndex) {
        if (geometryKey < 0 || geometryKey >= VARIANT_COUNT) return false;
        if (PortalGunVisualState.isLiquidTint(tintIndex)) {
            int liquidSlot = geometryKey & 7;
            return liquidSlot != 0 && tintIndex == liquidSlot + 1;
        }
        if (tintIndex == PortalGunCoreColors.OUTER_TINT
            || tintIndex == PortalGunCoreColors.INNER_TINT) {
            return (geometryKey & 8) != 0;
        }
        return true;
    }

    private PortalGunModelLayers() {}
}
