package dev.riftgun.service;

import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-owned, non-persistent external selection cache scoped to player login sessions. */
public final class ExternalDestinationSession {
    private final Map<UUID, ExternalDestinationSelection> selections = new ConcurrentHashMap<>();

    public void select(UUID playerId, ExternalDestinationSelection selection) {
        selections.put(playerId, selection);
    }

    public Optional<ExternalDestinationSelection> selected(UUID playerId) {
        return Optional.ofNullable(selections.get(playerId));
    }

    public void retain(
        UUID playerId,
        ExternalDestinationSource source,
        Set<String> currentStableIds
    ) {
        selections.computeIfPresent(playerId, (ignored, selected) ->
            selected.source() == source && !currentStableIds.contains(selected.stableId())
                ? null
                : selected);
    }

    public void clearSource(UUID playerId, ExternalDestinationSource source) {
        selections.computeIfPresent(playerId, (ignored, selected) ->
            selected.source() == source ? null : selected);
    }

    public void playerLeft(UUID playerId) {
        selections.remove(playerId);
    }

    public void clear() {
        selections.clear();
    }
}
