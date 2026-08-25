package dev.riftgun.api;

/** Public high-level portal capability owned by Rift Gun. */
@FunctionalInterface
public interface RiftGunPortalApi {
    default RiftGunApiVersion version() {
        return RiftGunApiVersion.CURRENT;
    }

    PortalOpenResult openPortal(PortalOpenRequest request);
}
