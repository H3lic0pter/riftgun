package dev.riftgun.client.compat.journeymap;

import dev.riftgun.client.external.ClientMapWaypointIntegration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.CommonEventRegistry;

@JourneyMapPlugin(apiVersion = "2.0.0")
public final class RiftGunJourneyMapPlugin implements IClientPlugin {
    private static final Set<String> DEATH_IDS = ConcurrentHashMap.newKeySet();
    private static volatile IClientAPI api;
    @Override public void initialize(IClientAPI value) {
        api = value;
        CommonEventRegistry.DEATH_WAYPOINT_EVENT.subscribe(getModId(), event -> {
            DEATH_IDS.add(event.getWaypoint().getGuid());
            ClientMapWaypointIntegration.markJourneyMapDirty();
        });
        CommonEventRegistry.WAYPOINT_EVENT.subscribe(getModId(), event ->
            ClientMapWaypointIntegration.markJourneyMapDirty());
        CommonEventRegistry.WAYPOINT_GROUP_EVENT.subscribe(getModId(), event ->
            ClientMapWaypointIntegration.markJourneyMapDirty());
        ClientMapWaypointIntegration.markJourneyMapDirty();
    }
    @Override public String getModId() { return "riftgun"; }
    static IClientAPI api() { return api; }
    static boolean isDeathWaypoint(String id) { return DEATH_IDS.contains(id); }
}
