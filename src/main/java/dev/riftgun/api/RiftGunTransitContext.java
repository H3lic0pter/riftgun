package dev.riftgun.api;

import java.util.Optional;

/** Read-only view of the synchronous Rift Gun transit currently invoking Minecraft transfer. */
public final class RiftGunTransitContext {
    public static Optional<PortalTransitAuthorization> currentAuthorization() {
        return RiftGunApiBootstrap.currentTransitAuthorization();
    }

    private RiftGunTransitContext() {}
}
