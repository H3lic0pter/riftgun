package dev.riftgun.ui;

import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.external.ExternalDestinationSource;
import dev.riftgun.external.client.ExternalDestination;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.riftgun.ui.PortalConfigRows.RowKind.DESTINATION;
import static dev.riftgun.ui.PortalConfigRows.RowKind.EXTERNAL_DESTINATION;
import static dev.riftgun.ui.PortalConfigRows.RowKind.PLAYER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalConfigRowsTest {
    @Test
    void pinnedDestinationsSortBeforeRecentEntries() {
        PortalPlayerData data = new PortalPlayerData();
        UUID olderPinned = UUID.randomUUID();
        UUID recent = UUID.randomUUID();
        data.destinations().add(destination(olderPinned, "Pinned", 1, true));
        data.destinations().add(destination(recent, "Recent", 20, false));

        var result = build(data, "", List.of(), emptyPlayers());
        List<UUID> destinations = result.rows().stream()
            .filter(row -> row.kind() == DESTINATION).map(PortalConfigRows.Row::id).toList();
        assertEquals(List.of(olderPinned, recent), destinations);
    }

    @Test
    void queryExpandsMatchingExternalAndPlayerRows() {
        PortalPlayerData data = new PortalPlayerData();
        ExternalDestination external = new ExternalDestination(
            ExternalDestinationSource.JOURNEYMAP, "home", "Moon Base", "Bases",
            "minecraft:overworld", 1, 2, 3, ExternalDestination.Availability.AVAILABLE);
        var externalSection = new PortalConfigRows.ExternalSection(
            ExternalDestinationSource.JOURNEYMAP, true, true, false, List.of(external));
        UUID playerId = UUID.randomUUID();
        var players = new PortalConfigRows.PlayerSection(true, false,
            List.of(new TestPlayerEntry(playerId, "Moonwalker", false, 0)));

        var result = build(data, "moon", List.of(externalSection), players);
        assertTrue(result.rows().stream().anyMatch(row -> row.kind() == EXTERNAL_DESTINATION));
        assertTrue(result.rows().stream().anyMatch(
            row -> row.kind() == PLAYER && row.id().equals(playerId)));
        assertEquals(external, result.externalRows().values().iterator().next());
    }

    @Test
    void customGroupsFollowDeclaredOrder() {
        PortalPlayerData data = new PortalPlayerData();
        UUID late = UUID.randomUUID();
        UUID early = UUID.randomUUID();
        data.groups().add(new DestinationGroup(late, "Late", 20));
        data.groups().add(new DestinationGroup(early, "Early", 1));

        var rows = build(data, "", List.of(), emptyPlayers()).rows();
        List<UUID> groups = rows.stream()
            .filter(row -> row.kind() == PortalConfigRows.RowKind.GROUP)
            .map(PortalConfigRows.Row::id).toList();
        assertEquals(List.of(PortalPlayerData.DEFAULT_GROUP_ID, early, late), groups);
    }

    private static BuiltRows build(
        PortalPlayerData data, String query,
        List<PortalConfigRows.ExternalSection> external,
        PortalConfigRows.PlayerSection players
    ) {
        Map<UUID, ExternalDestination> externalRows = new HashMap<>();
        List<PortalConfigRows.Row> rows = PortalConfigRows.build(data, query,
            id -> data.group(id).map(DestinationGroup::name).orElse("Default"),
            ignored -> 0.0, external, players, externalRows);
        return new BuiltRows(rows, externalRows);
    }

    private static PortalConfigRows.PlayerSection emptyPlayers() {
        return new PortalConfigRows.PlayerSection(false, false, List.of());
    }

    private static Destination destination(UUID id, String name, long lastUsedAt, boolean pinned) {
        // Row semantics do not inspect dimensions; avoid bootstrapping the 26.1.2 FML runtime here.
        return new Destination(id, name, PortalPlayerData.DEFAULT_GROUP_ID, null,
            0, 64, 0, 0, 0, lastUsedAt, pinned);
    }

    private record TestPlayerEntry(UUID id, String name, boolean pinned, int serverOrder)
        implements PortalConfigRows.PlayerEntryView {}

    private record BuiltRows(
        List<PortalConfigRows.Row> rows,
        Map<UUID, ExternalDestination> externalRows
    ) {}
}
