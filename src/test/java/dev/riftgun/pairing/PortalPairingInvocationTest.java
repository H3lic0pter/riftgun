package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class PortalPairingInvocationTest {
    @Test
    void shortcutDoesNotDependOnCurrentFunctionMode() {
        assertTrue(PortalPairingInvocation.SHORTCUT.allows(
            PortalFunctionMode.COORDINATE_TRAVEL));
        assertTrue(PortalPairingInvocation.SHORTCUT.allows(
            PortalFunctionMode.PORTAL_PAIRING));
        assertFalse(PortalPairingInvocation.MODE_BOUND.allows(
            PortalFunctionMode.COORDINATE_TRAVEL));
        assertTrue(PortalPairingInvocation.MODE_BOUND.allows(
            PortalFunctionMode.PORTAL_PAIRING));
    }
}
