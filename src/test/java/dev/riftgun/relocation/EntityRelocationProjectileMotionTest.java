package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.riftgun.portal.PortalOrientation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class EntityRelocationProjectileMotionTest {
    @Test
    void bottomExitRedirectsFullSpeedDownward() {
        Vec3 result = EntityRelocationProjectileMotion.exitVelocity(
            new Vec3(3.0, 0.0, 4.0), PortalOrientation.BOTTOM, 90.0F);

        assertEquals(new Vec3(0.0, -5.0, 0.0), result);
    }

    @Test
    void nearlyStationaryProjectileReceivesMinimumExitSpeed() {
        Vec3 result = EntityRelocationProjectileMotion.exitVelocity(
            Vec3.ZERO, PortalOrientation.BOTTOM, 0.0F);

        assertEquals(0.0, result.x, 1.0E-8);
        assertEquals(-EntityRelocationProjectileMotion.MINIMUM_EXIT_SPEED, result.y, 1.0E-8);
        assertEquals(0.0, result.z, 1.0E-8);
    }
}
