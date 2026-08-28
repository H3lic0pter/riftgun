package dev.riftgun.pairing;

public enum PortalPairingEndpoint {
    NONE,
    A,
    B,
    ENTITY_TARGET;

    public PortalPairingEndpoint opposite() {
        return switch (this) {
            case A -> B;
            case B -> A;
            case NONE, ENTITY_TARGET -> NONE;
        };
    }

    public static PortalPairingEndpoint byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }

}
