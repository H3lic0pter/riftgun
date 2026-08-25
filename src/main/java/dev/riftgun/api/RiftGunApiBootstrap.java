package dev.riftgun.api;

/**
 * Rift Gun implementation bridge. Integrating mods should use {@link RiftGunApi}
 * and must never call this class.
 */
public final class RiftGunApiBootstrap {
    private static final ThreadLocal<PortalTransitAuthorization> TRANSIT_AUTHORIZATION = new ThreadLocal<>();

    public static void installPortalApi(RiftGunPortalApi implementation) {
        RiftGunApi.installPortalApi(implementation);
    }

    public static void installCoordinateNoteApi(RiftGunCoordinateNoteApi implementation) {
        RiftGunApi.installCoordinateNoteApi(implementation);
    }

    static java.util.Optional<PortalTransitAuthorization> currentTransitAuthorization() {
        return java.util.Optional.ofNullable(TRANSIT_AUTHORIZATION.get());
    }

    public static <T> T withTransitAuthorization(
        java.util.Optional<PortalTransitAuthorization> authorization,
        java.util.function.Supplier<T> action
    ) {
        java.util.Objects.requireNonNull(authorization, "authorization");
        java.util.Objects.requireNonNull(action, "action");
        if (authorization.isEmpty()) return action.get();

        PortalTransitAuthorization previous = TRANSIT_AUTHORIZATION.get();
        TRANSIT_AUTHORIZATION.set(authorization.orElseThrow());
        try {
            return action.get();
        } finally {
            if (previous == null) TRANSIT_AUTHORIZATION.remove();
            else TRANSIT_AUTHORIZATION.set(previous);
        }
    }

    private RiftGunApiBootstrap() {}
}
