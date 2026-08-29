package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Pure geometry for a sparse four-corner placement footprint. */
public final class PortalPlacementPreviewGeometry {
    private static final double CORNER_LENGTH_RATIO = 0.16;
    private static final double NORMAL_OFFSET = 0.02;

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

    public record Segment(Vec3 from, Vec3 to) {}

    private PortalPlacementPreviewGeometry() {}
}
