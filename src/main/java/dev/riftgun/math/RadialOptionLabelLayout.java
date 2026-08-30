package dev.riftgun.math;

/** Pure label placement that keeps long precision-radial side text out of the center. */
public final class RadialOptionLabelLayout {
    static final int DEFAULT_MAXIMUM_WIDTH = 72;
    static final int FLOATING_SIDE_MAXIMUM_WIDTH = 32;
    private static final int FLOATING_SIDE_EDGE_GAP = 2;

    public static Placement resolve(int index, int optionCount, int centerX, int centerY,
                                    int labelRadius, int outerRadius,
                                    boolean floatingOrientation, int measuredTextWidth) {
        double angle = Math.toRadians(-90.0 + index * 360.0 / optionCount);
        int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius);
        int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius);
        int maximumWidth = DEFAULT_MAXIMUM_WIDTH;
        if (floatingOrientation && optionCount == 3 && index > 0
            && measuredTextWidth > FLOATING_SIDE_MAXIMUM_WIDTH) {
            int side = index == 1 ? 1 : -1;
            x = centerX + side * (outerRadius - FLOATING_SIDE_MAXIMUM_WIDTH / 2
                - FLOATING_SIDE_EDGE_GAP);
            maximumWidth = FLOATING_SIDE_MAXIMUM_WIDTH;
        }
        return new Placement(x, y, maximumWidth);
    }

    public record Placement(int x, int y, int maximumWidth) {}

    private RadialOptionLabelLayout() {}
}
