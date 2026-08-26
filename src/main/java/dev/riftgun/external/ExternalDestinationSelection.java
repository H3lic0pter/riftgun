package dev.riftgun.external;

import java.util.Objects;

/** Client-supplied external destination retained only for the current login session. */
public record ExternalDestinationSelection(
    ExternalDestinationSource source,
    String stableId,
    String name,
    String dimensionId,
    double x,
    double y,
    double z
) {
    public ExternalDestinationSelection {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(stableId, "stableId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dimensionId, "dimensionId");
    }
}
