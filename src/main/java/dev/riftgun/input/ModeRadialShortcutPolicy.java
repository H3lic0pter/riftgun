package dev.riftgun.input;

/** Pure edge-routing policy for shortcuts that interact with the open mode radial. */
public final class ModeRadialShortcutPolicy {
    public enum EdgeAction { NONE, COMMIT_PAIRING, TOGGLE_FUNCTION }

    public static EdgeAction pairingOperation(boolean radialOpen, boolean down,
                                              boolean wasDown) {
        return radialOpen && down && !wasDown ? EdgeAction.COMMIT_PAIRING : EdgeAction.NONE;
    }

    public static EdgeAction alternateCycle(boolean radialOpen, boolean cycleDown,
                                            boolean cycleWasDown, boolean altDown) {
        return !radialOpen && cycleDown && !cycleWasDown && altDown
            ? EdgeAction.TOGGLE_FUNCTION : EdgeAction.NONE;
    }

    private ModeRadialShortcutPolicy() {}
}
