package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.riftgun.core.visual.MagicCircleAnimation;
import dev.riftgun.portal.PortalLifecycle.Phase;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class MagicCircleAnimationTest {
    @Test
    void chargingShowsAllFiveOpaqueFramesWhileOnlyTheOuterRingFadesIn() {
        var frames = new HashSet<Integer>();
        for (int tick = 0; tick <= 60; tick++) {
            var frame = MagicCircleAnimation.frame(Phase.CHARGING, tick / 10.0F, 0.0F);
            frames.add(frame.triangle());
            assertEquals(1.0F, frame.alpha());
            assertTrue(frame.scale() >= 0.9F && frame.scale() <= 1.0F);
            assertEquals(0.0F, frame.inner1Scale());
            assertEquals(0.0F, frame.inner2Scale());
        }
        assertEquals(java.util.Set.of(0, 1, 2, 3, 4), frames);
        assertEquals(0.0F, MagicCircleAnimation.frame(Phase.CHARGING, 0, 0).outerAlpha());
        assertEquals(1.0F, MagicCircleAnimation.frame(Phase.CHARGING, 6, 0).outerAlpha());
        assertEquals(4, MagicCircleAnimation.frame(Phase.OPEN, 0, 1).triangle());
    }

    @Test
    void innerRingsGrowInOverlappingOrderWithoutShrinkingTheOuterLayers() {
        var first = MagicCircleAnimation.frame(Phase.OPENING, 0, 0.2F);
        assertTrue(first.inner1Scale() > 0);
        assertEquals(0.0F, first.inner2Scale());
        var overlap = MagicCircleAnimation.frame(Phase.OPENING, 0, 0.5F);
        assertTrue(overlap.inner1Scale() < 1);
        assertTrue(overlap.inner2Scale() > 0);
        var last = MagicCircleAnimation.frame(Phase.OPENING, 0, 0.8F);
        assertEquals(1.0F, last.inner1Scale());
        assertTrue(last.inner2Scale() < 1);
        assertEquals(1.0F, last.scale());
        assertEquals(1.0F, MagicCircleAnimation.frame(Phase.OPENING, 0, 1).inner2Scale());
    }

    @Test
    void closingEnlargesEveryLayerWhileFadingOut() {
        var start = MagicCircleAnimation.frame(Phase.CLOSING, 0, 1);
        var middle = MagicCircleAnimation.frame(Phase.CLOSING, 0, 0.5F);
        var end = MagicCircleAnimation.frame(Phase.CLOSING, 0, 0);
        assertEquals(1.0F, start.scale());
        assertEquals(1.1F, middle.scale(), 1.0E-6);
        assertEquals(0.5F, middle.alpha());
        assertEquals(middle.alpha(), middle.outerAlpha());
        assertEquals(1.0F, middle.inner1Scale());
        assertEquals(1.0F, middle.inner2Scale());
        assertEquals(1.2F, end.scale());
        assertEquals(0.0F, end.alpha());
    }

    @Test
    void defaultsRotateInOppositeDirectionsWithFasterInnerRings() {
        float outer = MagicCircleAnimation.rotation(100, 20, false);
        float inner = MagicCircleAnimation.rotation(100, 15, true);
        assertTrue(outer > 0 && inner < 0);
        assertEquals(20.0 / 15.0, -inner / outer, 1.0E-6);
        assertEquals(-outer, MagicCircleAnimation.rotation(100, 20, true));
        assertEquals(0.0F, MagicCircleAnimation.rotation(400, 20, false));
    }
}
