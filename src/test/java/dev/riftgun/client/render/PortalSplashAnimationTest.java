package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalLifecycle;
import org.junit.jupiter.api.Test;

final class PortalSplashAnimationTest {
    @Test
    void openingSplashHasVisibleAreaForTheWholeStartup() {
        for (int tick = 0; tick < PortalLifecycle.CHARGE_TICKS; tick++) {
            assertVisibleOpening(PortalSplashAnimation.sample(
                PortalLifecycle.Phase.CHARGING, tick, 0.5F));
        }
        for (int tick = 0; tick < PortalLifecycle.ANIMATION_TICKS; tick++) {
            assertVisibleOpening(PortalSplashAnimation.sample(
                PortalLifecycle.Phase.OPENING, tick, 0.5F));
        }
    }

    @Test
    void openPortalDoesNotContinuouslySplashButClosingDoes() {
        assertFalse(PortalSplashAnimation.sample(PortalLifecycle.Phase.OPEN, 20, 0.5F).visible());
        PortalSplashAnimation.Frame closing = PortalSplashAnimation.sample(
            PortalLifecycle.Phase.CLOSING, 2, 0.5F);
        assertTrue(closing.visible());
        assertFalse(closing.outward());
        assertTrue(closing.dropletLength() >= 0.14F);
    }

    private static void assertVisibleOpening(PortalSplashAnimation.Frame frame) {
        assertTrue(frame.visible());
        assertTrue(frame.outward());
        assertTrue(frame.alpha() >= 0.45F);
        assertTrue(frame.dropletLength() >= 0.18F);
        assertTrue(frame.droplets() >= 12);
    }
}
