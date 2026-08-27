package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PortalSmartFallbackPolicyTest {
    @Test
    void smartFallsBackWhenNearbySurfaceCannotHostAPortal() {
        assertTrue(VanillaPortalPlacementResolver.shouldUseFloatingFallback(true, false));
    }

    @Test
    void explicitSurfaceModeKeepsItsPlacementFailure() {
        assertFalse(VanillaPortalPlacementResolver.shouldUseFloatingFallback(false, false));
    }

    @Test
    void smartKeepsAValidAttachedPlacement() {
        assertFalse(VanillaPortalPlacementResolver.shouldUseFloatingFallback(true, true));
    }
}
