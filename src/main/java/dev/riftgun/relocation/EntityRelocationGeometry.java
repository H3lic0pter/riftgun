package dev.riftgun.relocation;

import net.minecraft.world.phys.Vec3;

/** Visual sizing and feet-plane placement shared by the entrance and exit adapters. */
public final class EntityRelocationGeometry {
    public static final float VISIBLE_CLEARANCE = 0.25F;
    public static final float MINIMUM_VISIBLE_COVERAGE = 0.80F;
    public static final float MINIMUM_SIDE = 1.0F;
    public static final float MAXIMUM_SIDE = 8.0F;

    public static float sideLength(float boundingWidth, float boundingDepth) {
        float largest = Math.max(Math.max(0.0F, boundingWidth), Math.max(0.0F, boundingDepth));
        return normalizeSide((largest + VISIBLE_CLEARANCE) / MINIMUM_VISIBLE_COVERAGE);
    }

    public static float normalizeSide(float side) {
        return Math.clamp(side, MINIMUM_SIDE, MAXIMUM_SIDE);
    }

    public static double centerY(double entityFeetY, double visualDepth) {
        return entityFeetY + Math.max(0.0, visualDepth) * 0.5;
    }

    public static Vec3 playerDestinationExitCenter(Vec3 playerPosition, double boundingBoxMaxY) {
        return new Vec3(playerPosition.x, boundingBoxMaxY + 1.0, playerPosition.z);
    }

    public static double bottomExitHeight(double entityHeight) {
        return Math.max(3.0, Math.max(0.0, entityHeight) + 0.35);
    }

    public static Vec3 savedDestinationBottomExitCenter(Vec3 destination, double entityHeight) {
        return destination.add(0.0, bottomExitHeight(entityHeight), 0.0);
    }

    public static Vec3 bottomOutputPosition(Vec3 exitCenter, double entityHeight) {
        return exitCenter.add(0.0, -(Math.max(0.0, entityHeight) + 0.15), 0.0);
    }

    private EntityRelocationGeometry() {}
}
