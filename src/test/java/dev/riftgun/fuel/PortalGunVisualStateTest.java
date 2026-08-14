package dev.riftgun.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalGunVisualStateTest {
    @Test
    void encodesEightLiquidStatesAndTwoCoreStates() {
        assertEquals(0, new PortalGunVisualState(0, false, 0).geometryKey());
        assertEquals(1, new PortalGunVisualState(2, false, 0).geometryKey());
        assertEquals(7, new PortalGunVisualState(8, false, 0).geometryKey());
        assertEquals(8, new PortalGunVisualState(0, true, 0).geometryKey());
        assertEquals(15, new PortalGunVisualState(8, true, 0).geometryKey());
    }

    @Test
    void distinguishesMigrationSentinelFromAnEmptyTank() {
        assertFalse(PortalGunVisualState.UNINITIALIZED.initialized());
        assertTrue(new PortalGunVisualState(0, false, 0).initialized());
    }

    @Test
    void quantizesLiquidUsingTheExistingSevenVisualLevels() {
        assertEquals(0, PortalGunVisualState.liquidTintIndex(0, 8000));
        assertEquals(8, PortalGunVisualState.liquidTintIndex(1, 8000));
        assertEquals(7, PortalGunVisualState.liquidTintIndex(400, 8000));
        assertEquals(2, PortalGunVisualState.liquidTintIndex(7600, 8000));
    }
}
