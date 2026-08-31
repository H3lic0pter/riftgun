package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PortalPairingStateMachineTest {
    @Test
    void firstEndpointCreatesPendingStateWithoutFuelCharge() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.EMPTY, PortalPairingEndpoint.A);

        assertEquals(PortalPairingStateMachine.State.A_ONLY, decision.next());
        assertFalse(decision.connectsPair());
        assertFalse(decision.consumesPairFuel());
        assertFalse(decision.resetsBothEndpoints());
        assertFalse(decision.replacesEndpoint());
    }

    @Test
    void oppositeEndpointConnectsAndChargesExactlyOnce() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.A_ONLY, PortalPairingEndpoint.B);

        assertEquals(PortalPairingStateMachine.State.CONNECTED, decision.next());
        assertTrue(decision.connectsPair());
        assertTrue(decision.consumesPairFuel());
        assertTrue(decision.resetsBothEndpoints());
        assertFalse(decision.replacesEndpoint());
    }

    @Test
    void samePendingEndpointIsReplacedWithoutConnecting() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.B_ONLY, PortalPairingEndpoint.B);

        assertEquals(PortalPairingStateMachine.State.B_ONLY, decision.next());
        assertFalse(decision.connectsPair());
        assertTrue(decision.replacesEndpoint());
    }

    @Test
    void connectedEndpointReplacementReconnectsAndResetsPair() {
        var decision = PortalPairingStateMachine.place(
            PortalPairingStateMachine.State.CONNECTED, PortalPairingEndpoint.A);

        assertEquals(PortalPairingStateMachine.State.CONNECTED, decision.next());
        assertTrue(decision.connectsPair());
        assertTrue(decision.consumesPairFuel());
        assertTrue(decision.resetsBothEndpoints());
        assertTrue(decision.replacesEndpoint());
    }

    @Test
    void stateDerivationIsCentralized() {
        assertEquals(PortalPairingStateMachine.State.EMPTY,
            PortalPairingStateMachine.State.from(false, false));
        assertEquals(PortalPairingStateMachine.State.A_ONLY,
            PortalPairingStateMachine.State.from(true, false));
        assertEquals(PortalPairingStateMachine.State.B_ONLY,
            PortalPairingStateMachine.State.from(false, true));
        assertEquals(PortalPairingStateMachine.State.CONNECTED,
            PortalPairingStateMachine.State.from(true, true));
    }
}
