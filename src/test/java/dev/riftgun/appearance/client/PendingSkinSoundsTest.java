package dev.riftgun.appearance.client;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PendingSkinSoundsTest {
    private static final String SCOPE = "server|player";
    private static final SkinSoundMemory.Sounds CUSTOM =
        new SkinSoundMemory.Sounds("riftgun:none", "riftgun:none", "riftgun:ender", true);
    private static final SkinSoundMemory.Sounds RECOMMENDED =
        new SkinSoundMemory.Sounds("riftgun:aperture_ish", "riftgun:rift", "riftgun:rift", true);

    @Test
    void oldSnapshotsDuringRapidOnOffDoNotReplaceTheCustomBackup() {
        var edits = new PendingSkinSounds();
        var on = SkinSoundMemory.apply(List.of(), SCOPE, CUSTOM, ignored -> RECOMMENDED);
        edits.submit("on", on);
        var off = SkinSoundMemory.apply(edits.memory(List.of()), SCOPE, edits.visible(CUSTOM), UnaryOperator.identity());
        edits.submit("off", off);
        assertEquals(CUSTOM, edits.visible(RECOMMENDED)); // Delayed full snapshot from the first request.
        var onAck = edits.acknowledge("on", true, RECOMMENDED).orElseThrow();
        assertEquals(CUSTOM, onAck.visible());
        var onAgain = SkinSoundMemory.apply(edits.memory(onAck.memory()), SCOPE,
            edits.visible(RECOMMENDED), ignored -> RECOMMENDED);
        edits.submit("on-again", onAgain);
        var offAck = edits.acknowledge("off", true, CUSTOM).orElseThrow();
        assertEquals(RECOMMENDED, offAck.visible());
        var offAgain = SkinSoundMemory.apply(edits.memory(offAck.memory()), SCOPE,
            edits.visible(CUSTOM), UnaryOperator.identity());
        edits.submit("off-again", offAgain);
        assertEquals(CUSTOM, edits.acknowledge("on-again", true, RECOMMENDED).orElseThrow().visible());
        var finalAck = edits.acknowledge("off-again", true, CUSTOM).orElseThrow();
        assertEquals(CUSTOM, finalAck.visible());
        assertEquals(CUSTOM, SkinSoundMemory.custom(finalAck.memory(), SCOPE, CUSTOM));
        assertFalse(edits.isPending());
    }

    @Test
    void rejectionRollsBackTheVisibleSelectionWithoutCommittingABackup() {
        var edits = new PendingSkinSounds();
        edits.submit("failed", SkinSoundMemory.apply(List.of(), SCOPE, CUSTOM, ignored -> RECOMMENDED));
        var result = edits.acknowledge("failed", false, CUSTOM).orElseThrow();
        assertFalse(result.accepted());
        assertNull(result.memory());
        assertEquals(CUSTOM, result.visible());
        assertFalse(edits.isPending());
    }

    @Test
    void supersededRepliesCannotOverwriteANewerConfirmation() {
        var edits = new PendingSkinSounds();
        var on = SkinSoundMemory.apply(List.of(), SCOPE, CUSTOM, ignored -> RECOMMENDED);
        edits.submit("old", on);
        edits.submit("new", SkinSoundMemory.apply(on.memory(), SCOPE, RECOMMENDED, UnaryOperator.identity()));
        assertEquals(CUSTOM, edits.acknowledge("new", true, CUSTOM).orElseThrow().visible());
        assertTrue(edits.acknowledge("old", true, RECOMMENDED).isEmpty());
        assertTrue(edits.acknowledge("new", true, CUSTOM).isEmpty());
    }

    @Test
    void acceptedServerNormalizationKeepsTheOriginalCustomBackup() {
        var edits = new PendingSkinSounds();
        var on = SkinSoundMemory.apply(List.of(), SCOPE, CUSTOM, ignored -> RECOMMENDED);
        edits.submit("normalized", on);
        var normalized = new SkinSoundMemory.Sounds("riftgun:rift", "riftgun:rift", "riftgun:rift", true);
        var confirmed = edits.acknowledge("normalized", true, normalized).orElseThrow();
        assertEquals(normalized, confirmed.visible());
        assertEquals(CUSTOM, SkinSoundMemory.custom(confirmed.memory(), SCOPE, normalized));
    }

    @Test
    void manualEditWhileAPresetIsPendingBecomesTheNewCustomChoice() {
        var edits = new PendingSkinSounds();
        edits.submit("preset", SkinSoundMemory.apply(List.of(), SCOPE, CUSTOM, ignored -> RECOMMENDED));
        var manual = new SkinSoundMemory.Sounds("riftgun:rift", CUSTOM.portal(), CUSTOM.transit(), false);
        var proposal = SkinSoundMemory.rememberCustom(edits.memory(List.of()), SCOPE, manual);
        edits.submit("manual", new SkinSoundMemory.Change(proposal, manual));
        assertEquals(manual, edits.acknowledge("preset", true, RECOMMENDED).orElseThrow().visible());
        var confirmation = edits.acknowledge("manual", true, manual).orElseThrow();
        assertEquals(manual, SkinSoundMemory.custom(confirmation.memory(), SCOPE, manual));
    }

    @Test
    void disconnectDiscardsUnconfirmedEdits() {
        var edits = new PendingSkinSounds();
        edits.submit("old-server", SkinSoundMemory.apply(List.of(), SCOPE, CUSTOM, ignored -> RECOMMENDED));
        edits.clear();
        assertTrue(edits.acknowledge("old-server", true, RECOMMENDED).isEmpty());
        assertEquals(CUSTOM, edits.visible(CUSTOM));
    }
}
