package dev.riftgun.fuel;

import java.util.function.IntUnaryOperator;

public final class PortalFuelCost {
    public static int choose(int minimum, int maximum, boolean randomConsumption,
                             IntUnaryOperator nextInt) {
        int safeMinimum = Math.max(1, minimum);
        int safeMaximum = Math.max(safeMinimum, maximum);
        if (!randomConsumption || safeMinimum == safeMaximum) return safeMinimum;
        return safeMinimum + nextInt.applyAsInt(safeMaximum - safeMinimum + 1);
    }

    public static int affordableCost(int stored, int minimum, int rolledCost) {
        return stored < minimum ? 0 : Math.min(stored, Math.max(minimum, rolledCost));
    }

    private PortalFuelCost() {}
}
