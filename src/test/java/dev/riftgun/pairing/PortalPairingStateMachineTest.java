package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalPairingStateMachineTest {
    @Test
    void eitherEndpointMayBePlacedFirstWithoutFuel() {
        assertFalse(PortalPairingStateMachine.connectsPair(
            PortalPairingStateMachine.State.EMPTY, PortalPairingEndpoint.A));
        assertFalse(PortalPairingStateMachine.connectsPair(
            PortalPairingStateMachine.State.EMPTY, PortalPairingEndpoint.B));
    }

    @Test
    void oppositeEndpointConnectsThePair() {
        assertTrue(PortalPairingStateMachine.connectsPair(
            PortalPairingStateMachine.State.A_ONLY, PortalPairingEndpoint.B));
    }

    @Test
    void replacingSameDormantEndpointDoesNotConnect() {
        assertFalse(PortalPairingStateMachine.connectsPair(
            PortalPairingStateMachine.State.A_ONLY, PortalPairingEndpoint.A));
    }

    @Test
    void replacingConnectedEndpointReconnectsThePair() {
        assertTrue(PortalPairingStateMachine.connectsPair(
            PortalPairingStateMachine.State.CONNECTED, PortalPairingEndpoint.A));
    }
}
