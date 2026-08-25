package dev.riftgun.api;

/** Public coordinate-note capability owned and implemented by Rift Gun. */
@FunctionalInterface
public interface RiftGunCoordinateNoteApi {
    default RiftGunApiVersion version() {
        return RiftGunApiVersion.CURRENT;
    }

    CoordinateNoteResult create(CoordinateNoteRequest request);
}
