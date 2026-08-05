package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalOpenRouteTest {
    @Test
    void onlyUnloadedCrossDimensionTargetsUseDeferredExit() {
        assertEquals(PortalOpenRoute.IMMEDIATE_PAIR, PortalOpenRoute.decide(false, false));
        assertEquals(PortalOpenRoute.IMMEDIATE_PAIR, PortalOpenRoute.decide(false, true));
        assertEquals(PortalOpenRoute.IMMEDIATE_PAIR, PortalOpenRoute.decide(true, true));
        assertEquals(PortalOpenRoute.DEFERRED_EXIT, PortalOpenRoute.decide(true, false));
    }
}
