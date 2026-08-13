package dev.riftgun.fuel;

public record PortalFuelUse(PortalFuelProfile profile, int amount, boolean virtual) {
    public PortalFuelUse(PortalFuelProfile profile, int amount) {
        this(profile, amount, false);
    }
}
