package dev.riftgun.service;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Entity-space limits for floating portals, independent of block build height and void damage. */
public final class FloatingPortalBounds {
    // Minecraft's Level.isInSpawnableBounds limits, kept independent of world/bootstrap state.
    private static final double HORIZONTAL_LIMIT = 30_000_000.0;
    private static final double VERTICAL_LIMIT = 20_000_000.0;

    public static boolean allows(AABB bounds) {
        return allows(bounds.minX, bounds.minY, bounds.minZ)
            && allows(bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    public static boolean allows(BlockPos position) {
        return allows(position.getX(), position.getY(), position.getZ());
    }

    private static boolean allows(double x, double y, double z) {
        return x >= -HORIZONTAL_LIMIT && x < HORIZONTAL_LIMIT
            && y >= -VERTICAL_LIMIT && y < VERTICAL_LIMIT
            && z >= -HORIZONTAL_LIMIT && z < HORIZONTAL_LIMIT;
    }

    private FloatingPortalBounds() {}
}
