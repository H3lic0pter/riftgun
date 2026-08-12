package dev.riftgun.portal;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Exact segment/portal-face test used after the chunk index finds a nearby candidate. */
final class PortalProjectileIntersection {
    private static final double EPSILON = 1.0E-7;

    static boolean crosses(PortalPlacement portal, Projectile projectile) {
        Vec3 start = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        Vec3 end = projectile.position();
        double radius = Math.max(projectile.getBbWidth(), projectile.getBbHeight()) * 0.5;
        return crosses(portal, start, end, radius);
    }

    static boolean crosses(PortalPlacement portal, Vec3 start, Vec3 end, double radius) {
        return Double.isFinite(crossingFraction(portal, start, end, radius));
    }

    static double crossingFraction(PortalPlacement portal, Vec3 start, Vec3 end, double radius) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < EPSILON) return Double.NaN;

        Vec3 normal = portal.orientation().normal(portal.yaw());
        double startDistance = start.subtract(portal.center()).dot(normal);
        double endDistance = end.subtract(portal.center()).dot(normal);
        double denominator = startDistance - endDistance;
        if (Math.abs(denominator) < EPSILON) return Double.NaN;
        double fraction = startDistance / denominator;
        if (fraction < 0.0 || fraction > 1.0) return Double.NaN;

        Vec3 hit = start.add(delta.scale(fraction)).subtract(portal.center());
        radius = Math.max(0.0, radius);
        double right = Math.abs(hit.dot(portal.orientation().right(portal.yaw())));
        double up = Math.abs(hit.dot(portal.orientation().up(portal.yaw())));
        return right <= portal.geometry().width() * 0.5 + radius
            && up <= portal.geometry().height() * 0.5 + radius ? fraction : Double.NaN;
    }

    private PortalProjectileIntersection() {}
}
