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

    private PortalTransform() {}
}
