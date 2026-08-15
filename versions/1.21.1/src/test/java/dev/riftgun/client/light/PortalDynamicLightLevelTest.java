package dev.riftgun.client.light;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.portal.PortalLifecycle;
import org.junit.jupiter.api.Test;

class PortalDynamicLightLevelTest {
    @Test
    void followsPortalOpeningAndClosingProgress() {
        assertEquals(0, level(PortalLifecycle.Phase.CHARGING, 5));
        assertEquals(0, level(PortalLifecycle.Phase.OPENING, 0));
        assertEquals(4, level(PortalLifecycle.Phase.OPENING, 2));
        assertEquals(9, level(PortalLifecycle.Phase.OPEN, 0));
        assertEquals(9, level(PortalLifecycle.Phase.CLOSING, 0));
        assertEquals(5, level(PortalLifecycle.Phase.CLOSING, 2));
        assertEquals(0, level(PortalLifecycle.Phase.CLOSED, 0));
    }

    @Test
    void clampsProviderValuesToMinecraftLuminanceRange() {
        assertEquals(0, PortalDynamicLightLevel.forLifecycle(
            PortalLifecycle.Phase.OPEN, 0, -4));
        assertEquals(15, PortalDynamicLightLevel.forLifecycle(
            PortalLifecycle.Phase.OPEN, 0, 99));
    }

    private static int level(PortalLifecycle.Phase phase, int phaseTicks) {
        return PortalDynamicLightLevel.forLifecycle(
            phase, phaseTicks, PortalDynamicLightLevel.DEFAULT_MAXIMUM);
    }
}
