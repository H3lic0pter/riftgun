package dev.riftgun.data;

/** A concrete or inherited decision for one privacy permission. */
public enum PortalPermissionPolicy {
    FOLLOW_GLOBAL,
    ALLOW,
    ASK,
    DENY;

    public PortalPermissionPolicy next(boolean supportsAsk) {
        return switch (this) {
            case FOLLOW_GLOBAL -> ALLOW;
            case ALLOW -> supportsAsk ? ASK : DENY;
            case ASK -> DENY;
            case DENY -> FOLLOW_GLOBAL;
        };
    }

    public PortalPermissionPolicy previous(boolean supportsAsk) {
        return switch (this) {
            case FOLLOW_GLOBAL -> DENY;
            case ALLOW -> FOLLOW_GLOBAL;
            case ASK -> ALLOW;
            case DENY -> supportsAsk ? ASK : ALLOW;
        };
    }

    public static PortalPermissionPolicy parse(String name, PortalPermissionPolicy fallback) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
