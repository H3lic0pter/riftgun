package dev.riftgun.external.client;

import dev.riftgun.external.ExternalDestinationSource;
import java.util.List;

/** One adapter read, including a compatibility status for the settings screen. */
public record ExternalDestinationReadResult(
    ExternalDestinationSource source,
    Status status,
    String installedVersion,
    String detail,
    List<ExternalWaypoint> waypoints
) {
    public ExternalDestinationReadResult {
        waypoints = List.copyOf(waypoints);
    }

    public static ExternalDestinationReadResult available(
        ExternalDestinationSource source,
        String installedVersion,
        List<ExternalWaypoint> waypoints
    ) {
        return new ExternalDestinationReadResult(source, Status.AVAILABLE, installedVersion, "",
            waypoints);
    }

    public enum Status {
        AVAILABLE,
        INCOMPATIBLE,
        READ_FAILED
    }
}
