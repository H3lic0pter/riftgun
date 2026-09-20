package dev.riftgun.portal;

import java.util.Collection;
import java.util.function.Predicate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Places an entity tree beyond the exit trigger, with one optional vertical-door collision lift. */
final class PortalTreeClearance {
    private static final double EXIT_MARGIN = 0.05;
    private static final double COLLISION_EPSILON = 0.001;
    private static final double COLLISION_LIFT = 1.0;

    /** Keeps the feet at the door bottom and places the actual body just beyond the trigger. */
    static Vec3 verticalPosition(PortalPlacement exit, Vec3 entityPosition, AABB entityBounds) {
        Vec3 normal = exit.normal();
        double nearestToPosition = entityBounds.getCenter().subtract(entityPosition).dot(normal)
            - projectedRadius(entityBounds, normal);
        double distance = Math.max(0.0,
            PortalTriggerShape.VERTICAL_DEPTH * 0.5 + EXIT_MARGIN - nearestToPosition);
        return exit.center().subtract(exit.up().scale(exit.geometry().height() * 0.5))
            .add(normal.scale(distance));
    }

    /** Collision is advisory: lift a vertical exit once, without searching or rejecting the destination. */
    static Vec3 destination(PortalPlacement exit, Vec3 destination, Collection<AABB> predictedBounds,
                            double horizontalTriggerExtend, Predicate<AABB> collides) {
        Vec3 correction = exit.normal().scale(outwardCorrection(exit, predictedBounds, horizontalTriggerExtend));
        Vec3 correctedDestination = destination.add(correction);
        if (exit.orientation() == PortalOrientation.VERTICAL) {
            for (AABB bounds : predictedBounds) {
                if (collides.test(bounds.move(correction).deflate(COLLISION_EPSILON))) {
                    return correctedDestination.add(0.0, COLLISION_LIFT, 0.0);
                }
            }
        }
        return correctedDestination;
    }

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
