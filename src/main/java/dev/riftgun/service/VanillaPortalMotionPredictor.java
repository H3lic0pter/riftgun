package dev.riftgun.service;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Short-horizon, movement-state-aware approximation for front portal placement. */
public final class VanillaPortalMotionPredictor implements PortalMotionPredictor {
    static final double DEFAULT_GRAVITY = 0.08;
    static final double SLOW_FALLING_GRAVITY = 0.01;
    static final double VERTICAL_DRAG = 0.98;
    static final double ELYTRA_HORIZONTAL_DRAG = 0.99;

    @Override
    public Vec3 predictDisplacement(ServerPlayer player, Purpose purpose, int ticks,
                                    double maximumHorizontalDisplacement) {
        int horizon = Math.max(0, ticks
            + PortalServices.PLACEMENT_CAPABILITIES.motionPredictionCalibrationTicks(player));
        Vec3 instantaneous = player.getDeltaMovement();
        Vec3 observed = PortalServices.MOTION_HISTORY.recentVelocity(player).orElse(instantaneous);

        if (player.getAbilities().flying) {
            return linear(observed, true, horizon, maximumHorizontalDisplacement);
        }
        if (player.isFallFlying()) {
            return elytra(instantaneous, player.getLookAngle(), player.getXRot(),
                player.getGravity(), horizon, maximumHorizontalDisplacement);
        }
        if (controlledMovement(player)) {
            return linear(observed, true, horizon, maximumHorizontalDisplacement);
        }
        if (player.onGround()) {
            return linear(observed, false, horizon, maximumHorizontalDisplacement);
        }

        MobEffectInstance levitation = player.getEffect(MobEffects.LEVITATION);
        return ordinaryAirborne(observed, instantaneous.y, player.getGravity(),
            player.hasEffect(MobEffects.SLOW_FALLING), levitation == null ? -1 : levitation.getAmplifier(), purpose,
            horizon, maximumHorizontalDisplacement);
    }

    private static boolean controlledMovement(ServerPlayer player) {
        return player.isPassenger() || player.onClimbable() || player.isSwimming()
            || player.isInWaterOrBubble() || player.isInLava() || player.isInFluidType();
    }

    static Vec3 ordinaryAirborne(Vec3 observedVelocity, double verticalVelocity, double gravity,
                                boolean slowFalling, int levitationAmplifier, Purpose purpose,
                                int ticks, double maximumHorizontalDisplacement) {
        if (purpose == Purpose.FRONT && levitationAmplifier < 0) {
            return linear(observedVelocity, false, ticks, maximumHorizontalDisplacement);
        }
        return ballistic(observedVelocity, verticalVelocity, gravity, slowFalling,
            levitationAmplifier, ticks, maximumHorizontalDisplacement);
    }

    static Vec3 linear(Vec3 velocity, boolean includeVertical, int ticks,
                       double maximumHorizontalDisplacement) {
        int horizon = Math.max(0, ticks);
        Vec3 displacement = new Vec3(velocity.x * horizon,
            includeVertical ? velocity.y * horizon : 0.0, velocity.z * horizon);
        return capHorizontal(displacement, maximumHorizontalDisplacement);
    }

    static Vec3 ballistic(Vec3 observedVelocity, double verticalVelocity, double gravity,
                          boolean slowFalling, int levitationAmplifier, int ticks,
                          double maximumHorizontalDisplacement) {
        int horizon = Math.max(0, ticks);
        double vertical = 0.0;
        double velocityY = verticalVelocity;
        for (int tick = 0; tick < horizon; tick++) {
            vertical += velocityY;
            if (levitationAmplifier >= 0) {
                double target = 0.05 * (levitationAmplifier + 1);
                velocityY += (target - velocityY) * 0.2;
            } else {
                double effectiveGravity = slowFalling && velocityY <= 0.0
                    ? Math.min(gravity, SLOW_FALLING_GRAVITY) : gravity;
                velocityY -= effectiveGravity;
            }
            velocityY *= VERTICAL_DRAG;
        }
        return capHorizontal(new Vec3(observedVelocity.x * horizon, vertical,
            observedVelocity.z * horizon), maximumHorizontalDisplacement);
    }

    static Vec3 elytra(Vec3 initialVelocity, Vec3 look, float pitchDegrees, double gravity,
                       int ticks, double maximumHorizontalDisplacement) {
        Vec3 velocity = initialVelocity;
        Vec3 displacement = Vec3.ZERO;
        double pitch = Math.toRadians(pitchDegrees);
        double horizontalLook = Math.hypot(look.x, look.z);
        double lookLength = look.length();
        double lift = Math.cos(pitch);
        lift = lift * lift * Math.min(1.0, lookLength / 0.4);

        for (int tick = 0; tick < Math.max(0, ticks); tick++) {
            double horizontalSpeed = velocity.horizontalDistance();
            velocity = velocity.add(0.0, gravity * (-1.0 + lift * 0.75), 0.0);
            if (velocity.y < 0.0 && horizontalLook > 0.0) {
                double glideLift = velocity.y * -0.1 * lift;
                velocity = velocity.add(look.x * glideLift / horizontalLook, glideLift,
                    look.z * glideLift / horizontalLook);
            }
            if (pitch < 0.0 && horizontalLook > 0.0) {
                double climb = horizontalSpeed * -Math.sin(pitch) * 0.04;
                velocity = velocity.add(-look.x * climb / horizontalLook, climb * 3.2,
                    -look.z * climb / horizontalLook);
            }
            if (horizontalLook > 0.0) {
                velocity = velocity.add(
                    (look.x / horizontalLook * horizontalSpeed - velocity.x) * 0.1,
                    0.0,
                    (look.z / horizontalLook * horizontalSpeed - velocity.z) * 0.1);
            }
            velocity = velocity.multiply(ELYTRA_HORIZONTAL_DRAG, VERTICAL_DRAG,
                ELYTRA_HORIZONTAL_DRAG);
            displacement = displacement.add(velocity);
        }
        return capHorizontal(displacement, maximumHorizontalDisplacement);
    }

    private static Vec3 capHorizontal(Vec3 displacement, double maximumHorizontalDisplacement) {
        double maximum = Math.max(0.0, maximumHorizontalDisplacement);
        double horizontal = Math.hypot(displacement.x, displacement.z);
        if (maximum == 0.0) return new Vec3(0.0, displacement.y, 0.0);
        if (horizontal <= maximum) return displacement;
        double scale = maximum / horizontal;
        return new Vec3(displacement.x * scale, displacement.y, displacement.z * scale);
    }
}
