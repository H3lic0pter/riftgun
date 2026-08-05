package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalPairClockTest {
    @Test
    void bothEndpointsDeriveLifecycleFromTheSameAbsoluteClock() {
        long startedAt = 100L;

        assertEquals(PortalLifecycle.Phase.CHARGING, PortalPairClock.phase(startedAt, -1L, 100L));
        assertEquals(PortalLifecycle.Phase.OPENING, PortalPairClock.phase(startedAt, -1L, 106L));
        assertEquals(PortalLifecycle.Phase.OPEN, PortalPairClock.phase(startedAt, -1L, 111L));
        assertEquals(PortalLifecycle.Phase.CLOSING, PortalPairClock.phase(startedAt, 120L, 123L));
        assertEquals(PortalLifecycle.Phase.CLOSED, PortalPairClock.phase(startedAt, 120L, 125L));
    }
}
