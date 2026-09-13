package dev.riftgun.appearance.client;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkinSoundMemoryTest {
    private static final SkinSoundMemory.Sounds CUSTOM = sounds("none", "ender");
    private static final SkinSoundMemory.Sounds RIFT = sounds("rift", "rift");
    private static final SkinSoundMemory.Sounds APERTURE = sounds("aperture_ish", "rift");

    @Test
    void multipleSkinChangesAndRestartKeepTheOriginalCustomSounds() {
        var first = SkinSoundMemory.apply(List.of(), "world-a|player", CUSTOM, ignored -> RIFT);
        var second = SkinSoundMemory.apply(List.copyOf(first.memory()), "world-a|player", first.sounds(), ignored -> APERTURE);
        var disabled = SkinSoundMemory.apply(List.copyOf(second.memory()), "world-a|player", second.sounds(), UnaryOperator.identity());
        assertEquals(CUSTOM, disabled.sounds());
        assertEquals(1, disabled.memory().size());
    }

    @Test
    void customPresetRestoresCustomWithoutLosingItForTheNextSkin() {
        var first = SkinSoundMemory.apply(List.of(), "a", CUSTOM, ignored -> RIFT);
        var staff = SkinSoundMemory.apply(first.memory(), "a", RIFT, UnaryOperator.identity());
        assertEquals(CUSTOM, staff.sounds());
        var aperture = SkinSoundMemory.apply(staff.memory(), "a", staff.sounds(), ignored -> APERTURE);
        assertEquals(CUSTOM, SkinSoundMemory.custom(aperture.memory(), "a", APERTURE));
    }

    @Test
    void differentWorldsAndPlayersHaveIndependentBackups() {
        var first = SkinSoundMemory.apply(List.of(), "a|alice", CUSTOM, ignored -> RIFT);
        var second = SkinSoundMemory.apply(first.memory(), "b|alice", APERTURE, ignored -> RIFT);
        var third = SkinSoundMemory.apply(second.memory(), "a|bob", APERTURE, ignored -> RIFT);
        assertEquals(CUSTOM, SkinSoundMemory.custom(third.memory(), "a|alice", RIFT));
        assertEquals(APERTURE, SkinSoundMemory.custom(third.memory(), "b|alice", RIFT));
        assertEquals(APERTURE, SkinSoundMemory.custom(third.memory(), "a|bob", RIFT));
    }

    @Test
    void serverValuesNeverSilentlyReplaceAnExistingCustomBackup() {
        var first = SkinSoundMemory.apply(List.of(), "a", CUSTOM, ignored -> RIFT);
        var next = SkinSoundMemory.apply(first.memory(), "a", APERTURE, UnaryOperator.identity());
        assertEquals(CUSTOM, next.sounds());
    }

    @Test
    void disconnectAfterServerApplicationButBeforeAcknowledgementKeepsTheOriginalBackup() {
        var captured = SkinSoundMemory.captureCustom(List.of(), "a", CUSTOM);
        // The replacement reached the server, but no confirmation reached the client before restart.
        var afterReconnect = SkinSoundMemory.captureCustom(captured, "a", APERTURE);
        assertEquals(CUSTOM, SkinSoundMemory.custom(afterReconnect, "a", APERTURE));
    }

    @Test
    void explicitEditsUseCustomChannelsInsteadOfCapturingRecommendedChannels() {
        var first = SkinSoundMemory.apply(List.of(), "a", CUSTOM, ignored -> RIFT);
        var custom = SkinSoundMemory.custom(first.memory(), "a", RIFT);
        var edited = new SkinSoundMemory.Sounds("riftgun:aperture_ish", custom.portal(), custom.transit(), custom.splash());
        var memory = SkinSoundMemory.rememberCustom(first.memory(), "a", edited);
        var next = SkinSoundMemory.apply(memory, "a", edited, ignored -> RIFT);
        assertEquals(edited, SkinSoundMemory.custom(next.memory(), "a", RIFT));
        assertEquals("riftgun:ender", edited.transit());
    }

    @Test
    void legacyAppliedFieldDoesNotPreventRestoringCustomSounds() {
        String legacy = """
            {"scope":"a","custom":{"shot":"riftgun:none","portal":"riftgun:rift",
            "transit":"riftgun:ender","splash":true},"applied":{"shot":"riftgun:rift",
            "portal":"riftgun:rift","transit":"riftgun:rift","splash":true}}
            """;
        var restored = SkinSoundMemory.apply(List.of(legacy), "a", RIFT, UnaryOperator.identity());
        assertEquals(CUSTOM, restored.sounds());
        assertEquals(CUSTOM, SkinSoundMemory.custom(restored.memory(), "a", RIFT));
        assertFalse(restored.memory().getFirst().contains("\"applied\""));
    }

    @Test
    void malformedMemoryFallsBackToTheCurrentSettings() {
        var next = SkinSoundMemory.apply(List.of("broken", "null", "{}"), "a", CUSTOM, UnaryOperator.identity());
        assertEquals(CUSTOM, next.sounds());
        assertEquals(1, next.memory().size());
    }

    private static SkinSoundMemory.Sounds sounds(String shot, String transit) {
        return new SkinSoundMemory.Sounds("riftgun:" + shot, "riftgun:rift", "riftgun:" + transit, true);
    }
}
