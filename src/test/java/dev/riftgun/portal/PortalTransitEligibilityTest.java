package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalTransitEligibilityTest {
    @Test
    void disabledPassengerTreeTransitRejectsMountedTreesOnly() {
        assertFalse(PortalTransitEligibility.allowsPassengerTree(false, true));
        assertTrue(PortalTransitEligibility.allowsPassengerTree(false, false));
    }

    @Test
    void enabledPassengerTreeTransitPreservesCurrentBehavior() {
        assertTrue(PortalTransitEligibility.allowsPassengerTree(true, true));
        assertTrue(PortalTransitEligibility.allowsPassengerTree(true, false));
    }
}
