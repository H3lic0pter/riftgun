package dev.riftgun.pairing;

import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Line geometry rendered by the same pipeline for a pending endpoint frame and glyph. */
public final class PortalPairingPreviewGeometry {
    public static List<PortalPlacementPreviewGeometry.Segment> segments(
        PortalPlacement placement, PortalPairingEndpoint endpoint
    ) {
        List<PortalPlacementPreviewGeometry.Segment> segments = new ArrayList<>(
            PortalPlacementPreviewGeometry.corners(placement));
        Vec3 center = PortalPlacementPreviewGeometry.labelCenter(placement);
        double scale = Math.min(placement.geometry().width(), placement.geometry().height());
        Vec3 right = placement.right().scale(scale * 0.13);
        Vec3 up = placement.up().scale(scale * 0.18);
        if (endpoint == PortalPairingEndpoint.A) addA(segments, center, right, up);
        else if (endpoint == PortalPairingEndpoint.B) addB(segments, center, right, up);
        return List.copyOf(segments);
    }

    private static void addA(List<PortalPlacementPreviewGeometry.Segment> segments,
                             Vec3 center, Vec3 right, Vec3 up) {
        Vec3 leftBottom = center.subtract(right).subtract(up);
        Vec3 top = center.add(up);
        Vec3 rightBottom = center.add(right).subtract(up);
        segment(segments, leftBottom, top);
        segment(segments, top, rightBottom);
        segment(segments, center.subtract(right.scale(0.55)).subtract(up.scale(0.12)),
            center.add(right.scale(0.55)).subtract(up.scale(0.12)));
    }

    private static void addB(List<PortalPlacementPreviewGeometry.Segment> segments,
                             Vec3 center, Vec3 right, Vec3 up) {
        Vec3 leftTop = center.subtract(right).add(up);
        Vec3 leftMiddle = center.subtract(right);
        Vec3 leftBottom = center.subtract(right).subtract(up);
        Vec3 rightUpper = center.add(right).add(up.scale(0.52));
        Vec3 rightLower = center.add(right).subtract(up.scale(0.52));
        segment(segments, leftBottom, leftTop);
        segment(segments, leftTop, rightUpper);
        segment(segments, rightUpper, leftMiddle);
        segment(segments, leftMiddle, rightLower);
        segment(segments, rightLower, leftBottom);
    }

    private static void segment(List<PortalPlacementPreviewGeometry.Segment> segments,
                                Vec3 from, Vec3 to) {
        segments.add(new PortalPlacementPreviewGeometry.Segment(from, to));
    }

    private PortalPairingPreviewGeometry() {}
}
