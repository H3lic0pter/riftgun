package dev.riftgun.external.client;

import dev.riftgun.external.ExternalDestinationSource;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owns normalized client-session snapshots; third-party types stop at adapter boundaries. */
public final class ClientExternalDestinationCatalog {
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAXIMUM_LIMIT = 1000;

    private static final Comparator<ExternalDestination> DISPLAY_ORDER = Comparator
        .comparing(ExternalDestination::sourceGroup, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(ExternalDestination::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(ExternalDestination::dimensionId)
        .thenComparingDouble(ExternalDestination::x)
        .thenComparingDouble(ExternalDestination::y)
        .thenComparingDouble(ExternalDestination::z)
        .thenComparing(ExternalDestination::stableId);

    private final Map<ExternalDestinationSource, List<ExternalDestination>> destinations =
        new EnumMap<>(ExternalDestinationSource.class);
    private final Map<ExternalDestinationSource, ExternalDestinationReadResult> readResults =
        new EnumMap<>(ExternalDestinationSource.class);

    public void replace(
        ExternalDestinationReadResult result,
        Set<String> availableDimensions,
        int configuredLimit
    ) {
        readResults.put(result.source(), result);
        if (result.status() != ExternalDestinationReadResult.Status.AVAILABLE) {
            destinations.remove(result.source());
            return;
        }

        int limit = Math.clamp(configuredLimit, 1, MAXIMUM_LIMIT);
        List<ExternalDestination> normalized = result.waypoints().stream()
            .filter(ExternalWaypoint::enabled)
            .filter(ExternalWaypoint::persistent)
            .filter(waypoint -> !waypoint.deathpoint())
            .map(waypoint -> normalize(result.source(), waypoint, availableDimensions))
            .sorted(DISPLAY_ORDER)
            .limit(limit)
            .toList();
        if (normalized.isEmpty()) destinations.remove(result.source());
        else destinations.put(result.source(), normalized);
    }

    public List<ExternalDestination> destinations(ExternalDestinationSource source) {
        return destinations.getOrDefault(source, List.of());
    }

    public boolean isGroupVisible(ExternalDestinationSource source) {
        return !destinations(source).isEmpty();
    }

    public ExternalDestinationReadResult readResult(ExternalDestinationSource source) {
        return readResults.get(source);
    }

    public void clear() {
        destinations.clear();
        readResults.clear();
    }

    public void clear(ExternalDestinationSource source) {
        destinations.remove(source);
        readResults.remove(source);
    }

    private static ExternalDestination normalize(
        ExternalDestinationSource source,
        ExternalWaypoint waypoint,
        Set<String> availableDimensions
    ) {
        String dimensionId = safe(waypoint.dimensionId());
        ExternalDestination.Availability availability = !dimensionId.isBlank()
            && availableDimensions.contains(dimensionId)
            ? ExternalDestination.Availability.AVAILABLE
            : ExternalDestination.Availability.UNKNOWN_DIMENSION;
        return new ExternalDestination(source, safe(waypoint.stableId()), safe(waypoint.name()),
            safe(waypoint.sourceGroup()), dimensionId, waypoint.x(), waypoint.y(), waypoint.z(),
            availability);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
