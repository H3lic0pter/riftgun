package dev.riftgun.client.compat.immersiveportal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ImmersivePortalDirtyStateTest {
    @Test
    void onlyMarksChangedSnapshotsForApplication() {
        ImmersivePortalDirtyState<String> state = new ImmersivePortalDirtyState<>();

        assertTrue(state.update("first"));
        assertFalse(state.update("first"));
        assertTrue(state.update("second"));
        assertFalse(state.update("second"));
    }
}
