package dev.riftgun.fuel;

/** Separates automation capability exposure from player-held bucket interaction. */
public final class PortalGunCapabilityPolicy {
    public enum Access {
        CAPABILITY,
        DIRECT_INTERACTION
    }

    public static boolean allows(Access access, boolean bucketMode) {
        return access == Access.CAPABILITY || bucketMode;
    }

    private PortalGunCapabilityPolicy() {}
}
