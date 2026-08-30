package dev.riftgun.service;

import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Pure positioning rule for a horizontal FRONT portal adjacent to the predicted player bounds. */
public final class FrontHorizontalPortalPlacement {
    private static final double BELOW_FEET_CLEARANCE = 0.002;
    /** Half a block keeps the portal visually separate while remaining reachable by a normal jump. */
    private static final double ABOVE_HEAD_CLEARANCE = 0.5;

    public static Vec3 center(AABB playerBounds, Vec3 prediction,
                              PortalOrientation orientation) {
        if (orientation == PortalOrientation.VERTICAL) {
            throw new IllegalArgumentException("horizontal orientation required");
        }
        AABB predicted = playerBounds.move(prediction);
        double centerX = (predicted.minX + predicted.maxX) * 0.5;
        double centerZ = (predicted.minZ + predicted.maxZ) * 0.5;
        double halfDepth = PortalPlacement.DEPTH * 0.5;
        double centerY = orientation == PortalOrientation.BOTTOM
            ? predicted.maxY + halfDepth + ABOVE_HEAD_CLEARANCE
            : predicted.minY - halfDepth - BELOW_FEET_CLEARANCE;
        return new Vec3(centerX, centerY, centerZ);
    }

    private FrontHorizontalPortalPlacement() {}
}
