package dev.riftgun.network;

import net.minecraft.server.level.ServerLevel;

/** Normalizes user-entered destination coordinates to blocks accepted by Minecraft. */
final class DestinationCoordinateBounds {
    // Mirrors Level.isInWorldBoundsHorizontal; Minecraft exposes the predicate but not its bounds.
    static final int MINIMUM_HORIZONTAL_BLOCK = -30_000_000;
    static final int MAXIMUM_HORIZONTAL_BLOCK_EXCLUSIVE = 30_000_000;

    static Coordinates clamp(ServerLevel level, double x, double y, double z) {
//? if >=1.21.11 {
        /*int minimumY = level.dimensionType().minY();
        return clamp(x, y, z, minimumY, minimumY + level.dimensionType().height());
*///?} else {
        return clamp(x, y, z, level.getMinBuildHeight(), level.getMaxBuildHeight());
//?}
    }

    static Coordinates clamp(double x, double y, double z,
                             int minimumY, int maximumYExclusive) {
        return new Coordinates(
            clampToBlockRange(x, MINIMUM_HORIZONTAL_BLOCK, MAXIMUM_HORIZONTAL_BLOCK_EXCLUSIVE),
            clampToBlockRange(y, minimumY, maximumYExclusive),
            clampToBlockRange(z, MINIMUM_HORIZONTAL_BLOCK, MAXIMUM_HORIZONTAL_BLOCK_EXCLUSIVE));
    }

    private static double clampToBlockRange(double value, int minimum,
                                            int maximumExclusive) {
        if (value < minimum) return minimum;
        if (value >= maximumExclusive) return maximumExclusive - 1.0;
        return value;
    }

    record Coordinates(double x, double y, double z) {}

    private DestinationCoordinateBounds() {}
}
