package dev.riftgun.client.compat.journeymap;

import dev.riftgun.external.client.ClientExternalDestinationAdapter;
import dev.riftgun.external.client.ExternalDestinationReadResult;
import dev.riftgun.external.client.ExternalWaypoint;
import dev.riftgun.external.ExternalDestinationSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.common.waypoint.Waypoint;

public final class JourneyMapExternalDestinationAdapter implements ClientExternalDestinationAdapter {
    @Override public ExternalDestinationSource source() { return ExternalDestinationSource.JOURNEYMAP; }
    @Override public ExternalDestinationReadResult read(String version) {
        IClientAPI api = RiftGunJourneyMapPlugin.api();
        if (api == null) return new ExternalDestinationReadResult(source(),
            ExternalDestinationReadResult.Status.READ_FAILED, version, "API not initialized", List.of());
        Map<String, String> groups = api.getAllWaypointGroups().stream().collect(Collectors.toMap(
            group -> group.getGuid(), group -> group.getName(), (first, ignored) -> first));
        List<ExternalWaypoint> waypoints = api.getAllWaypoints().stream()
            .map(waypoint -> map(waypoint, groups)).toList();
        return ExternalDestinationReadResult.available(source(), version, waypoints);
    }
    private static ExternalWaypoint map(Waypoint waypoint, Map<String, String> groups) {
        return new ExternalWaypoint(waypoint.getGuid(), waypoint.getName(),
            groups.getOrDefault(waypoint.getGroupId(), ""), waypoint.getPrimaryDimension(),
            waypoint.getX(), waypoint.getY(), waypoint.getZ(), waypoint.isEnabled(),
            waypoint.isPersistent(), RiftGunJourneyMapPlugin.isDeathWaypoint(waypoint.getGuid()));
    }
}
