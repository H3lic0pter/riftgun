package dev.riftgun.client.model;

import dev.riftgun.fuel.PortalGunVisualState;

/** Selects one of the model's discrete fuel columns from the stored-to-nominal ratio. */
public final class PortalGunFluidLevel {
    public static final int FULL_TINT = 2;
    public static final int LOWEST_TINT = 8;

    public static int tintIndex(int amount, int nominalCapacity) {
        int tint = PortalGunVisualState.liquidTintIndex(amount, nominalCapacity);
        return tint == 0 ? -1 : tint;
    }

    public static boolean isLiquidTint(int tintIndex) {
        return PortalGunVisualState.isLiquidTint(tintIndex);
    }

    private PortalGunFluidLevel() {}
}
