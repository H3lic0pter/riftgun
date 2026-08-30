package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Shared FRONT geometry and validation used by server authority and client previews. */
public final class FrontPortalPlacementPlanner {
    public static Result resolve(Vec3 playerPosition, AABB playerBounds, Vec3 displacement,
                                 float yaw, PortalOrientation orientation,
                                 PortalAperture aperture, double frontDistance,
                                 int minimumBuildHeight, double minimumExposure,
                                 Probe probe) {
        if (orientation == null || aperture == null || probe == null) {
            throw new IllegalArgumentException("front placement inputs are required");
        }
        List<PortalGeometry> geometries = PortalAperturePolicy.expanded(aperture)
            ? List.of(expandedGeometry(orientation), standardGeometry(orientation))
            : List.of(standardGeometry(orientation));
        Result last = Result.failure("message.riftgun.front_obstructed");
        for (PortalGeometry geometry : geometries) {
            PortalPlacement placement = placement(playerPosition, playerBounds, displacement,
                yaw, orientation, geometry, frontDistance);
            if (!FloatingPortalBounds.allows(placement.bounds(), minimumBuildHeight)) {
                last = Result.failure("message.riftgun.void_portal_too_late");
                continue;
            }
            double exposure = geometry.expanded()
                ? PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE : minimumExposure;
            if (probe.hasMinimumExposure(placement, exposure)) return Result.success(placement);
            last = Result.failure("message.riftgun.front_obstructed");
        }
        return last;
    }

    private static PortalPlacement placement(Vec3 playerPosition, AABB playerBounds,
                                             Vec3 displacement, float yaw,
                                             PortalOrientation orientation,
                                             PortalGeometry geometry,
                                             double frontDistance) {
        if (orientation != PortalOrientation.VERTICAL) {
            Vec3 center = FrontHorizontalPortalPlacement.center(
                playerBounds, displacement, orientation);
            return new PortalPlacement(center, orientation, geometry, yaw, null, null);
        }
        Vec3 look = Vec3.directionFromRotation(0.0F, yaw).normalize();
        Vec3 center = playerPosition.add(displacement).add(look.scale(frontDistance))
            .add(0.0, geometry.height() * 0.5, 0.0);
        return new PortalPlacement(center, orientation, geometry,
            yawFromNormal(look.scale(-1.0)), null, null);
    }

    private static PortalGeometry standardGeometry(PortalOrientation orientation) {
        return orientation == PortalOrientation.VERTICAL
            ? PortalGeometry.FLOATING_VERTICAL : PortalGeometry.HORIZONTAL;
    }

    private static PortalGeometry expandedGeometry(PortalOrientation orientation) {
        return orientation == PortalOrientation.VERTICAL
            ? PortalAperturePolicy.floatingVertical() : PortalAperturePolicy.horizontal();
    }

    private static float yawFromNormal(Vec3 normal) {
        return (float) Math.toDegrees(Math.atan2(-normal.x, normal.z));
    }

    @FunctionalInterface
    public interface Probe {
        boolean hasMinimumExposure(PortalPlacement placement, double minimumExposure);
    }

    public record Result(@Nullable PortalPlacement placement, @Nullable String errorKey) {
        public static Result success(PortalPlacement placement) {
            return new Result(placement, null);
        }

        public static Result failure(String errorKey) {
            return new Result(null, errorKey);
        }

        public boolean successful() {
            return placement != null;
        }
    }

    private FrontPortalPlacementPlanner() {}
}
