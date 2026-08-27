package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalPairingStateMachineTest {
    @Test
    void eitherEndpointMayBePlacedFirstWithoutFuel() {
        var a = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.EMPTY, PortalPairingEndpoint.A);
        var b = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.EMPTY, PortalPairingEndpoint.B);
        assertEquals(PortalPairingStateMachine.State.A_ONLY, a.next());
        assertEquals(PortalPairingStateMachine.State.B_ONLY, b.next());
        assertFalse(a.consumesPairFuel());
        assertFalse(b.consumesPairFuel());
    }

    @Test
    void oppositeEndpointConnectsAndResetsBoth() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.A_ONLY, PortalPairingEndpoint.B);
        assertEquals(PortalPairingStateMachine.State.CONNECTED, decision.next());
        assertTrue(decision.consumesPairFuel());
        assertTrue(decision.resetsBothEndpoints());
    }

    @Test
    void replacingSameDormantEndpointDoesNotConsume() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.A_ONLY, PortalPairingEndpoint.A);
        assertEquals(PortalPairingStateMachine.State.A_ONLY, decision.next());
        assertFalse(decision.consumesPairFuel());
    }

    @Test
    void replacingConnectedEndpointConsumesAndResetsPair() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.CONNECTED, PortalPairingEndpoint.A);
        assertTrue(decision.consumesPairFuel());
        assertTrue(decision.resetsBothEndpoints());
    }
}
