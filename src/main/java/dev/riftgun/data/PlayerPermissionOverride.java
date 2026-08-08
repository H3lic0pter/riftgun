package dev.riftgun.data;

/** Per-player permission override stored on the target's data. */
public enum PlayerPermissionOverride {
    /** Follow the target's global privacy setting. */
    DEFAULT,
    /** Always allow this requester, regardless of global privacy. */
    ALLOW,
    /** Always deny this requester, regardless of global privacy. */
    DENY;

    public PlayerPermissionOverride next() {
        return switch (this) {
            case DEFAULT -> ALLOW;
            case ALLOW -> DENY;
            case DENY -> DEFAULT;
        };
    }

    public static PlayerPermissionOverride parse(String name, PlayerPermissionOverride fallback) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
