package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;

public final class PortalTransform {
    public static Vec3 between(Vec3 vector,
                               PortalOrientation sourceOrientation, float sourceYaw,
                               PortalOrientation targetOrientation, float targetYaw) {
        Vec3 sourceRight = sourceOrientation.traversalRight(sourceYaw);
        Vec3 sourceUp = sourceOrientation.traversalUp(sourceYaw);
        Vec3 sourceNormal = sourceOrientation.normal(sourceYaw);
        double localRight = vector.dot(sourceRight);
        double localUp = vector.dot(sourceUp);
        double localNormal = vector.dot(sourceNormal);
        return targetOrientation.traversalRight(targetYaw).scale(localRight)
            .add(targetOrientation.traversalUp(targetYaw).scale(localUp))
            .add(targetOrientation.normal(targetYaw).scale(Math.abs(localNormal)));
    }

    /**
     * Coordinate remap with explicit control over the right and normal components.
     * {@code rightFactor} mirrors the lateral axis ({@code -1} makes entering from the
     * portal's left exit from the target's right), while {@code normalFactor} chooses the
     * heading relative to the target face: {@code 1} points away (standard walk-through),
     * {@code -1} points at the face (used so a player who backed through faces the exit).
     */
    public static Vec3 betweenFactors(Vec3 vector,
                                      PortalOrientation sourceOrientation, float sourceYaw,
                                      PortalOrientation targetOrientation, float targetYaw,
                                      float rightFactor, float normalFactor) {
        Vec3 sourceRight = sourceOrientation.traversalRight(sourceYaw);
        Vec3 sourceUp = sourceOrientation.traversalUp(sourceYaw);
        Vec3 sourceNormal = sourceOrientation.normal(sourceYaw);
        double localRight = vector.dot(sourceRight);
        double localUp = vector.dot(sourceUp);
        double localNormal = vector.dot(sourceNormal);
        return targetOrientation.traversalRight(targetYaw).scale(localRight * rightFactor)
            .add(targetOrientation.traversalUp(targetYaw).scale(localUp))
            .add(targetOrientation.normal(targetYaw).scale(localNormal * normalFactor));
    }

    private PortalTransform() {}
}
