package dev.riftgun.api;

import net.minecraft.network.chat.Component;

/** Stable entry point for Rift Gun integrations. */
public final class RiftGunApi {
    public static final RiftGunApiVersion VERSION = RiftGunApiVersion.CURRENT;

    private static volatile RiftGunPortalApi portalApi = request -> PortalOpenResult.rejected(
        PortalOpenStatus.API_NOT_READY,
        Component.translatable("message.riftgun.api_not_ready"));
    private static volatile RiftGunCoordinateNoteApi coordinateNoteApi = request ->
        new CoordinateNoteResult(CoordinateNoteStatus.API_NOT_READY,
            Component.translatable("message.riftgun.api_not_ready"));
    private static boolean portalApiInstalled;
    private static boolean coordinateNoteApiInstalled;

    public static RiftGunPortalApi portals() {
        return portalApi;
    }

    public static RiftGunCoordinateNoteApi coordinateNotes() {
        return coordinateNoteApi;
    }

    static synchronized void installPortalApi(RiftGunPortalApi implementation) {
        if (portalApiInstalled) throw new IllegalStateException("Rift Gun portal API already installed");
        portalApi = java.util.Objects.requireNonNull(implementation, "implementation");
        portalApiInstalled = true;
    }

    static synchronized void installCoordinateNoteApi(RiftGunCoordinateNoteApi implementation) {
        if (coordinateNoteApiInstalled) {
            throw new IllegalStateException("Rift Gun coordinate-note API already installed");
        }
        coordinateNoteApi = java.util.Objects.requireNonNull(implementation, "implementation");
        coordinateNoteApiInstalled = true;
    }

    private RiftGunApi() {}
}
