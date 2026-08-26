package dev.riftgun.external.client;

import dev.riftgun.external.ExternalDestinationSource;

/** A normalized, read-only destination suitable for display in RiftGun's GUI. */
public record ExternalDestination(
    ExternalDestinationSource source,
    String stableId,
    String name,
    String sourceGroup,
    String dimensionId,
    double x,
    double y,
    double z,
    Availability availability
) {
    public boolean selectable() {
        return availability == Availability.AVAILABLE;
    }

    public enum Availability {
        AVAILABLE,
        UNKNOWN_DIMENSION
    }
}
