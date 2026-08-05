package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalFuelCostTest {
    @Test
    void randomConsumptionIncludesBothConfiguredBounds() {
        assertEquals(5, PortalFuelCost.choose(5, 8, true, ignored -> 0));
        assertEquals(8, PortalFuelCost.choose(5, 8, true, bound -> bound - 1));
    }

    @Test
    void disablingRandomConsumptionUsesMinimum() {
        assertEquals(5, PortalFuelCost.choose(5, 8, false, ignored -> 3));
    }

    @Test
    void remainingFuelAboveMinimumIsFullyUsable() {
        assertEquals(6, PortalFuelCost.affordableCost(6, 5, 8));
        assertEquals(0, PortalFuelCost.affordableCost(4, 5, 5));
    }
}
