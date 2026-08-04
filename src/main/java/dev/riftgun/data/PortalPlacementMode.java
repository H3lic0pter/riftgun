package dev.riftgun.data;

public enum PortalPlacementMode {
    SMART,
    FRONT,
    SURFACE;

    public PortalPlacementMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static PortalPlacementMode parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return SMART;
        }
    }
}
