package dev.riftgun.math;

import java.util.OptionalInt;

/** Pure radial hit testing shared by both client versions. */
public final class RadialModeGeometry {
    public static OptionalInt selectionIndex(double dx, double dy, int optionCount, double deadZone) {
        if (optionCount <= 0 || dx * dx + dy * dy <= deadZone * deadZone) return OptionalInt.empty();
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        double halfSector = 180.0 / optionCount;
        double normalized = (angle + halfSector + 90.0) % 360.0;
        if (normalized < 0.0) normalized += 360.0;
        return OptionalInt.of(Math.min(optionCount - 1, (int) Math.floor(normalized * optionCount / 360.0)));
    }

    private RadialModeGeometry() {}
}
