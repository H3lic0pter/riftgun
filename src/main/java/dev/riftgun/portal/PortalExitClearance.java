package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;

/** Places a projectile so its complete collision bounds begin beyond the exit plane. */
final class PortalExitClearance {
    static final double EPSILON = 0.002;

    static Vec3 projectilePosition(PortalPlacement exit, double width, double height) {
        width = Math.max(0.0, width);
        height = Math.max(0.0, height);
        double planeClearance = PortalPlacement.DEPTH * 0.5 + EPSILON;
        double offset = switch (exit.orientation()) {
            case VERTICAL -> planeClearance + width * 0.5;
            case TOP -> planeClearance;
            case BOTTOM -> planeClearance + height;
        };
        return exit.center().add(exit.normal().scale(offset));
    }

    private PortalExitClearance() {}
}
