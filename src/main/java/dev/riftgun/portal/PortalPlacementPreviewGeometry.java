package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Pure geometry for a sparse four-corner placement footprint. */
public final class PortalPlacementPreviewGeometry {
    private static final double CORNER_LENGTH_RATIO = 0.16;
    private static final double NORMAL_OFFSET = 0.02;
    private static final double DIRECTION_LENGTH = 0.6;
    private static final double DIRECTION_HEAD_LENGTH = 0.16;
    private static final double DIRECTION_HEAD_WIDTH = 0.12;

    /** Adds a normal-direction arrow so horizontal portals remain legible from an edge-on view. */
    public static List<Segment> visibleOutline(PortalPlacement placement) {
        List<Segment> corners = corners(placement);
        if (placement.orientation() == PortalOrientation.VERTICAL) return corners;
        List<Segment> visible = new ArrayList<>(corners);
        Vec3 center = placement.center();
        // TOP is approached from above; BOTTOM is approached from below.
        Vec3 direction = placement.orientation() == PortalOrientation.TOP
            ? new Vec3(0.0, -1.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 tip = center.add(direction.scale(DIRECTION_LENGTH));
        Vec3 headBase = tip.subtract(direction.scale(DIRECTION_HEAD_LENGTH));
        Vec3 side = placement.right().scale(DIRECTION_HEAD_WIDTH);
        visible.add(new Segment(center, tip));
        visible.add(new Segment(tip, headBase.add(side)));
        visible.add(new Segment(tip, headBase.subtract(side)));
        return List.copyOf(visible);
    }

    public static List<Segment> corners(PortalPlacement placement) {
        double halfWidth = placement.geometry().width() * 0.5;
        double halfHeight = placement.geometry().height() * 0.5;
        double horizontalLength = placement.geometry().width() * CORNER_LENGTH_RATIO;
        double verticalLength = placement.geometry().height() * CORNER_LENGTH_RATIO;
        Vec3 center = placement.center().add(placement.normal().scale(NORMAL_OFFSET));
        Vec3 right = placement.right();
        Vec3 up = placement.up();
        List<Segment> segments = new ArrayList<>(8);

        for (int rightSign : new int[] {-1, 1}) {
            for (int upSign : new int[] {-1, 1}) {
                Vec3 corner = center
                    .add(right.scale(rightSign * halfWidth))
                    .add(up.scale(upSign * halfHeight));
                segments.add(new Segment(corner,
                    corner.add(right.scale(-rightSign * horizontalLength))));
                segments.add(new Segment(corner,
                    corner.add(up.scale(-upSign * verticalLength))));
            }
        }
        return List.copyOf(segments);
    }

    /** World-space center used by the pending-endpoint label. */
    public static Vec3 labelCenter(PortalPlacement placement) {
        return placement.center().add(placement.normal().scale(NORMAL_OFFSET));
    }

    public record Segment(Vec3 from, Vec3 to) {}

    private PortalPlacementPreviewGeometry() {}
}
