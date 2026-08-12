package dev.riftgun.relocation;

import dev.riftgun.portal.PortalOrientation;
import net.minecraft.world.phys.Vec3;

/** Exit-motion policy for projectiles captured without physically crossing an entrance face. */
final class EntityRelocationProjectileMotion {
    static final double MINIMUM_EXIT_SPEED = 0.12;

    static Vec3 exitVelocity(Vec3 incoming, PortalOrientation exitOrientation, float exitYaw) {
        double speed = Math.max(MINIMUM_EXIT_SPEED, incoming.length());
        return exitOrientation.normal(exitYaw).scale(speed);
    }

    private EntityRelocationProjectileMotion() {}
}
