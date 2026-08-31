package dev.riftgun.input;

/** Loader-neutral mouse contract shared by both radial screen adapters. */
public enum ModeRadialPointerAction {
    NONE,
    COMMIT_SELECTION,
    START_RANGE_DRAG,
    TOGGLE_FUNCTION,
    TOGGLE_FACE_FRAME,
    SWITCH_PAGE;

    public static ModeRadialPointerAction resolve(int button, boolean precisionPreviewOnly,
                                                  boolean overRangeSlider,
                                                  boolean surfaceFacePage,
                                                  boolean pairingInstalled) {
        if (button == 0 && precisionPreviewOnly) return COMMIT_SELECTION;
        if (button == 0 && overRangeSlider) return START_RANGE_DRAG;
        if (button == 0 && !surfaceFacePage && pairingInstalled) return TOGGLE_FUNCTION;
        if (button == 1 && surfaceFacePage) return TOGGLE_FACE_FRAME;
        if (button == 1 && !precisionPreviewOnly) return SWITCH_PAGE;
        return NONE;
    }
}
