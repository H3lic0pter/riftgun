package dev.riftgun.pairing;

/** Pure transition decisions for A/B endpoint placement; world mutation stays in the manager. */
public final class PortalPairingStateMachine {
    public enum State {
        EMPTY(false, false),
        A_ONLY(true, false),
        B_ONLY(false, true),
        CONNECTED(true, true);

        private final boolean hasA;
        private final boolean hasB;

        State(boolean hasA, boolean hasB) {
            this.hasA = hasA;
            this.hasB = hasB;
        }

        public static State from(boolean hasA, boolean hasB) {
            if (hasA && hasB) return CONNECTED;
            if (hasA) return A_ONLY;
            if (hasB) return B_ONLY;
            return EMPTY;
        }

        boolean has(PortalPairingEndpoint endpoint) {
            return endpoint == PortalPairingEndpoint.A ? hasA : hasB;
        }
    }

    public record Decision(State previous, State next, boolean connectsPair,
                           boolean consumesPairFuel, boolean resetsBothEndpoints,
                           boolean replacesEndpoint) {}

    public static Decision place(State state, PortalPairingEndpoint endpoint) {
        if (state == null || endpoint != PortalPairingEndpoint.A
            && endpoint != PortalPairingEndpoint.B) {
            throw new IllegalArgumentException("A or B endpoint required");
        }
        boolean connects = state == State.CONNECTED || state.has(endpoint.opposite());
        State next = connects ? State.CONNECTED
            : endpoint == PortalPairingEndpoint.A ? State.A_ONLY : State.B_ONLY;
        return new Decision(state, next, connects, connects, connects, state.has(endpoint));
    }

    private PortalPairingStateMachine() {}
}
