package dev.riftgun.client.model;

/** Selects one of the model's discrete fuel columns from the stored-to-nominal ratio. */
public final class PortalGunFluidLevel {
    public static final int FULL_TINT = 2;
    public static final int LOWEST_TINT = 8;

    public static int tintIndex(int amount, int nominalCapacity) {
        if (amount <= 0 || nominalCapacity <= 0) return -1;
        double ratio = (double) amount / nominalCapacity;
        if (ratio >= 0.95) return 2;
        if (ratio >= 0.80) return 3;
        if (ratio >= 0.60) return 4;
        if (ratio >= 0.40) return 5;
        if (ratio >= 0.20) return 6;
        if (ratio >= 0.05) return 7;
        return 8;
    }

    public static boolean isLiquidTint(int tintIndex) {
        return tintIndex >= FULL_TINT && tintIndex <= LOWEST_TINT;
    }

    private PortalGunFluidLevel() {}
}
