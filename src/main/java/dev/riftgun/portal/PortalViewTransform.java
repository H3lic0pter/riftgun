package dev.riftgun.portal;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Player-camera transform kept separate from the physical momentum transform. */
public final class PortalViewTransform {
    private static final double HORIZONTAL_EPSILON_SQUARED = 1.0E-12;

    public static Rotation playerRotation(Vec3 inputLook, float inputYaw, float inputPitch,
                                          PortalOrientation sourceOrientation, float sourceYaw,
                                          PortalOrientation targetOrientation, float targetYaw,
                                          float facingDot, float facingThreshold) {
        if (sourceOrientation != PortalOrientation.VERTICAL
            || targetOrientation != PortalOrientation.VERTICAL) {
            return new Rotation(inputYaw, inputPitch);
        }

        Vec3 look = PortalTransform.between(inputLook,
            sourceOrientation, sourceYaw, targetOrientation, targetYaw).normalize();
        if (facingDot > 0.0F) {
            float blend = Mth.clamp(facingDot / facingThreshold, 0.0F, 1.0F);
            Vec3 mirrored = PortalTransform.betweenFactors(inputLook,
                sourceOrientation, sourceYaw, targetOrientation, targetYaw,
                -1.0F, 1.0F).normalize();
            Vec3 flipped = PortalTransform.betweenFactors(inputLook,
                sourceOrientation, sourceYaw, targetOrientation, targetYaw,
                -1.0F, -1.0F).normalize();
            look = mirrored.lerp(flipped, blend).normalize();
        }
        return rotationFor(look, inputYaw);
    }

    public static Rotation rotationFor(Vec3 look, float fallbackYaw) {
        Vec3 normalized = look.normalize();
        double horizontalSquared = normalized.x * normalized.x + normalized.z * normalized.z;
        float yaw = horizontalSquared <= HORIZONTAL_EPSILON_SQUARED
            ? fallbackYaw
            : (float) Math.toDegrees(Math.atan2(-normalized.x, normalized.z));
        float pitch = (float) Math.toDegrees(
            Math.asin(Mth.clamp(-normalized.y, -1.0, 1.0)));
        return new Rotation(yaw, pitch);
    }

    public record Rotation(float yaw, float pitch) {}

    private PortalViewTransform() {}
}
