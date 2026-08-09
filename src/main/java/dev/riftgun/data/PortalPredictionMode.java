package dev.riftgun.data;

/** How front placement accounts for the player's motion. */
public enum PortalPredictionMode {
    /** Fixed door distance, no motion lead. */
    OFF,
    /** Door distance grows with the projection of current velocity onto the view. */
    PROJECTION,
    /** Existing trajectory model: fixed distance plus displacement extrapolation. */
    TRAJECTORY;

    public PortalPredictionMode next() {
        return switch (this) {
            case OFF -> PROJECTION;
            case PROJECTION -> TRAJECTORY;
            case TRAJECTORY -> OFF;
        };
    }

    public static PortalPredictionMode parse(String name, PortalPredictionMode fallback) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
