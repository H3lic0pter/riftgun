package dev.riftgun.pairing;

/** Pure transition decisions for endpoint placement; world mutation happens in the manager. */
public final class PortalPairingStateMachine {
    public enum State { EMPTY, A_ONLY, B_ONLY, CONNECTED }

    public static boolean connectsPair(State state, PortalPairingEndpoint endpoint) {
        if (state == null || endpoint != PortalPairingEndpoint.A && endpoint != PortalPairingEndpoint.B) {
            throw new IllegalArgumentException("A or B endpoint required");
        }
        return state == State.CONNECTED
            || state == State.A_ONLY && endpoint == PortalPairingEndpoint.B
            || state == State.B_ONLY && endpoint == PortalPairingEndpoint.A;
    }

    private PortalPairingStateMachine() {}
}
