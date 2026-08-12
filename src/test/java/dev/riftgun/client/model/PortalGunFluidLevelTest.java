package dev.riftgun.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalGunFluidLevelTest {
    @Test
    void selectsNearestAuthoredFuelColumn() {
        assertEquals(-1, PortalGunFluidLevel.tintIndex(0, 8000));
        assertEquals(8, PortalGunFluidLevel.tintIndex(1, 8000));
        assertEquals(7, PortalGunFluidLevel.tintIndex(400, 8000));
        assertEquals(6, PortalGunFluidLevel.tintIndex(1600, 8000));
        assertEquals(5, PortalGunFluidLevel.tintIndex(3200, 8000));
        assertEquals(4, PortalGunFluidLevel.tintIndex(4800, 8000));
        assertEquals(3, PortalGunFluidLevel.tintIndex(6400, 8000));
        assertEquals(2, PortalGunFluidLevel.tintIndex(7600, 8000));
    }

    @Test
    void overflowStaysFullAndCapacityModulesChangeTheRatio() {
        assertEquals(2, PortalGunFluidLevel.tintIndex(8500, 8000));
        assertEquals(5, PortalGunFluidLevel.tintIndex(8000, 16000));
    }

    @Test
    void recognizesOnlyFuelTintSlots() {
        assertFalse(PortalGunFluidLevel.isLiquidTint(1));
        assertTrue(PortalGunFluidLevel.isLiquidTint(2));
        assertTrue(PortalGunFluidLevel.isLiquidTint(8));
        assertFalse(PortalGunFluidLevel.isLiquidTint(9));
    }
}
