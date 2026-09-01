package dev.riftgun.client.screen;

import net.minecraft.client.Minecraft;
import dev.riftgun.client.PlayerListState;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.ui.PortalPlayerTargetSession;
import dev.riftgun.ui.PortalPlayerTargetSession.Command;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Client-side state and network commands for the expandable Player Target section. */
final class PlayerTargetController {
    private final PortalPlayerTargetSession session;

    PlayerTargetController(PortalPlayerData data) {
        session = new PortalPlayerTargetSession(data.selectedPlayerId(),
            data.expandedGroups().contains(PortalPlayerData.PLAYER_SECTION_ID));
    }

    @Nullable UUID selectedId() {
        return session.selectedId();
    }

    boolean expanded() {
        return session.expanded();
    }

    boolean visible() {
        return PortalClientState.gun().moduleCount(PortalModuleKind.PLAYER_TARGET) > 0
            && PortalClientState.gun().playerTargetEnabled();
    }

    void requestListIfNeeded() {
        execute(session.requestListIfNeeded(visible(),
            Minecraft.getInstance().getConnection() != null));
    }

    void requestList() {
        execute(session.requestList());
    }

    void select(UUID id) {
        execute(session.select(id));
    }

    void clearSelection() {
        session.clearSelection();
    }

    void togglePin(UUID id, boolean pinned) {
        execute(session.togglePin(id, pinned));
    }

    void toggleExpanded() {
        session.toggleExpanded().forEach(this::execute);
    }

    void openSelected() {
        execute(session.openSelected());
    }

    void sync(PortalPlayerData data) {
        session.sync(data.selectedPlayerId(),
            data.expandedGroups().contains(PortalPlayerData.PLAYER_SECTION_ID));
    }

    boolean clearUnavailableSelection() {
        return session.clearUnavailableSelection(id -> PlayerListState.player(id) != null);
    }

    private void execute(@Nullable Command command) {
        if (command == null) return;
        switch (command.type()) {
            case REQUEST_LIST -> PortalNetworking.sendRequest(PortalAction.REQUEST_PLAYERS);
            case SELECT -> PortalNetworking.sendRequest(PortalAction.SELECT_PLAYER,
                tag -> tag.putUUID("Target", command.id()));
            case SET_PINNED -> {
                PlayerListState.markPinned(command.id(), command.value());
                PortalNetworking.sendRequest(PortalAction.TOGGLE_PLAYER_PIN,
                    tag -> tag.putUUID("Target", command.id()));
            }
            case SET_EXPANDED -> PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
                tag.putUUID("Group", PortalPlayerData.PLAYER_SECTION_ID);
                tag.putBoolean("Expanded", command.value());
            });
            case OPEN -> PortalNetworking.sendRequest(PortalAction.OPEN_PLAYER_PORTAL,
                tag -> tag.putUUID("Target", command.id()));
        }
    }
}
