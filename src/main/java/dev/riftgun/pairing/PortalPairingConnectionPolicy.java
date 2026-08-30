package dev.riftgun.pairing;

/** Decides whether placing an endpoint completes or replaces a connected pair. */
public final class PortalPairingConnectionPolicy {
    public static boolean connectsPair(boolean hasA, boolean hasB,
                                       PortalPairingEndpoint endpoint) {
        if (endpoint != PortalPairingEndpoint.A && endpoint != PortalPairingEndpoint.B) {
            throw new IllegalArgumentException("A or B endpoint required");
        }
        return hasA && hasB
            || endpoint == PortalPairingEndpoint.A && hasB
            || endpoint == PortalPairingEndpoint.B && hasA;
    }

    private PortalPairingConnectionPolicy() {}
}
