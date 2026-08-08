package dev.riftgun.data;

/** How a player's own Player Target portals may be opened by others. */
public enum TargetPrivacy {
    /** Anyone may open a Player Target portal next to this player. */
    PUBLIC,
    /** Others must be allowed by an explicit in-flight request response or override. */
    REQUEST,
    /** No one may open a Player Target portal next to this player. */
    PRIVATE;

    public TargetPrivacy next() {
        return switch (this) {
            case PUBLIC -> REQUEST;
            case REQUEST -> PRIVATE;
            case PRIVATE -> PUBLIC;
        };
    }

    public static TargetPrivacy parse(String name, TargetPrivacy fallback) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
