package dev.riftgun.data;

public enum PortalPlacementMode {
    SMART,
    FRONT,
    REMOTE,
    SURFACE,
    ENTITY_RELOCATION;

    public PortalPlacementMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public PortalPlacementMode previous() {
        return values()[(ordinal() - 1 + values().length) % values().length];
    }

    public static PortalPlacementMode parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return SMART;
        }
    }
}
