package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class ProjectileMotionTest {
    @Test
    void rotationFollowsVelocity() {
        ProjectileMotion.Rotation forward = ProjectileMotion.rotationFor(
            new Vec3(0.0, 0.0, 2.0), 41.0F, -17.0F);
        assertEquals(0.0F, forward.yaw(), 1.0E-5F);
        assertEquals(0.0F, forward.pitch(), 1.0E-5F);

        ProjectileMotion.Rotation right = ProjectileMotion.rotationFor(
            new Vec3(2.0, 0.0, 0.0), 41.0F, -17.0F);
        assertEquals(90.0F, right.yaw(), 1.0E-5F);
        assertEquals(0.0F, right.pitch(), 1.0E-5F);

        ProjectileMotion.Rotation downward = ProjectileMotion.rotationFor(
            new Vec3(0.0, -3.0, 0.0), 41.0F, -17.0F);
        assertEquals(0.0F, downward.yaw(), 1.0E-5F);
        assertEquals(-90.0F, downward.pitch(), 1.0E-5F);
    }

    @Test
    void zeroVelocityPreservesFallbackRotation() {
        ProjectileMotion.Rotation rotation = ProjectileMotion.rotationFor(
            Vec3.ZERO, 41.0F, -17.0F);
        assertEquals(41.0F, rotation.yaw());
        assertEquals(-17.0F, rotation.pitch());
    }
}
