package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;

/** Exact segment/portal-face test used after the chunk index finds a nearby candidate. */
final class PortalSweptIntersection {
    private static final double EPSILON = 1.0E-7;
    private static final double IMPACT_EPSILON = 1.0E-4;

    static boolean crosses(PortalPlacement portal, Vec3 start, Vec3 end, double radius) {
        return Double.isFinite(crossingFraction(portal, start, end, radius));
    }

    static boolean crossesBeforeImpact(PortalPlacement portal, Vec3 start, Vec3 end,
                                       double radius, Vec3 impact) {
        double crossing = crossingFraction(portal, start, end, radius);
        if (!Double.isFinite(crossing)) return false;
        Vec3 path = end.subtract(start);
        double pathLengthSqr = path.lengthSqr();
        if (pathLengthSqr < EPSILON) return false;
        double impactFraction = impact.subtract(start).dot(path) / pathLengthSqr;
        return crossing <= impactFraction + IMPACT_EPSILON;
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

    private PortalSweptIntersection() {}
}
