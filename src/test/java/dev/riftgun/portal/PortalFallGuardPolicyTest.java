package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalFallGuardPolicyTest {
    @Test
    void playerAndEntityPreferencesRemainIndependent() {
        assertTrue(PortalFallGuardPolicy.applies(true, true, true, false));
        assertFalse(PortalFallGuardPolicy.applies(true, true, false, true));
        assertTrue(PortalFallGuardPolicy.applies(false, true, false, true));
        assertFalse(PortalFallGuardPolicy.applies(false, true, true, false));
        assertFalse(PortalFallGuardPolicy.applies(false, false, true, true));
    }
}
