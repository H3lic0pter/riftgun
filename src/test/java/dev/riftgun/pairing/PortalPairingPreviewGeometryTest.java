package dev.riftgun.pairing;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void frameContainsEndpointGlyphInThePortalPlane() {
        PortalPlacement placement = placement();
        List<PortalPlacementPreviewGeometry.Segment> corners =
            PortalPlacementPreviewGeometry.corners(placement);
        var a = PortalPairingPreviewGeometry.segments(placement, PortalPairingEndpoint.A);
        var b = PortalPairingPreviewGeometry.segments(placement, PortalPairingEndpoint.B);

        assertEquals(corners.size() + 3, a.size(), "I needs one stem and two bars");
        assertEquals(corners.size() + 4, b.size(), "II needs two stems and two bars");
        assertNotEquals(a, b);
        assertTrue(a.subList(corners.size(), a.size()).stream().allMatch(colored ->
            colored.color() == 0xFF9CC9D8 && inFront(colored.geometry(), placement)));
        assertTrue(b.subList(corners.size(), b.size()).stream().allMatch(colored ->
            colored.color() == 0xFFE19A52 && inFront(colored.geometry(), placement)));
        assertTrue(a.subList(0, corners.size()).stream().allMatch(colored ->
            colored.color() == 0xFFF0F0F0));
    }

    @Test
    void entityTargetContainsOnlyTheBlueFirstNumeral() {
        PortalPlacement placement = placement();
        var marker = PortalPairingPreviewGeometry.entityTargetSegments(placement);

        assertEquals(3, marker.size());
        assertTrue(marker.stream().allMatch(segment ->
            segment.color() == 0xFF9CC9D8 && inFront(segment.geometry(), placement)));
    }

    private static PortalPlacement placement() {
        return new PortalPlacement(Vec3.ZERO, PortalOrientation.VERTICAL,
            PortalGeometry.SURFACE_VERTICAL, 0.0F, null, null);
    }

    private static boolean inFront(PortalPlacementPreviewGeometry.Segment segment,
                                   PortalPlacement placement) {
        return segment.from().subtract(placement.center()).dot(placement.normal()) > 0.0
            && segment.to().subtract(placement.center()).dot(placement.normal()) > 0.0;
    }
}
