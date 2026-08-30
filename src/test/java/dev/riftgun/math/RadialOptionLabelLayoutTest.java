package dev.riftgun.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RadialOptionLabelLayoutTest {
    private static final int OUTER_RADIUS = 66;
    private static final int LABEL_RADIUS = 48;

    @Test
    void longFloatingSideLabelsStayInsideTheRingAndOutsideTheCenter() {
        for (int index : new int[] {1, 2}) {
            RadialOptionLabelLayout.Placement placement = RadialOptionLabelLayout.resolve(
                index, 3, 0, 0, LABEL_RADIUS, OUTER_RADIUS, true, 60);
            int halfWidth = placement.maximumWidth() / 2;

            assertEquals(RadialOptionLabelLayout.FLOATING_SIDE_MAXIMUM_WIDTH,
                placement.maximumWidth());
            assertTrue(Math.abs(placement.x()) + halfWidth <= OUTER_RADIUS);
            assertTrue(Math.abs(placement.x()) - halfWidth >= 30,
                "side label must not overlap the center label reserve");
        }
    }

    @Test
    void topAndShortLocalizedLabelsKeepTheOriginalCircularLayout() {
        RadialOptionLabelLayout.Placement top = RadialOptionLabelLayout.resolve(
            0, 3, 0, 0, LABEL_RADIUS, OUTER_RADIUS, true, 60);
        RadialOptionLabelLayout.Placement shortSide = RadialOptionLabelLayout.resolve(
            1, 3, 0, 0, LABEL_RADIUS, OUTER_RADIUS, true, 24);

        assertEquals(0, top.x());
        assertEquals(-LABEL_RADIUS, top.y());
        assertEquals(RadialOptionLabelLayout.DEFAULT_MAXIMUM_WIDTH, top.maximumWidth());
        assertEquals(42, shortSide.x());
        assertEquals(RadialOptionLabelLayout.DEFAULT_MAXIMUM_WIDTH, shortSide.maximumWidth());
    }
}
