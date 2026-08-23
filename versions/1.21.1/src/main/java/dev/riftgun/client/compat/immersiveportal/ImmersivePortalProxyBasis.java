package dev.riftgun.client.compat.immersiveportal;

import net.minecraft.world.phys.Vec3;

/** IP portal axes whose cross product agrees with the Rift face normal. */
record ImmersivePortalProxyBasis(Vec3 right, Vec3 up) {
    static ImmersivePortalProxyBasis orient(Vec3 right, Vec3 up, Vec3 normal) {
        return right.cross(up).dot(normal) < 0.0
            ? new ImmersivePortalProxyBasis(right.scale(-1.0), up)
            : new ImmersivePortalProxyBasis(right, up);
    }

    static Vec3 transformView(ImmersivePortalProxyBasis source,
                              ImmersivePortalProxyBasis target, Vec3 view) {
        Vec3 sourceNormal = source.right.cross(source.up);
        Vec3 targetNormal = target.right.cross(target.up);
        return target.right.scale(-view.dot(source.right))
            .add(target.up.scale(view.dot(source.up)))
            .add(targetNormal.scale(-view.dot(sourceNormal)));
    }
}
