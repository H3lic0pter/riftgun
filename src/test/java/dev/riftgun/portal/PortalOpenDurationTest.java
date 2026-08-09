package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class PortalOpenDurationTest {
    @Test
    void extensionCapIsAppliedToOrdinaryRequests() {
        assertEquals(60, PortalOpenDuration.authorizedSeconds(60, 60, false));
        assertEquals(60, PortalOpenDuration.authorizedSeconds(120, 60, false));
    }

    @Test
    void eternalRequestRequiresTheModule() {
        assertEquals(15, PortalOpenDuration.authorizedSeconds(301, 15, false));
        assertEquals(15, PortalOpenDuration.authorizedSeconds(
            PortalOpenDuration.ETERNAL_SECONDS, 15, false));
        assertEquals(PortalOpenDuration.ETERNAL_SECONDS,
            PortalOpenDuration.authorizedSeconds(301, 300, true));
    }
}
