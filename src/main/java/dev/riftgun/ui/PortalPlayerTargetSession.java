package dev.riftgun.ui;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/** Player-target selection state machine; adapters execute returned network commands. */
public final class PortalPlayerTargetSession {
    private @Nullable UUID selectedId;
    private boolean expanded;
    private boolean listRequested;

    public PortalPlayerTargetSession(@Nullable UUID selectedId, boolean expanded) {
        sync(selectedId, expanded);
    }

    public @Nullable UUID selectedId() {
        return selectedId;
    }

    public boolean expanded() {
        return expanded;
    }

    public @Nullable Command requestListIfNeeded(boolean visible, boolean connected) {
        if (!visible || !connected || listRequested) return null;
        listRequested = true;
        return new Command(CommandType.REQUEST_LIST, null, false);
    }

    public Command requestList() {
        listRequested = true;
        return new Command(CommandType.REQUEST_LIST, null, false);
    }

    public Command select(UUID id) {
        selectedId = id;
        return new Command(CommandType.SELECT, id, false);
    }

    public void clearSelection() {
        selectedId = null;
    }

    public Command togglePin(UUID id, boolean pinned) {
        return new Command(CommandType.SET_PINNED, id, !pinned);
    }

    public List<Command> toggleExpanded() {
        expanded = !expanded;
        List<Command> commands = new ArrayList<>();
        if (expanded) commands.add(requestList());
        commands.add(new Command(CommandType.SET_EXPANDED, null, expanded));
        return List.copyOf(commands);
    }

    public @Nullable Command openSelected() {
        return selectedId == null ? null
            : new Command(CommandType.OPEN, selectedId, false);
    }

    public void sync(@Nullable UUID nextSelectedId, boolean nextExpanded) {
        selectedId = nextSelectedId;
        expanded = nextExpanded;
    }

    public boolean clearUnavailableSelection(Predicate<UUID> available) {
        if (selectedId == null || available.test(selectedId)) return false;
        selectedId = null;
        return true;
    }

    public enum CommandType { REQUEST_LIST, SELECT, SET_PINNED, SET_EXPANDED, OPEN }

    public record Command(CommandType type, @Nullable UUID id, boolean value) {}
}
