package dev.riftgun.api;

/** Stable, machine-readable outcome of a portal-open request. */
public enum PortalOpenStatus {
    OPENED,
    API_NOT_READY,
    WRONG_THREAD,
    NO_PORTAL_GUN,
    SOURCE_POLICY_REJECTED,
    TARGET_DIMENSION_REJECTED,
    TARGET_DIMENSION_UNAVAILABLE,
    TARGET_OUT_OF_BOUNDS,
    ENTRY_PLACEMENT_REJECTED,
    EXIT_PLACEMENT_REJECTED,
    INSUFFICIENT_FUEL,
    PORTAL_OPEN_FAILED
}
