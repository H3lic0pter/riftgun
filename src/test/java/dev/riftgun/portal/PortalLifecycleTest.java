package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.service.FixedOpenDurationClosePolicy;
import org.junit.jupiter.api.Test;

final class PortalLifecycleTest {
    @Test
    void traversesChargingOpeningOpenAndClosingPhases() {
        assertEquals(6, PortalLifecycle.CHARGE_TICKS);
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
        int duration = FixedOpenDurationClosePolicy.DEFAULT_OPEN_TICKS;
        assertFalse(policy.shouldClose(PortalLifecycle.Phase.OPEN,
            duration - 1, duration));
        assertTrue(policy.shouldClose(PortalLifecycle.Phase.OPEN,
            duration, duration));
        assertFalse(policy.shouldClose(PortalLifecycle.Phase.OPENING, 999, duration));
    }

    @Test
    void durationRulesClampWithoutRewritingDesiredValue() {
        assertEquals(3, PortalOpenDuration.effectiveSeconds(3, 15));
        assertEquals(15, PortalOpenDuration.effectiveSeconds(30, 15));
        assertEquals(1, PortalOpenDuration.effectiveSeconds(-4, 15));
        assertEquals(300, PortalOpenDuration.effectiveSeconds(999, 999));
        assertEquals(60, PortalOpenDuration.ticks(3));
    }
}
