package dev.riftgun.client.screen;

import dev.riftgun.client.PlayerListState;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Client-side state and network commands for the expandable Player Target section. */
final class PlayerTargetController {
    private @Nullable UUID selectedId;
    private boolean expanded;
    private boolean listRequested;

    PlayerTargetController(PortalPlayerData data) {
        sync(data);
    }

    @Nullable UUID selectedId() {
        return selectedId;
    }

    boolean expanded() {
        return expanded;
    }

    boolean visible() {
        return PortalClientState.gun().getCompound("Modules").getInt("PLAYER_TARGET") > 0
            && PortalClientState.gun().getBoolean("PlayerTargetEnabled");
    }

    List<PlayerListState.PlayerEntry> entries(String normalizedQuery) {
        List<PlayerListState.PlayerEntry> entries = PlayerListState.players().stream()
            .filter(entry -> normalizedQuery.isEmpty()
                || entry.name().toLowerCase(Locale.ROOT).contains(normalizedQuery))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        entries.sort(Comparator.comparing(PlayerListState.PlayerEntry::pinned).reversed()
            .thenComparingInt(PlayerListState.PlayerEntry::serverOrder));
        return entries;
    }

    void requestListIfNeeded() {
        if (!visible() || listRequested) return;
        listRequested = true;
        requestList();
    }

    void requestList() {
        PortalNetworking.sendRequest(PortalAction.REQUEST_PLAYERS);
    }

    void select(UUID id) {
        selectedId = id;
        PortalNetworking.sendRequest(PortalAction.SELECT_PLAYER, tag -> tag.putUUID("Target", id));
    }

    void clearSelection() {
        selectedId = null;
    }

    void togglePin(UUID id, boolean pinned) {
        PlayerListState.markPinned(id, !pinned);
        PortalNetworking.sendRequest(PortalAction.TOGGLE_PLAYER_PIN, tag -> tag.putUUID("Target", id));
    }

    void toggleExpanded() {
        expanded = !expanded;
        if (expanded) requestList();
        PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
            tag.putUUID("Group", PortalPlayerData.PLAYER_SECTION_ID);
            tag.putBoolean("Expanded", expanded);
        });
    }

    void openSelected() {
        if (selectedId == null) return;
        PortalNetworking.sendRequest(PortalAction.OPEN_PLAYER_PORTAL,
            tag -> tag.putUUID("Target", selectedId));
    }

    void sync(PortalPlayerData data) {
        selectedId = data.selectedPlayerId();
        expanded = data.expandedGroups().contains(PortalPlayerData.PLAYER_SECTION_ID);
    }

    boolean clearUnavailableSelection() {
        if (selectedId == null || PlayerListState.player(selectedId) != null) return false;
        selectedId = null;
        return true;
    }
}
