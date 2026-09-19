package dev.riftgun.portal;

/** Frozen portal-size capability. Future tiers can extend this enum without changing placement APIs. */
public enum PortalAperture {
    STANDARD,
    EXPANDED, // Legacy full-support mode; keep persisted ordinals stable.
    EXPANDED_ADAPTIVE,
    EXPANDED_PREFER_LARGE;

    public boolean expanded() { return this != STANDARD; }
    public boolean requiresFullSupport() { return this == EXPANDED; }

    public double fuelCostMultiplier() {
        return 1.0;
    }

    public static PortalAperture byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : STANDARD;
    }
}
