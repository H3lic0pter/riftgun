package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class ExternalDestinationSessionTest {
    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void retainsSelectionForTheLoginSessionUntilPlayerLeaves() {
        ExternalDestinationSession session = new ExternalDestinationSession();
        ExternalDestinationSelection selection = selection(
            ExternalDestinationSource.JOURNEYMAP, "village");

        session.select(PLAYER, selection);
        assertEquals(Optional.of(selection), session.selected(PLAYER));

        session.playerLeft(PLAYER);
        assertTrue(session.selected(PLAYER).isEmpty());
    }

    @Test
    void refreshClearsASelectionWhoseStableIdDisappeared() {
        ExternalDestinationSession session = new ExternalDestinationSession();
        session.select(PLAYER, selection(ExternalDestinationSource.XAERO_MINIMAP, "old"));

        session.retain(PLAYER, ExternalDestinationSource.XAERO_MINIMAP, Set.of("new"));

        assertTrue(session.selected(PLAYER).isEmpty());
    }

    @Test
    void refreshOfAnotherSourceDoesNotClearSelection() {
        ExternalDestinationSession session = new ExternalDestinationSession();
        ExternalDestinationSelection selection = selection(
            ExternalDestinationSource.JOURNEYMAP, "home");
        session.select(PLAYER, selection);

        session.retain(PLAYER, ExternalDestinationSource.XAERO_MINIMAP, Set.of());

        assertEquals(Optional.of(selection), session.selected(PLAYER));
    }

    @Test
    void disablingSourceClearsItsSelectionOnly() {
        ExternalDestinationSession session = new ExternalDestinationSession();
        session.select(PLAYER, selection(ExternalDestinationSource.JOURNEYMAP, "home"));

        session.clearSource(PLAYER, ExternalDestinationSource.JOURNEYMAP);

        assertTrue(session.selected(PLAYER).isEmpty());
    }

    private static ExternalDestinationSelection selection(
        ExternalDestinationSource source,
        String stableId
    ) {
        return new ExternalDestinationSelection(source, stableId, "Village", "minecraft:overworld",
            123.5, 64, -88.5);
    }
}
