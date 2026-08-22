package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalOpenRouteTest {
    @Test
    void modernNodeDefersEveryUnloadedTarget() {
        assertEquals(PortalOpenRoute.DEFERRED_EXIT,
            PortalOpenRoute.decide(false, false, true));
        assertEquals(PortalOpenRoute.DEFERRED_EXIT,
            PortalOpenRoute.decide(true, false, true));
        assertEquals(PortalOpenRoute.IMMEDIATE_PAIR,
            PortalOpenRoute.decide(false, true, true));
    }

    @Test
    void legacyNodeKeepsSameDimensionImmediatePair() {
        assertEquals(PortalOpenRoute.IMMEDIATE_PAIR,
            PortalOpenRoute.decide(false, false, false));
        assertEquals(PortalOpenRoute.DEFERRED_EXIT,
            PortalOpenRoute.decide(true, false, false));
    }
}
