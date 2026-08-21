package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwirlPortalVisualRendererTest {
    private static final float EPSILON = 1.0E-4F;
    private static final float TAU = (float) (Math.PI * 2.0);

    @Test
    void completesOneRotationAtTheConfiguredPeriod() {
        // One full turn (period * 20 ticks) wraps back to the starting angle.
        assertEquals(0.0F, SwirlPortalVisualRenderer.swirlRotation(0.0F, 20.0F, 0.0F), EPSILON);
        assertEquals(0.0F, SwirlPortalVisualRenderer.swirlRotation(20.0F * 20.0F, 20.0F, 0.0F), EPSILON);
    }

    @Test
    void phaseAddsAFixedOffsetToTheStartingAngle() {
        // age 0 -> only the phase offset survives, scaled onto the 0..TAU circle.
        float offset = SwirlPortalVisualRenderer.swirlRotation(0.0F, 20.0F, 0.75F);
        assertEquals(0.75F * TAU, offset, EPSILON);
    }

    @Test
    void rotationStaysBoundedWithinOneTurn() {
        for (float ticks = 0.0F; ticks < 2000.0F; ticks += 61.0F) {
            float angle = SwirlPortalVisualRenderer.swirlRotation(ticks, 7.0F, 0.5F);
            assertEquals(true, angle >= 0.0F && angle < TAU,
                "angle out of range at ticks=" + ticks);
        }
    }

    @Test
    void rotationEncodingWrapsIntoTheUnsignedLightmapRange() {
        assertEquals(0, SwirlPortalVisualRenderer.encodeRotation(0.0F));
        assertEquals(0, SwirlPortalVisualRenderer.encodeRotation(TAU));
        assertEquals(32767, SwirlPortalVisualRenderer.encodeRotation(TAU * 0.5F));

        int wrapped = SwirlPortalVisualRenderer.encodeRotation(-0.5F);
        assertEquals(true, wrapped >= 0 && wrapped < 65536, "encoded out of range: " + wrapped);
    }

    @Test
    void shaderFallbackLeavesFourPixelsOfRotationMargin() {
        assertEquals(-0.0319149F, SwirlPortalVisualRenderer.fallbackTextureUv(0.0F), EPSILON);
        assertEquals(0.5F, SwirlPortalVisualRenderer.fallbackTextureUv(0.5F), EPSILON);
        assertEquals(1.0319149F, SwirlPortalVisualRenderer.fallbackTextureUv(1.0F), EPSILON);
    }
}
