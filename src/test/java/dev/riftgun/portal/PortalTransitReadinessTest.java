package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalTransitReadinessTest {
    @Test
    void openTargetIsReadyOnBothNodes() {
        assertEquals(PortalTransitReadiness.READY,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.OPEN, true, false));
        assertEquals(PortalTransitReadiness.READY,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.OPEN, false, true));
    }

    @Test
    void modernNodeAllowsPreOpenTargetWhileItsChunkIsNotTicking() {
        assertEquals(PortalTransitReadiness.ASYNC_CHUNK_LOADING,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.CHARGING, false, true));
        assertEquals(PortalTransitReadiness.ASYNC_CHUNK_LOADING,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.OPENING, false, true));
    }

    @Test
    void tickingOrLegacyPreOpenTargetRemainsBlocked() {
        assertEquals(PortalTransitReadiness.BLOCKED_OPENING,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.OPENING, true, true));
        assertEquals(PortalTransitReadiness.BLOCKED_OPENING,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.OPENING, false, false));
    }

    @Test
    void closingTargetIsNeverAllowedByAsyncBypass() {
        assertEquals(PortalTransitReadiness.BLOCKED_CLOSING,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.CLOSING, false, true));
        assertEquals(PortalTransitReadiness.BLOCKED_CLOSED,
            PortalTransitReadiness.evaluate(PortalLifecycle.Phase.CLOSED, false, true));
    }
}
