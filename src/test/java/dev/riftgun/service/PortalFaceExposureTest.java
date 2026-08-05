package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalFaceExposureTest {
    @Test
    void allowsExactlyFortyPercentOfVerticalFaceToRemainExposed() {
        PortalPlacement placement = vertical(0.0F);
        AABB sixtyPercentCover = new AABB(-1.0, -2.0, 0.0, 0.13, 2.0, 1.0);

        assertTrue(PortalFaceExposure.hasMinimumExposure(
            placement, List.of(sixtyPercentCover), 0.40));
    }

    @Test
    void rejectsWhenOnlyQuarterOfVerticalFaceRemainsExposed() {
        PortalPlacement placement = vertical(0.0F);
        AABB seventyFivePercentCover = new AABB(-1.0, -2.0, 0.0, 0.31, 2.0, 1.0);

        assertFalse(PortalFaceExposure.hasMinimumExposure(
            placement, List.of(seventyFivePercentCover), 0.40));
    }

    @Test
    void ignoresCollisionInsideRotatedBoundingBoxButOutsideRealFace() {
        PortalPlacement placement = vertical(45.0F);
        AABB broadPhaseCorner = new AABB(0.30, -2.0, -0.46, 0.46, 2.0, -0.30);

        assertTrue(placement.bounds().intersects(broadPhaseCorner));
        assertTrue(PortalFaceExposure.hasMinimumExposure(
            placement, List.of(broadPhaseCorner), 0.40));
    }

    @Test
    void appliesSameExposureRuleToHorizontalDownshotFace() {
        PortalPlacement placement = new PortalPlacement(Vec3.ZERO, PortalOrientation.TOP,
            PortalGeometry.HORIZONTAL, 0.0F, null, null);
        AABB seventyFivePercentCover = new AABB(-1.0, 0.0, -1.0, 0.26, 1.0, 1.0);

        assertFalse(PortalFaceExposure.hasMinimumExposure(
            placement, List.of(seventyFivePercentCover), 0.40));
    }

    @Test
    void sumsExposureAcrossSeparatedOpenRegions() {
        PortalPlacement placement = vertical(0.0F);
        AABB middleSixtyPercentCover = new AABB(-0.37, -2.0, 0.0, 0.37, 2.0, 1.0);

        assertTrue(PortalFaceExposure.hasMinimumExposure(
            placement, List.of(middleSixtyPercentCover), 0.40));
    }

    private static PortalPlacement vertical(float yaw) {
        return new PortalPlacement(Vec3.ZERO, PortalOrientation.VERTICAL,
            PortalGeometry.FLOATING_VERTICAL, yaw, null, null);
    }
}
