package dev.riftgun.data;

/** Aggregate policy shown for one requester in the Privacy Terminal. */
public enum PlayerPermissionProfileMode {
    FOLLOW_GLOBAL,
    ALLOW_ALL,
    DENY_ALL,
    CUSTOM;

    /** Custom is derived from detail edits and is never selected by cycling the outer row. */
    public PlayerPermissionProfileMode nextPreset() {
        return switch (this) {
            case FOLLOW_GLOBAL, CUSTOM -> ALLOW_ALL;
            case ALLOW_ALL -> DENY_ALL;
            case DENY_ALL -> FOLLOW_GLOBAL;
        };
    }

    public static PlayerPermissionProfileMode parse(String name, PlayerPermissionProfileMode fallback) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
