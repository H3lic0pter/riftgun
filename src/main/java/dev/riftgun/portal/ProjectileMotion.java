package dev.riftgun.portal;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Keeps projectile render rotation consistent with the velocity produced by portal transit. */
public final class ProjectileMotion {
    private static final double MINIMUM_DIRECTION_LENGTH_SQUARED = 1.0E-12;

    public static void alignToVelocity(Projectile projectile, Vec3 velocity) {
        Rotation rotation = rotationFor(velocity, projectile.getYRot(), projectile.getXRot());
        projectile.setYRot(rotation.yaw());
        projectile.setXRot(rotation.pitch());
        projectile.yRotO = rotation.yaw();
        projectile.xRotO = rotation.pitch();
    }

    public static Rotation rotationFor(Vec3 velocity, float fallbackYaw, float fallbackPitch) {
        if (velocity.lengthSqr() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            return new Rotation(fallbackYaw, fallbackPitch);
        }
        double horizontal = velocity.horizontalDistance();
        float yaw = (float) Math.toDegrees(Mth.atan2(velocity.x, velocity.z));
        float pitch = (float) Math.toDegrees(Mth.atan2(velocity.y, horizontal));
        return new Rotation(yaw, pitch);
    }

    public record Rotation(float yaw, float pitch) {}

    private ProjectileMotion() {}
}
