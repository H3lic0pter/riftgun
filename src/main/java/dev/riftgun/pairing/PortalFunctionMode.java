package dev.riftgun.pairing;

public enum PortalFunctionMode {
    COORDINATE_TRAVEL,
    PORTAL_PAIRING;

    public PortalFunctionMode toggle() {
        return this == COORDINATE_TRAVEL ? PORTAL_PAIRING : COORDINATE_TRAVEL;
    }
}
