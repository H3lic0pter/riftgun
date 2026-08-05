package dev.riftgun.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Short-horizon approximation used only to place a portal where the player will reach it. */
public final class VanillaPortalMotionPredictor implements PortalMotionPredictor {
    static final double GRAVITY = 0.08;
    static final double VERTICAL_DRAG = 0.98;

    @Override
    public Vec3 predictDisplacement(ServerPlayer player, int ticks, double maximumHorizontalDisplacement) {
        return predict(player.getDeltaMovement(), player.onGround(), ticks, maximumHorizontalDisplacement);
    }

    static Vec3 predict(Vec3 velocity, boolean onGround, int ticks, double maximumHorizontalDisplacement) {
        int horizon = Math.max(0, ticks);
        Vec3 horizontal = new Vec3(velocity.x * horizon, 0.0, velocity.z * horizon);
        double maximum = Math.max(0.0, maximumHorizontalDisplacement);
        if (horizontal.lengthSqr() > maximum * maximum && maximum > 0.0) {
            horizontal = horizontal.normalize().scale(maximum);
        } else if (maximum == 0.0) {
            horizontal = Vec3.ZERO;
        }

        double vertical = 0.0;
        if (!onGround) {
            double velocityY = velocity.y;
            for (int tick = 0; tick < horizon; tick++) {
                vertical += velocityY;
                velocityY = (velocityY - GRAVITY) * VERTICAL_DRAG;
            }
        }
        return new Vec3(horizontal.x, vertical, horizontal.z);
    }
}
