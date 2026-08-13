package dev.riftgun.portal;

import java.util.Collection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Computes one root-level translation that clears every passenger bound past an exit. */
final class PortalTreeClearance {
    private static final double EXIT_MARGIN = 0.05;

    static double outwardCorrection(PortalPlacement exit, Collection<AABB> bounds,
                                    double horizontalTriggerExtend) {
        double result = 0.0;
        Vec3 normal = exit.normal();
        double required = normalHalfDepth(exit, horizontalTriggerExtend)
            + EXIT_MARGIN;
        for (AABB bound : bounds) {
            Vec3 center = bound.getCenter();
            double radius = projectedRadius(bound, normal);
            double nearest = center.subtract(exit.center()).dot(normal) - radius;
            result = Math.max(result, required - nearest);
        }
        return Math.max(0.0, result);
    }

    private static double normalHalfDepth(PortalPlacement exit,
                                          double horizontalTriggerExtend) {
        return exit.orientation() == PortalOrientation.VERTICAL
            ? PortalTriggerShape.VERTICAL_DEPTH * 0.5
            : PortalTriggerShape.HORIZONTAL_HEIGHT * 0.5
                + Math.max(0.0, horizontalTriggerExtend);
    }

    private static double projectedRadius(AABB bounds, Vec3 axis) {
        return Math.abs(axis.x) * bounds.getXsize() * 0.5
            + Math.abs(axis.y) * bounds.getYsize() * 0.5
            + Math.abs(axis.z) * bounds.getZsize() * 0.5;
    }

    private PortalTreeClearance() {}
}
