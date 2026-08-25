package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class PortalGuiScrollMemoryTest {
    @AfterEach
    void clearMemory() {
        PortalGuiScrollMemory.clear();
    }

    @Test
    void restoresRememberedPositionsWhileEnabled() {
        PortalGuiScrollMemory.remember(true, 72, 31);

        assertEquals(new PortalGuiScrollMemory.Position(72, 31),
            PortalGuiScrollMemory.restore(true));
    }

    @Test
    void disablingClearsRememberedPositions() {
        PortalGuiScrollMemory.remember(true, 72, 31);

        assertEquals(new PortalGuiScrollMemory.Position(0, 0),
            PortalGuiScrollMemory.restore(false));
        assertEquals(new PortalGuiScrollMemory.Position(0, 0),
            PortalGuiScrollMemory.restore(true));
    }

    @Test
    void negativePositionsAreNormalized() {
        PortalGuiScrollMemory.remember(true, -1, -2);

        assertEquals(new PortalGuiScrollMemory.Position(0, 0),
            PortalGuiScrollMemory.restore(true));
    }
}
