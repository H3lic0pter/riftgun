package dev.riftgun.portal;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PortalTriggerShape {
    public static final double VERTICAL_DEPTH = 0.12;
    public static final double HORIZONTAL_HEIGHT = 0.18;
    public static final double HORIZONTAL_EDGE_INSET = 0.12;

    public static boolean intersects(PortalPlacement placement, AABB entityBounds) {
        return intersects(placement, entityBounds, 0.0);
    }

    public static boolean intersects(PortalPlacement placement, AABB entityBounds,
                                     double horizontalTriggerExtend) {
        Vec3 entityCenter = entityBounds.getCenter();
        Vec3 delta = entityCenter.subtract(placement.center());
        Vec3 right = placement.right();
        Vec3 up = placement.up();
        Vec3 normal = placement.normal();

        double entityRightRadius = projectedRadius(entityBounds, right);
        double entityUpRadius = projectedRadius(entityBounds, up);
        double entityNormalRadius = projectedRadius(entityBounds, normal);
        // Extra trigger reach along the normal for flat doors catches falling bodies before
        // their feet touch the ground, so fall damage is resolved at the exit. Server-configurable.
        double normalHalfDepth = placement.orientation() == PortalOrientation.VERTICAL
            ? VERTICAL_DEPTH * 0.5
            : HORIZONTAL_HEIGHT * 0.5 + Math.max(0.0, horizontalTriggerExtend);

        boolean withinNormal = Math.abs(delta.dot(normal)) <= normalHalfDepth + entityNormalRadius;
        if (placement.orientation() != PortalOrientation.VERTICAL) {
            double halfWidth = Math.max(0.0, placement.geometry().width() * 0.5 - HORIZONTAL_EDGE_INSET);
            double halfHeight = Math.max(0.0, placement.geometry().height() * 0.5 - HORIZONTAL_EDGE_INSET);
            return withinNormal
                && Math.abs(delta.dot(right)) <= halfWidth
                && Math.abs(delta.dot(up)) <= halfHeight;
        }

        return withinNormal
            && Math.abs(delta.dot(right)) <= placement.geometry().width() * 0.5 + entityRightRadius
            && Math.abs(delta.dot(up)) <= placement.geometry().height() * 0.5 + entityUpRadius;
    }

    private static double projectedRadius(AABB bounds, Vec3 axis) {
        double halfX = bounds.getXsize() * 0.5;
        double halfY = bounds.getYsize() * 0.5;
        double halfZ = bounds.getZsize() * 0.5;
        return Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
    }

    private PortalTriggerShape() {}
}
