package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import org.junit.jupiter.api.Test;

final class PortalGunCapabilitiesTest {
    @Test
    void configuredRemoteDistanceCanBeLowerThanTheBaseCapability() {
        assertEquals(1, PortalGunCapabilities.configuredRemoteDistance(1, 80));
        assertEquals(24, PortalGunCapabilities.configuredRemoteDistance(24, 80));
        assertEquals(80, PortalGunCapabilities.configuredRemoteDistance(120, 80));
    }

    @Test
    void smartRangeDependsOnCapabilityMaximumNotRemoteDistance() {
        int remoteDistance = PortalGunCapabilities.configuredRemoteDistance(12, 80);
        int smartRange = PortalGunCapabilities.configuredSmartDistance(64, 80);

        assertEquals(12, remoteDistance);
        assertEquals(64, smartRange);
    }

    @Test
    void pairingRemoteFallbackRequiresBothIndependentModules() {
        assertEquals(PortalFloatingFallback.FRONT,
            PortalGunCapabilities.configuredPairingSmartFallback(
                false, false, PortalFloatingFallback.REMOTE));
        assertEquals(PortalFloatingFallback.FRONT,
            PortalGunCapabilities.configuredPairingSmartFallback(
                true, false, PortalFloatingFallback.REMOTE));
        assertEquals(PortalFloatingFallback.FRONT,
            PortalGunCapabilities.configuredPairingSmartFallback(
                false, true, PortalFloatingFallback.REMOTE));
        assertEquals(PortalFloatingFallback.REMOTE,
            PortalGunCapabilities.configuredPairingSmartFallback(
                true, true, PortalFloatingFallback.REMOTE));
    }

    @Test
    void unavailableRemoteUsesFrontWithoutChangingThePreferredValue() {
        PortalPlacementMode preferred = PortalPlacementMode.REMOTE;

        assertEquals(PortalPlacementMode.FRONT,
            PortalGunCapabilities.effectivePlacementMode(preferred, false));
        assertEquals(PortalPlacementMode.REMOTE,
            PortalGunCapabilities.effectivePlacementMode(preferred, true));
        assertEquals(PortalPlacementMode.REMOTE, preferred);
    }

    @Test
    void remoteDistanceControlsFollowEffectiveRemoteRoutes() {
        assertTrue(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.REMOTE, PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.FRONT));
        assertTrue(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.SMART, PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.REMOTE));
        assertTrue(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.SMART, PortalFunctionMode.PORTAL_PAIRING,
            PortalFloatingFallback.REMOTE));
        assertTrue(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.ENTITY_RELOCATION, PortalFunctionMode.PORTAL_PAIRING,
            PortalFloatingFallback.FRONT));
        assertFalse(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.SMART, PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.FRONT));
        assertFalse(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.ENTITY_RELOCATION, PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.REMOTE));
        assertFalse(PortalGunCapabilities.usesRemoteDistanceControls(
            PortalPlacementMode.FRONT, PortalFunctionMode.PORTAL_PAIRING,
            PortalFloatingFallback.REMOTE));
    }
}
