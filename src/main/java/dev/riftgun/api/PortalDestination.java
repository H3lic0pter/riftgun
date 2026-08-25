package dev.riftgun.api;

import java.util.Objects;

/** Immutable destination resolved by an integrating mod before portal creation. */
public record PortalDestination(
    RiftResourceId dimensionId,
    double x,
    double y,
    double z,
    float yaw
) {
    public PortalDestination {
        Objects.requireNonNull(dimensionId, "dimensionId");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw)) {
            throw new IllegalArgumentException("Portal destination pose must be finite");
        }
    }
}
