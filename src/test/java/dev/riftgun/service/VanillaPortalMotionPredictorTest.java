package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class VanillaPortalMotionPredictorTest {
    @Test
    void groundedPredictionUsesOnlyHorizontalVelocity() {
        Vec3 result = VanillaPortalMotionPredictor.predict(new Vec3(0.2, -0.4, 0.1), true, 10, 16.0);

        assertEquals(2.0, result.x, 1.0E-8);
        assertEquals(0.0, result.y, 1.0E-8);
        assertEquals(1.0, result.z, 1.0E-8);
    }

    @Test
    void airbornePredictionAcceleratesDownward() {
        Vec3 result = VanillaPortalMotionPredictor.predict(Vec3.ZERO, false, 11, 16.0);

        assertTrue(result.y < -3.5);
    }

    @Test
    void horizontalPredictionIsCappedWithoutCappingFallDistance() {
        Vec3 result = VanillaPortalMotionPredictor.predict(new Vec3(10.0, -4.0, 0.0), false, 11, 16.0);

        assertEquals(16.0, Math.hypot(result.x, result.z), 1.0E-8);
        assertTrue(result.y < -40.0);
    }

    @Test
    void controlledLinearPredictionDoesNotInventGravity() {
        Vec3 result = VanillaPortalMotionPredictor.linear(new Vec3(0.25, 0.0, -0.1), true, 11, 16.0);

        assertEquals(2.75, result.x, 1.0E-8);
        assertEquals(0.0, result.y, 1.0E-8);
        assertEquals(-1.1, result.z, 1.0E-8);
    }

    @Test
    void slowFallingDescendsLessThanOrdinaryGravity() {
        Vec3 normal = VanillaPortalMotionPredictor.ballistic(Vec3.ZERO, 0.0, 0.08,
            false, -1, 11, 16.0);
        Vec3 slow = VanillaPortalMotionPredictor.ballistic(Vec3.ZERO, 0.0, 0.08,
            true, -1, 11, 16.0);

        assertTrue(slow.y > normal.y);
        assertTrue(slow.y > -1.0);
    }

    @Test
    void levitationConvergesTowardEffectTarget() {
        Vec3 result = VanillaPortalMotionPredictor.ballistic(Vec3.ZERO, 0.0, 0.08,
            false, 1, 11, 16.0);

        assertTrue(result.y > 0.4);
    }

    @Test
    void elytraUsesGlidePhysicsInsteadOfOrdinaryFallPhysics() {
        Vec3 look = new Vec3(0.0, 0.0, 1.0);
        Vec3 glide = VanillaPortalMotionPredictor.elytra(new Vec3(0.0, 0.0, 0.8),
            look, 0.0F, 0.08, 11, 16.0);
        Vec3 fall = VanillaPortalMotionPredictor.ballistic(new Vec3(0.0, 0.0, 0.8),
            0.0, 0.08, false, -1, 11, 16.0);

        assertTrue(glide.z > 7.0);
        assertTrue(glide.y > fall.y);
    }

    @Test
    void downshotStartsAtConfiguredPitchBoundary() {
        assertTrue(VanillaPortalPlacementResolver.usesDownshot(78.0F, 78.0F));
        assertTrue(VanillaPortalPlacementResolver.usesDownshot(90.0F, 78.0F));
        assertFalse(VanillaPortalPlacementResolver.usesDownshot(77.99F, 78.0F));
    }
}
