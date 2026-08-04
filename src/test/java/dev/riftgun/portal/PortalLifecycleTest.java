package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.service.FixedOpenDurationClosePolicy;
import org.junit.jupiter.api.Test;

final class PortalLifecycleTest {
    @Test
    void traversesChargingOpeningOpenAndClosingPhases() {
        PortalLifecycle.Step step = new PortalLifecycle.Step(PortalLifecycle.Phase.CHARGING, 0);
        for (int tick = 0; tick < PortalLifecycle.CHARGE_TICKS; tick++) {
            step = PortalLifecycle.tick(step.phase(), step.phaseTicks());
        }
        assertEquals(PortalLifecycle.Phase.OPENING, step.phase());

        for (int tick = 0; tick < PortalLifecycle.ANIMATION_TICKS; tick++) {
            step = PortalLifecycle.tick(step.phase(), step.phaseTicks());
        }
        assertEquals(PortalLifecycle.Phase.OPEN, step.phase());

        step = new PortalLifecycle.Step(PortalLifecycle.Phase.CLOSING, 0);
        for (int tick = 0; tick < PortalLifecycle.ANIMATION_TICKS; tick++) {
            step = PortalLifecycle.tick(step.phase(), step.phaseTicks());
        }
        assertEquals(PortalLifecycle.Phase.CLOSED, step.phase());
    }

    @Test
    void visualProgressIsBounded() {
        assertEquals(0.0F, PortalLifecycle.visibleProgress(PortalLifecycle.Phase.OPENING, -10, 0));
        assertEquals(1.0F, PortalLifecycle.visibleProgress(PortalLifecycle.Phase.OPENING, 99, 0));
        assertEquals(1.0F, PortalLifecycle.visibleProgress(PortalLifecycle.Phase.OPEN, 0, 0));
    }

    @Test
    void fixedPolicyClosesAfterThreeFullSecondsOpen() {
        FixedOpenDurationClosePolicy policy = new FixedOpenDurationClosePolicy();
        assertFalse(policy.shouldClose(PortalLifecycle.Phase.OPEN,
            FixedOpenDurationClosePolicy.OPEN_TICKS - 1));
        assertTrue(policy.shouldClose(PortalLifecycle.Phase.OPEN,
            FixedOpenDurationClosePolicy.OPEN_TICKS));
        assertFalse(policy.shouldClose(PortalLifecycle.Phase.OPENING, 999));
    }
}
