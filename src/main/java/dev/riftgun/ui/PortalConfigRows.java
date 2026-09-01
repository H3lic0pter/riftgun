package dev.riftgun.ui;

import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.external.ExternalDestinationSource;
import dev.riftgun.external.client.ExternalDestination;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/** Builds one semantic row list from server, map-integration, and player snapshots. */
public final class PortalConfigRows {
    public static List<Row> build(PortalPlayerData data, String query,
                                  Function<UUID, String> groupName,
                                  ToDoubleFunction<Destination> distance,
                                  List<ExternalSection> externalSections,
                                  PlayerSection playerSection,
                                  Map<UUID, ExternalDestination> externalRows) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        externalRows.clear();
        for (UUID groupId : orderedGroupIds(data)) {
            String name = groupName.apply(groupId);
            List<Destination> destinations = data.destinations().stream()
                .filter(destination -> destination.groupId().equals(groupId))
                .filter(destination -> matches(destination, name, normalized))
                .sorted(destinationComparator(data.settings().sort(), distance)).toList();
            boolean groupMatch = normalized.isEmpty()
                || name.toLowerCase(Locale.ROOT).contains(normalized);
            if (!groupMatch && destinations.isEmpty()) continue;
            rows.add(new Row(RowKind.GROUP, groupId, 0));
            if (data.expandedGroups().contains(groupId) || !normalized.isEmpty()) {
                destinations.forEach(destination ->
                    rows.add(new Row(RowKind.DESTINATION, destination.id(), 0)));
            }
        }
        for (ExternalSection section : externalSections) {
            addExternalRows(rows, externalRows, section, normalized);
        }
        addPlayerRows(rows, playerSection, normalized);
        return rows;
    }

    private static void addExternalRows(List<Row> rows,
                                        Map<UUID, ExternalDestination> externalRows,
                                        ExternalSection section, String normalized) {
        if (!section.enabled() || !section.visible()) return;
        List<ExternalDestination> matches = section.destinations().stream()
            .filter(destination -> matchesExternal(destination, normalized)).toList();
        boolean groupMatch = normalized.isEmpty()
            || section.source().displayName().toLowerCase(Locale.ROOT).contains(normalized);
        if (!groupMatch && matches.isEmpty()) return;
        rows.add(new Row(RowKind.EXTERNAL_GROUP, externalGroupId(section.source()), 0));
        if (section.expanded() || !normalized.isEmpty()) {
            for (ExternalDestination destination : matches) {
                UUID rowId = externalRowId(section.source(), destination.stableId());
                externalRows.put(rowId, destination);
                rows.add(new Row(RowKind.EXTERNAL_DESTINATION, rowId, 0));
            }
        }
    }

    private static void addPlayerRows(List<Row> rows, PlayerSection section, String normalized) {
        if (!section.visible()) return;
        List<PlayerEntryView> entries = new ArrayList<>(section.entries());
        entries.removeIf(entry -> !normalized.isEmpty()
            && !entry.name().toLowerCase(Locale.ROOT).contains(normalized));
        entries.sort(Comparator.comparing(PlayerEntryView::pinned).reversed()
            .thenComparingInt(PlayerEntryView::serverOrder));
        boolean sectionMatch = normalized.isEmpty() || "player".contains(normalized);
        if (!sectionMatch && entries.isEmpty()) return;
        rows.add(new Row(RowKind.PLAYER_SECTION, PortalPlayerData.PLAYER_SECTION_ID, 0));
        if (section.expanded() || !normalized.isEmpty()) {
            entries.forEach(entry -> rows.add(new Row(RowKind.PLAYER, entry.id(), 0)));
        }
    }

    public static List<UUID> orderedGroupIds(PortalPlayerData data) {
        List<UUID> ids = new ArrayList<>();
        ids.add(PortalPlayerData.DEFAULT_GROUP_ID);
        data.groups().stream().sorted(Comparator.comparingInt(DestinationGroup::order))
            .map(DestinationGroup::id).forEach(ids::add);
        if (data.destinations().stream()
            .anyMatch(destination ->
                destination.groupId().equals(PortalPlayerData.SHARED_SECTION_ID))) {
            ids.add(PortalPlayerData.SHARED_SECTION_ID);
        }
        return ids;
    }

    private static Comparator<Destination> destinationComparator(
        DestinationSort sort, ToDoubleFunction<Destination> distance
    ) {
        Comparator<Destination> pinned = Comparator.comparing(Destination::pinned).reversed();
        Comparator<Destination> secondary = switch (sort) {
            case RECENT -> Comparator.comparingLong(Destination::lastUsedAt).reversed();
            case NAME -> Comparator.comparing(value -> value.name().toLowerCase(Locale.ROOT));
            case CREATED -> Comparator.comparingLong(Destination::createdAt).reversed();
            case DISTANCE -> Comparator.comparingDouble(distance);
        };
        return pinned.thenComparing(secondary).thenComparing(Destination::id);
    }

    private static boolean matches(Destination destination, String group, String normalized) {
        if (normalized.isEmpty() || group.toLowerCase(Locale.ROOT).contains(normalized)
            || destination.name().toLowerCase(Locale.ROOT).contains(normalized)) return true;
        return String.format(Locale.ROOT, "%s %s %s",
            destination.x(), destination.y(), destination.z()).contains(normalized);
    }

    private static boolean matchesExternal(ExternalDestination destination, String normalized) {
        return normalized.isEmpty()
            || destination.name().toLowerCase(Locale.ROOT).contains(normalized)
            || destination.sourceGroup().toLowerCase(Locale.ROOT).contains(normalized)
            || destination.dimensionId().toLowerCase(Locale.ROOT).contains(normalized)
            || String.format(Locale.ROOT, "%s %s %s",
                destination.x(), destination.y(), destination.z()).contains(normalized);
    }

    public static UUID externalGroupId(ExternalDestinationSource source) {
        return UUID.nameUUIDFromBytes(("riftgun:external-group:" + source.name())
            .getBytes(StandardCharsets.UTF_8));
    }

    public static UUID externalSectionId(ExternalDestinationSource source) {
        return source == ExternalDestinationSource.JOURNEYMAP
            ? PortalPlayerData.JOURNEYMAP_SECTION_ID : PortalPlayerData.XAERO_MINIMAP_SECTION_ID;
    }

    public static UUID externalRowId(ExternalDestinationSource source, String stableId) {
        return UUID.nameUUIDFromBytes(("riftgun:external:" + source.name() + ':' + stableId)
            .getBytes(StandardCharsets.UTF_8));
    }

    public static ExternalDestinationSource externalSource(UUID groupId) {
        for (ExternalDestinationSource source : ExternalDestinationSource.values()) {
            if (externalGroupId(source).equals(groupId)) return source;
        }
        return null;
    }

    public enum RowKind {
        GROUP, DESTINATION, EXTERNAL_GROUP, EXTERNAL_DESTINATION, PLAYER_SECTION, PLAYER
    }

    public record Row(RowKind kind, UUID id, int y) {}

    public record ExternalSection(
        ExternalDestinationSource source,
        boolean enabled,
        boolean visible,
        boolean expanded,
        List<ExternalDestination> destinations
    ) {}

    public record PlayerSection(
        boolean visible, boolean expanded, List<? extends PlayerEntryView> entries
    ) {}

    public interface PlayerEntryView {
        UUID id();
        String name();
        boolean pinned();
        int serverOrder();
    }

    private PortalConfigRows() {}
}
