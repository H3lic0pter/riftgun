package dev.riftgun.client.compat.xaero;

import dev.riftgun.external.client.ClientExternalDestinationAdapter;
import dev.riftgun.external.client.ExternalDestinationReadResult;
import dev.riftgun.external.client.ExternalWaypoint;
import dev.riftgun.external.ExternalDestinationSource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;
import xaero.hud.minimap.waypoint.set.WaypointSet;

public final class XaeroExternalDestinationAdapter implements ClientExternalDestinationAdapter {
    @Override
    public ExternalDestinationSource source() {
        return ExternalDestinationSource.XAERO_MINIMAP;
    }

    @Override
    public ExternalDestinationReadResult read(String installedVersion) {
        XaeroMinimapSession hudSession = XaeroMinimapSession.getCurrentSession();
        if (hudSession == null) return ExternalDestinationReadResult.available(
            source(), installedVersion, List.of());
        MinimapSession session = hudSession.getSession(BuiltInHudModules.MINIMAP);
        MinimapWorldRootContainer root = session == null ? null
            : session.getWorldManager().getCurrentRootContainer();
        if (root == null) return ExternalDestinationReadResult.available(
            source(), installedVersion, List.of());

        List<ExternalWaypoint> result = new ArrayList<>();
        for (MinimapWorld world : root.getAllWorldsIterable()) {
            String dimension = world.getDimId().identifier().toString();
            for (WaypointSet set : world.getIterableWaypointSets()) {
                for (Waypoint waypoint : set.getWaypoints()) {
                    result.add(map(dimension, set.getName(), waypoint));
                }
            }
        }
        return ExternalDestinationReadResult.available(source(), installedVersion, result);
    }

    private static ExternalWaypoint map(String dimension, String set, Waypoint waypoint) {
        String identity = dimension + '\n' + set + '\n' + waypoint.getName() + '\n'
            + waypoint.getX() + ',' + waypoint.getY() + ',' + waypoint.getZ();
        String stableId = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
        return new ExternalWaypoint(stableId, waypoint.getName(), set, dimension,
            waypoint.getX(), waypoint.getY(), waypoint.getZ(),
            !waypoint.isDisabled() && !waypoint.isEffectivelyDeleted(), !waypoint.isTemporary(),
            waypoint.getPurpose().isDeath());
    }
}
