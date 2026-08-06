package dev.riftgun.portal;

/** Frozen portal-size capability. Future tiers can extend this enum without changing placement APIs. */
public enum PortalAperture {
    STANDARD,
    EXPANDED;

    public double fuelCostMultiplier() {
        return 1.0;
    }

    public static PortalAperture byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : STANDARD;
    }
}
