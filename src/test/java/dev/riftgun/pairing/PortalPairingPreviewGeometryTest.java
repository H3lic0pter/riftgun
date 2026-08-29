package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PortalPairingPreviewGeometryTest {
    @Test
    void frameContainsVisibleEndpointGlyphInTheSameLineBuffer() {
        PortalPlacement placement = new PortalPlacement(Vec3.ZERO,
            PortalOrientation.VERTICAL, PortalGeometry.SURFACE_VERTICAL,
            0.0F, null, null);

        List<PortalPlacementPreviewGeometry.Segment> corners =
            PortalPlacementPreviewGeometry.corners(placement);
        var a = PortalPairingPreviewGeometry.segments(placement, PortalPairingEndpoint.A);
        var b = PortalPairingPreviewGeometry.segments(placement, PortalPairingEndpoint.B);

        assertTrue(a.size() > corners.size(), "A glyph must share the visible frame buffer");
        assertTrue(b.size() > corners.size(), "B glyph must share the visible frame buffer");
        assertNotEquals(a, b, "A and B glyph geometry must differ");
        assertTrue(a.subList(corners.size(), a.size()).stream().allMatch(segment ->
            segment.from().subtract(placement.center()).dot(placement.normal()) > 0.0
                && segment.to().subtract(placement.center()).dot(placement.normal()) > 0.0));
    }
}
