package dev.riftgun.external.client;

/** Loader-neutral waypoint emitted by an optional map-mod adapter. */
public record ExternalWaypoint(
    String stableId,
    String name,
    String sourceGroup,
    String dimensionId,
    double x,
    double y,
    double z,
    boolean enabled,
    boolean persistent,
    boolean deathpoint
) {}
