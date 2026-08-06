package dev.riftgun.portal;

public enum PortalGeometry {
    FLOATING_VERTICAL(1.2F, 2.2F),
    SURFACE_VERTICAL(1.0F, 2.0F),
    SURFACE_COMPACT(1.0F, 1.0F),
    HORIZONTAL(1.0F, 1.0F),
    FLOATING_EXPANDED(2.2F, 2.2F),
    SURFACE_EXPANDED(2.0F, 2.0F),
    HORIZONTAL_EXPANDED(2.0F, 2.0F);

    private final float width;
    private final float height;

    PortalGeometry(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public boolean expanded() {
        return this == FLOATING_EXPANDED || this == SURFACE_EXPANDED || this == HORIZONTAL_EXPANDED;
    }

    public static PortalGeometry byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : FLOATING_VERTICAL;
    }
}
