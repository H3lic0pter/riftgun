package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SwirlFallbackAnimationTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void completesOneRotationAtTheConfiguredOuterPeriod() {
        SwirlFallbackAnimation.Uv start = SwirlFallbackAnimation.rotate(0.2F, 0.7F,
            0.0F, 20.0F, 0.0F, true);
        SwirlFallbackAnimation.Uv completed = SwirlFallbackAnimation.rotate(0.2F, 0.7F,
            20.0F * 20.0F, 20.0F, 0.0F, true);

        assertEquals(start.u(), completed.u(), EPSILON);
        assertEquals(start.v(), completed.v(), EPSILON);
    }

    @Test
    void disabledAnimationLeavesTextureCoordinatesUntouched() {
        SwirlFallbackAnimation.Uv uv = SwirlFallbackAnimation.rotate(0.2F, 0.7F,
            123.0F, 20.0F, 0.75F, false);

        assertEquals(0.2F, uv.u(), EPSILON);
        assertEquals(0.7F, uv.v(), EPSILON);
    }

    @Test
    void backFaceCompensatesForItsMirroredGeometry() {
        SwirlFallbackAnimation.Uv front = SwirlFallbackAnimation.rotate(1.0F, 0.5F,
            100.0F, 20.0F, 0.0F, true, 1.0F);
        SwirlFallbackAnimation.Uv back = SwirlFallbackAnimation.rotate(1.0F, 0.5F,
            100.0F, 20.0F, 0.0F, true, -1.0F);

        assertEquals(0.5F, front.u(), EPSILON);
        assertEquals(1.0F, front.v(), EPSILON);
        assertEquals(0.5F, back.u(), EPSILON);
        assertEquals(0.0F, back.v(), EPSILON);
    }
}
