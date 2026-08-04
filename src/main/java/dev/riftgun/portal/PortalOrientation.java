package dev.riftgun.portal;

import net.minecraft.world.phys.Vec3;

public enum PortalOrientation {
    VERTICAL,
    TOP,
    BOTTOM;

    public Vec3 normal(float yaw) {
        return switch (this) {
            case VERTICAL -> Vec3.directionFromRotation(0.0F, yaw).normalize();
            case TOP -> new Vec3(0.0, 1.0, 0.0);
            case BOTTOM -> new Vec3(0.0, -1.0, 0.0);
        };
    }

    public Vec3 up(float yaw) {
        return switch (this) {
            case VERTICAL -> new Vec3(0.0, 1.0, 0.0);
            case TOP, BOTTOM -> new Vec3(0.0, 0.0, 1.0);
        };
    }

    public Vec3 right(float yaw) {
        return switch (this) {
            case VERTICAL -> up(yaw).cross(normal(yaw)).normalize();
            case TOP, BOTTOM -> new Vec3(1.0, 0.0, 0.0);
        };
    }

    public Vec3 traversalUp(float yaw) {
        return switch (this) {
            case VERTICAL -> up(yaw);
            case TOP, BOTTOM -> Vec3.directionFromRotation(0.0F, yaw).normalize();
        };
    }

    public Vec3 traversalRight(float yaw) {
        return traversalUp(yaw).cross(normal(yaw)).normalize();
    }

    public PortalOrientation oppositeSurface() {
        return switch (this) {
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case VERTICAL -> VERTICAL;
        };
    }

    public static PortalOrientation byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : VERTICAL;
    }
}
