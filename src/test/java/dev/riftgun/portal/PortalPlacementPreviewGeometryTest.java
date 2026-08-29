package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalPlacementPreviewGeometryTest {
    @Test
    void createsTwoShortSegmentsAtEveryFootprintCorner() {
        PortalPlacement placement = new PortalPlacement(new Vec3(4.0, 8.0, 12.0),
            PortalOrientation.VERTICAL, PortalGeometry.FLOATING_VERTICAL,
            0.0F, null, null);

        var segments = PortalPlacementPreviewGeometry.corners(placement);

        assertEquals(8, segments.size());
        assertTrue(segments.stream().allMatch(segment -> segment.from().distanceTo(segment.to()) > 0.0));
        assertTrue(segments.stream().allMatch(segment -> segment.from().distanceTo(segment.to())
            < Math.max(placement.geometry().width(), placement.geometry().height()) * 0.25));
    }

    @Test
    void offsetsHorizontalFootprintAlongItsNormal() {
        PortalPlacement placement = new PortalPlacement(Vec3.ZERO,
            PortalOrientation.TOP, PortalGeometry.HORIZONTAL,
            35.0F, null, null);

        var segments = PortalPlacementPreviewGeometry.corners(placement);

        assertTrue(segments.stream().allMatch(segment -> segment.from().y > 0.0));
        assertTrue(segments.stream().allMatch(segment -> segment.to().y > 0.0));
    }

    @Test
    void offsetsLabelInFrontOfAttachedSurface() {
        PortalPlacement placement = new PortalPlacement(new Vec3(4.0, 8.0, 12.0),
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL,
            0.0F, null, null);

        Vec3 labelCenter = PortalPlacementPreviewGeometry.labelCenter(placement);

        assertTrue(labelCenter.subtract(placement.center()).dot(placement.normal()) > 0.0,
            "depth-tested label must sit in front of its backing surface");
    }
}
