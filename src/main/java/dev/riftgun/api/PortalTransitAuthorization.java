package dev.riftgun.api;

import java.util.Objects;

/**
 * Opaque creation-time authorization attached to a Rift Gun-owned portal pair.
 * The authority that issued it decides how to interpret the snapshot.
 */
public record PortalTransitAuthorization(
    RiftResourceId authorityId,
    RiftResourceId destinationDimension
) {
    public PortalTransitAuthorization {
        Objects.requireNonNull(authorityId, "authorityId");
        Objects.requireNonNull(destinationDimension, "destinationDimension");
    }
}
