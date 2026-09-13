package dev.riftgun.appearance.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Separates optimistic sound edits from confirmed backups, including delayed or superseded replies. */
public final class PendingSkinSounds {
    private record Edit(long sequence, SkinSoundMemory.Change change) {}
    public record Confirmation(List<String> memory, SkinSoundMemory.Sounds visible, boolean accepted) {}
    private final LinkedHashMap<String, Edit> pending = new LinkedHashMap<>();
    private long sequence;
    private long confirmedSequence;

    public boolean isPending() { return !pending.isEmpty(); }

    public List<? extends String> memory(List<? extends String> confirmed) {
        return pending.isEmpty() ? confirmed : pending.lastEntry().getValue().change().memory();
    }

    public SkinSoundMemory.Sounds visible(SkinSoundMemory.Sounds server) {
        return pending.isEmpty() ? server : pending.lastEntry().getValue().change().sounds();
    }

    public void submit(String requestId, SkinSoundMemory.Change change) {
        pending.put(requestId, new Edit(++sequence, change));
    }

    public Optional<Confirmation> acknowledge(String requestId, boolean accepted, SkinSoundMemory.Sounds server) {
        Edit edit = pending.remove(requestId);
        if (edit == null || edit.sequence() <= confirmedSequence) return Optional.empty();
        confirmedSequence = edit.sequence();
        pending.values().removeIf(older -> older.sequence() <= confirmedSequence);
        return Optional.of(new Confirmation(accepted ? edit.change().memory() : null, visible(server), accepted));
    }

    public void clear() {
        pending.clear();
        sequence = 0;
        confirmedSequence = 0;
    }
}
