package dev.riftgun.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ModeRadialShortcutPolicyTest {
    @Test
    void pairingOperationCommitsOnlyOnThePressEdgeInsideTheRadial() {
        assertEquals(ModeRadialShortcutPolicy.EdgeAction.COMMIT_PAIRING,
            ModeRadialShortcutPolicy.pairingOperation(true, true, false));
        assertEquals(ModeRadialShortcutPolicy.EdgeAction.NONE,
            ModeRadialShortcutPolicy.pairingOperation(true, true, true));
        assertEquals(ModeRadialShortcutPolicy.EdgeAction.NONE,
            ModeRadialShortcutPolicy.pairingOperation(false, true, false));
    }

    @Test
    void alternateCycleTogglesFunctionOnlyOnTheOutsidePressEdge() {
        assertEquals(ModeRadialShortcutPolicy.EdgeAction.TOGGLE_FUNCTION,
            ModeRadialShortcutPolicy.alternateCycle(false, true, false, true));
        assertEquals(ModeRadialShortcutPolicy.EdgeAction.NONE,
            ModeRadialShortcutPolicy.alternateCycle(false, true, true, true));
        assertEquals(ModeRadialShortcutPolicy.EdgeAction.NONE,
            ModeRadialShortcutPolicy.alternateCycle(true, true, false, true));
    }
}
