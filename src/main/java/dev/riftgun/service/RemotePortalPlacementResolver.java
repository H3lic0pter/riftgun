package dev.riftgun.service;

import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Shared REMOTE candidate search used by server authority and client-only previews. */
public final class RemotePortalPlacementResolver {
    private static final double MINIMUM_DISTANCE = 1.5;
    private static final double HIT_OFFSET = 0.18;
    private static final double SEARCH_STEP = 0.25;

    public static Optional<PortalPlacement> resolve(Level level, Entity viewer,
                                                    double maximumRange, PortalAperture aperture,
                                                    float horizontalPitchThreshold,
                                                    double minimumExposure) {
        Vec3 eye = viewer.getEyePosition();
        Vec3 look = viewer.getLookAngle().normalize();
        Vec3 rayEnd = eye.add(look.scale(maximumRange));
        HitResult hit = level.clip(new ClipContext(
            eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        double distance = hit.getType() == HitResult.Type.BLOCK
            ? Math.max(MINIMUM_DISTANCE, eye.distanceTo(hit.getLocation()) - HIT_OFFSET)
            : maximumRange;
        PortalOrientation orientation = VanillaPortalPlacementResolver.horizontalOrientation(
            viewer.getXRot(), horizontalPitchThreshold);
        PortalGeometry standard = orientation == PortalOrientation.VERTICAL
            ? PortalGeometry.FLOATING_VERTICAL : PortalGeometry.HORIZONTAL;
        PortalGeometry expanded = orientation == PortalOrientation.VERTICAL
            ? PortalAperturePolicy.floatingVertical() : PortalAperturePolicy.horizontal();

        for (double candidateDistance = distance;
             candidateDistance >= MINIMUM_DISTANCE;
             candidateDistance -= SEARCH_STEP) {
            Vec3 center = eye.add(look.scale(candidateDistance));
            if (PortalAperturePolicy.expanded(aperture)) {
                PortalPlacement placement = placement(center, orientation, expanded, viewer.getYRot());
                if (available(level, placement, PortalAperturePolicy.EXPANDED_MINIMUM_EXPOSURE)) {
                    return Optional.of(placement);
                }
            }
            PortalPlacement placement = placement(center, orientation, standard, viewer.getYRot());
            if (available(level, placement, minimumExposure)) return Optional.of(placement);
        }
        return Optional.empty();
    }

    private static PortalPlacement placement(Vec3 center, PortalOrientation orientation,
                                             PortalGeometry geometry, float yaw) {
        return new PortalPlacement(center, orientation, geometry, yaw, null, null);
    }

    private static boolean available(Level level, PortalPlacement placement, double minimumExposure) {
        return !outsideWorld(level, placement)
            && PortalFaceExposure.hasMinimumExposure(level, placement, minimumExposure);
    }

    private static boolean outsideWorld(Level level, PortalPlacement placement) {
//? if >=1.21.11 {
        /*int minimumY = level.dimensionType().minY();
        int maximumY = minimumY + level.dimensionType().height();
*///?} else {
        int minimumY = level.getMinBuildHeight();
        int maximumY = level.getMaxBuildHeight();
//?}
        return placement.bounds().minY < minimumY || placement.bounds().maxY > maximumY;
    }

    private RemotePortalPlacementResolver() {}
}
