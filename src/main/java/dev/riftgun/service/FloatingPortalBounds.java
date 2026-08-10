package dev.riftgun.service;

import net.minecraft.world.phys.AABB;

/** Entity-space limit for floating portals; block build ceilings do not apply. */
public final class FloatingPortalBounds {
    public static final int VOID_SURVIVAL_DEPTH = 64;

    public static boolean allows(AABB bounds, int minimumBuildHeight) {
        return bounds.minY >= minimumBuildHeight - VOID_SURVIVAL_DEPTH;
    }

    private FloatingPortalBounds() {}
}
