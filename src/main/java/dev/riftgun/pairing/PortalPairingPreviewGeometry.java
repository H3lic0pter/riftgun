package dev.riftgun.pairing;

import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.portal.PortalPlacementPreviewGeometry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Static world-space line geometry for pending pairing endpoints. */
public final class PortalPairingPreviewGeometry {
    private static final int FRAME_COLOR = 0xFFF0F0F0;

    public static List<ColoredSegment> segments(
        PortalPlacement placement, PortalPairingEndpoint endpoint
    ) {
        List<ColoredSegment> segments = new ArrayList<>();
        PortalPlacementPreviewGeometry.corners(placement).forEach(
            segment -> segments.add(new ColoredSegment(segment, FRAME_COLOR)));
        addGlyph(segments, placement, endpoint);
        return List.copyOf(segments);
    }

    /** Entity relocation keeps only its fixed-target numeral. */
    public static List<ColoredSegment> entityTargetSegments(PortalPlacement placement) {
        List<ColoredSegment> segments = new ArrayList<>();
        addGlyph(segments, placement, PortalPairingEndpoint.A);
        return List.copyOf(segments);
    }

    private static void addGlyph(List<ColoredSegment> segments, PortalPlacement placement,
                                 PortalPairingEndpoint endpoint) {
        Vec3 center = PortalPlacementPreviewGeometry.labelCenter(placement);
        double scale = Math.min(placement.geometry().width(), placement.geometry().height());
        Vec3 right = placement.right().scale(scale * 0.13);
        Vec3 up = placement.up().scale(scale * 0.18);
        int color = PortalPairingLabels.colorArgb(endpoint);
        if (endpoint == PortalPairingEndpoint.A) addOne(segments, center, right, up, color);
        else if (endpoint == PortalPairingEndpoint.B) addTwo(segments, center, right, up, color);
    }

    private static void addOne(List<ColoredSegment> segments, Vec3 center,
                               Vec3 right, Vec3 up, int color) {
        segment(segments, center.subtract(up), center.add(up), color);
        segment(segments, center.add(up).subtract(right.scale(0.72)),
            center.add(up).add(right.scale(0.72)), color);
        segment(segments, center.subtract(up).subtract(right.scale(0.72)),
            center.subtract(up).add(right.scale(0.72)), color);
    }

    private static void addTwo(List<ColoredSegment> segments, Vec3 center,
                               Vec3 right, Vec3 up, int color) {
        segment(segments, center.subtract(right.scale(0.42)).subtract(up),
            center.subtract(right.scale(0.42)).add(up), color);
        segment(segments, center.add(right.scale(0.42)).subtract(up),
            center.add(right.scale(0.42)).add(up), color);
        segment(segments, center.add(up).subtract(right), center.add(up).add(right), color);
        segment(segments, center.subtract(up).subtract(right), center.subtract(up).add(right), color);
    }

    private static void segment(List<ColoredSegment> segments, Vec3 from, Vec3 to, int color) {
        segments.add(new ColoredSegment(
            new PortalPlacementPreviewGeometry.Segment(from, to), color));
    }

    public record ColoredSegment(PortalPlacementPreviewGeometry.Segment geometry, int color) {}

    private PortalPairingPreviewGeometry() {}
}
