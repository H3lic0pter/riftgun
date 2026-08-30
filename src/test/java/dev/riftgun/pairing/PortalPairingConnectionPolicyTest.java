package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalPairingConnectionPolicyTest {
    @Test
    void eitherEndpointMayBePlacedFirstWithoutConnecting() {
        assertFalse(PortalPairingConnectionPolicy.connectsPair(
            false, false, PortalPairingEndpoint.A));
        assertFalse(PortalPairingConnectionPolicy.connectsPair(
            false, false, PortalPairingEndpoint.B));
    }

    @Test
    void oppositeEndpointConnectsThePair() {
        assertTrue(PortalPairingConnectionPolicy.connectsPair(
            true, false, PortalPairingEndpoint.B));
        assertTrue(PortalPairingConnectionPolicy.connectsPair(
            false, true, PortalPairingEndpoint.A));
    }

    @Test
    void replacingSameDormantEndpointDoesNotConnect() {
        assertFalse(PortalPairingConnectionPolicy.connectsPair(
            true, false, PortalPairingEndpoint.A));
        assertFalse(PortalPairingConnectionPolicy.connectsPair(
            false, true, PortalPairingEndpoint.B));
    }

    @Test
    void replacingConnectedEndpointReconnectsThePair() {
        assertTrue(PortalPairingConnectionPolicy.connectsPair(
            true, true, PortalPairingEndpoint.A));
        assertTrue(PortalPairingConnectionPolicy.connectsPair(
            true, true, PortalPairingEndpoint.B));
    }
}
