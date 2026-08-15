package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalLifecycle;
import org.junit.jupiter.api.Test;

final class PortalSplashPatternTest {
    @Test
    void emitsDuringBothTransitionsButNotWhileStable() {
        assertTrue(PortalSplashPattern.particleCount(PortalLifecycle.Phase.CHARGING) > 0);
        assertTrue(PortalSplashPattern.particleCount(PortalLifecycle.Phase.OPENING)
            > PortalSplashPattern.particleCount(PortalLifecycle.Phase.CLOSING));
        assertTrue(PortalSplashPattern.particleCount(PortalLifecycle.Phase.CLOSING) > 0);
        assertEquals(0, PortalSplashPattern.particleCount(PortalLifecycle.Phase.OPEN));
        assertEquals(0, PortalSplashPattern.particleCount(PortalLifecycle.Phase.CLOSED));
    }

    @Test
    void edgeTracksExpansionAndContraction() {
        float earlyOpening = PortalSplashPattern.edgeScale(PortalLifecycle.Phase.OPENING, 0);
        float lateOpening = PortalSplashPattern.edgeScale(
            PortalLifecycle.Phase.OPENING, PortalLifecycle.ANIMATION_TICKS - 1);
        float earlyClosing = PortalSplashPattern.edgeScale(PortalLifecycle.Phase.CLOSING, 0);
        float lateClosing = PortalSplashPattern.edgeScale(
            PortalLifecycle.Phase.CLOSING, PortalLifecycle.ANIMATION_TICKS - 1);

        assertTrue(earlyOpening < lateOpening);
        assertTrue(earlyClosing > lateClosing);
    }

    @Test
    void samplesRectanglePerimeterForVerticalAndHorizontalGeometry() {
        assertOnEdge(1.0F, 2.0F, 0.75F, 0.13);
        assertOnEdge(1.0F, 1.0F, 0.75F, 0.61);
    }

    private static void assertOnEdge(float width, float height, float scale, double sample) {
        PortalSplashPattern.EdgePoint point = PortalSplashPattern.sampleEdge(width, height, scale, sample);
        double halfWidth = width * scale * 0.5;
        double halfHeight = height * scale * 0.5;
        boolean onVerticalEdge = Math.abs(Math.abs(point.right()) - halfWidth) < 1.0E-9;
        boolean onHorizontalEdge = Math.abs(Math.abs(point.up()) - halfHeight) < 1.0E-9;
        assertTrue(onVerticalEdge || onHorizontalEdge);
        assertTrue(Math.abs(point.right()) <= halfWidth + 1.0E-9);
        assertTrue(Math.abs(point.up()) <= halfHeight + 1.0E-9);
    }
}
