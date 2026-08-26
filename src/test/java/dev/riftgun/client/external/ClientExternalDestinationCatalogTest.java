package dev.riftgun.client.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.external.ExternalDestinationSource;
import dev.riftgun.external.client.ClientExternalDestinationCatalog;
import dev.riftgun.external.client.ExternalDestination;
import dev.riftgun.external.client.ExternalDestinationReadResult;
import dev.riftgun.external.client.ExternalWaypoint;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ClientExternalDestinationCatalogTest {
    @Test
    void filtersUnsupportedWaypointsAndSortsTheVisibleSnapshot() {
        ClientExternalDestinationCatalog catalog = new ClientExternalDestinationCatalog();

        catalog.replace(ExternalDestinationReadResult.available(
            ExternalDestinationSource.JOURNEYMAP,
            "2.0.0",
            List.of(
                waypoint("enabled-b", "Beta", "Set B", "minecraft:overworld", 4, 70, 8,
                    true, true, false),
                waypoint("disabled", "Disabled", "Set A", "minecraft:overworld", 0, 64, 0,
                    false, true, false),
                waypoint("temporary", "Temporary", "Set A", "minecraft:overworld", 0, 64, 0,
                    true, false, false),
                waypoint("death", "Death", "Set A", "minecraft:overworld", 0, 64, 0,
                    true, true, true),
                waypoint("enabled-a", "Alpha", "Set A", "minecraft:the_nether", 1, 65, 2,
                    true, true, false)
            )),
            Set.of("minecraft:overworld", "minecraft:the_nether"),
            100);

        assertEquals(List.of("enabled-a", "enabled-b"), catalog.destinations(
            ExternalDestinationSource.JOURNEYMAP).stream()
            .map(ExternalDestination::stableId)
            .toList());
    }

    @Test
    void unknownDimensionsRemainVisibleButCannotBeSelected() {
        ClientExternalDestinationCatalog catalog = new ClientExternalDestinationCatalog();
        catalog.replace(ExternalDestinationReadResult.available(
            ExternalDestinationSource.XAERO_MINIMAP,
            "26.4.2",
            List.of(waypoint("modded", "Moon", "Space", "example:moon", 1, 2, 3,
                true, true, false))),
            Set.of("minecraft:overworld"),
            100);

        ExternalDestination destination = catalog.destinations(
            ExternalDestinationSource.XAERO_MINIMAP).getFirst();
        assertEquals(ExternalDestination.Availability.UNKNOWN_DIMENSION,
            destination.availability());
        assertFalse(destination.selectable());
        assertTrue(catalog.isGroupVisible(ExternalDestinationSource.XAERO_MINIMAP));
    }

    @Test
    void emptyAndIncompatibleSourcesHideTheirGroups() {
        ClientExternalDestinationCatalog catalog = new ClientExternalDestinationCatalog();
        catalog.replace(ExternalDestinationReadResult.available(
            ExternalDestinationSource.JOURNEYMAP, "2.0.0", List.of()), Set.of(), 100);
        catalog.replace(new ExternalDestinationReadResult(
            ExternalDestinationSource.XAERO_MINIMAP,
            ExternalDestinationReadResult.Status.INCOMPATIBLE,
            "27.0.0",
            "Unsupported Xaero version",
            List.of()), Set.of(), 100);

        assertFalse(catalog.isGroupVisible(ExternalDestinationSource.JOURNEYMAP));
        assertFalse(catalog.isGroupVisible(ExternalDestinationSource.XAERO_MINIMAP));
        assertEquals("27.0.0", catalog.readResult(
            ExternalDestinationSource.XAERO_MINIMAP).installedVersion());
    }

    @Test
    void clampsConfiguredLimitToOneThroughOneThousand() {
        ClientExternalDestinationCatalog catalog = new ClientExternalDestinationCatalog();
        List<ExternalWaypoint> waypoints = java.util.stream.IntStream.range(0, 1001)
            .mapToObj(index -> waypoint("id-" + index, "Name " + index, "Set",
                "minecraft:overworld", index, 64, 0, true, true, false))
            .toList();

        catalog.replace(ExternalDestinationReadResult.available(
            ExternalDestinationSource.JOURNEYMAP, "2.0.0", waypoints),
            Set.of("minecraft:overworld"), 5000);
        assertEquals(1000, catalog.destinations(ExternalDestinationSource.JOURNEYMAP).size());

        catalog.replace(ExternalDestinationReadResult.available(
            ExternalDestinationSource.JOURNEYMAP, "2.0.0", waypoints),
            Set.of("minecraft:overworld"), 0);
        assertEquals(1, catalog.destinations(ExternalDestinationSource.JOURNEYMAP).size());
    }

    private static ExternalWaypoint waypoint(
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
    ) {
        return new ExternalWaypoint(stableId, name, sourceGroup, dimensionId, x, y, z,
            enabled, persistent, deathpoint);
    }
}
