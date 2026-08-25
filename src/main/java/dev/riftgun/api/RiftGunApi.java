package dev.riftgun.api;

import net.minecraft.network.chat.Component;

/** Stable entry point for Rift Gun integrations. */
public final class RiftGunApi {
    public static final RiftGunApiVersion VERSION = RiftGunApiVersion.CURRENT;

    private static volatile RiftGunPortalApi portalApi = request -> PortalOpenResult.rejected(
        PortalOpenStatus.API_NOT_READY,
        Component.translatable("message.riftgun.api_not_ready"));
    private static boolean portalApiInstalled;

    public static RiftGunPortalApi portals() {
        return portalApi;
    }

    static synchronized void installPortalApi(RiftGunPortalApi implementation) {
        if (portalApiInstalled) throw new IllegalStateException("Rift Gun portal API already installed");
        portalApi = java.util.Objects.requireNonNull(implementation, "implementation");
        portalApiInstalled = true;
    }

    private RiftGunApi() {}
}
