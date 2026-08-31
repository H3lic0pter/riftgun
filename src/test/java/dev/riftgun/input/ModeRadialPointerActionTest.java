package dev.riftgun.input;

import static dev.riftgun.input.ModeRadialPointerAction.COMMIT_SELECTION;
import static dev.riftgun.input.ModeRadialPointerAction.NONE;
import static dev.riftgun.input.ModeRadialPointerAction.START_RANGE_DRAG;
import static dev.riftgun.input.ModeRadialPointerAction.SWITCH_PAGE;
import static dev.riftgun.input.ModeRadialPointerAction.TOGGLE_FACE_FRAME;
import static dev.riftgun.input.ModeRadialPointerAction.TOGGLE_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ModeRadialPointerActionTest {
    @Test
    void resolvesTheSharedLeftAndRightClickContract() {
        assertEquals(COMMIT_SELECTION, resolve(0, true, false, false, true));
        assertEquals(START_RANGE_DRAG, resolve(0, false, true, false, true));
        assertEquals(TOGGLE_FUNCTION, resolve(0, false, false, false, true));
        assertEquals(TOGGLE_FACE_FRAME, resolve(1, true, false, true, true));
        assertEquals(SWITCH_PAGE, resolve(1, false, false, false, false));
        assertEquals(NONE, resolve(0, false, false, true, true));
        assertEquals(NONE, resolve(2, false, false, false, true));
    }

    private static ModeRadialPointerAction resolve(int button, boolean precision,
                                                    boolean overRangeSlider,
                                                    boolean surfacePage,
                                                    boolean pairingInstalled) {
        return ModeRadialPointerAction.resolve(button, precision, overRangeSlider,
            surfacePage, pairingInstalled);
    }
}
