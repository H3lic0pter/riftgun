package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class RecentMotionWindowTest {
    @Test
    void estimateWeightsNewestMovementMostHeavily() {
        RecentMotionWindow history = new RecentMotionWindow();
        history.record(Level.OVERWORLD, new Vec3(0.0, 64.0, 0.0), 10, 8.0);
        history.record(Level.OVERWORLD, new Vec3(0.1, 64.0, 0.0), 11, 8.0);
        history.record(Level.OVERWORLD, new Vec3(0.3, 64.0, 0.0), 12, 8.0);
        history.record(Level.OVERWORLD, new Vec3(0.6, 64.0, 0.0), 13, 8.0);

        Vec3 velocity = history.estimatedVelocity().orElseThrow();
        assertEquals(1.4 / 6.0, velocity.x, 1.0E-8);
        assertEquals(0.0, velocity.y, 1.0E-8);
    }

    @Test
    void sameTickReplacesSampleInsteadOfInventingVelocity() {
        RecentMotionWindow history = new RecentMotionWindow();
        history.record(Level.OVERWORLD, Vec3.ZERO, 10, 8.0);
        history.record(Level.OVERWORLD, new Vec3(0.25, 0.0, 0.0), 10, 8.0);

        assertEquals(1, history.size());
        assertTrue(history.estimatedVelocity().isEmpty());
    }

    @Test
    void teleportAndDimensionChangeDiscardOldTrajectory() {
        RecentMotionWindow history = new RecentMotionWindow();
        history.record(Level.OVERWORLD, Vec3.ZERO, 10, 8.0);
        history.record(Level.OVERWORLD, new Vec3(0.2, 0.0, 0.0), 11, 8.0);
        history.record(Level.OVERWORLD, new Vec3(20.0, 0.0, 0.0), 12, 8.0);

        assertEquals(1, history.size());
        assertTrue(history.estimatedVelocity().isEmpty());

        history.record(Level.NETHER, new Vec3(20.1, 0.0, 0.0), 13, 8.0);
        assertEquals(1, history.size());
        assertTrue(history.estimatedVelocity().isEmpty());
    }
}
