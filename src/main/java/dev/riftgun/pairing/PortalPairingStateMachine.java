package dev.riftgun.pairing;

/** Pure transition decisions for endpoint placement; world mutation happens in the manager. */
public final class PortalPairingStateMachine {
    public enum State { EMPTY, A_ONLY, B_ONLY, CONNECTED }

    public record Decision(State next, boolean consumesPairFuel, boolean resetsBothEndpoints) {}

    public static Decision place(State state, PortalPairingEndpoint endpoint) {
        if (state == null || endpoint != PortalPairingEndpoint.A && endpoint != PortalPairingEndpoint.B) {
            throw new IllegalArgumentException("A or B endpoint required");
        }
        if (state == State.CONNECTED) return new Decision(State.CONNECTED, true, true);
        boolean completes = state == State.A_ONLY && endpoint == PortalPairingEndpoint.B
            || state == State.B_ONLY && endpoint == PortalPairingEndpoint.A;
        if (completes) return new Decision(State.CONNECTED, true, true);
        State next = endpoint == PortalPairingEndpoint.A ? State.A_ONLY : State.B_ONLY;
        return new Decision(next, false, false);
    }

    private PortalPairingStateMachine() {}
}
