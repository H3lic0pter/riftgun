package dev.riftgun.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record PortalPlacement(
    Vec3 center,
    PortalOrientation orientation,
    PortalGeometry geometry,
    float yaw,
    @Nullable BlockPos anchor,
    @Nullable Direction anchorFace
) {
    public static final double DEPTH = 0.12;

    public Vec3 normal() {
        return orientation.normal(yaw);
    }

    public Vec3 up() {
        return orientation.up(yaw);
    }

    public Vec3 right() {
        return orientation.right(yaw);
    }

    public AABB bounds() {
        Vec3 right = right();
        Vec3 up = up();
        Vec3 normal = normal();
        double ex = Math.abs(right.x) * geometry.width() * 0.5
            + Math.abs(up.x) * geometry.height() * 0.5 + Math.abs(normal.x) * DEPTH * 0.5;
        double ey = Math.abs(right.y) * geometry.width() * 0.5
            + Math.abs(up.y) * geometry.height() * 0.5 + Math.abs(normal.y) * DEPTH * 0.5;
        double ez = Math.abs(right.z) * geometry.width() * 0.5
            + Math.abs(up.z) * geometry.height() * 0.5 + Math.abs(normal.z) * DEPTH * 0.5;
        return new AABB(center.x - ex, center.y - ey, center.z - ez,
            center.x + ex, center.y + ey, center.z + ez);
    }

    public double distanceToSqr(Vec3 point) {
        AABB box = bounds();
        double x = Math.max(box.minX, Math.min(point.x, box.maxX));
        double y = Math.max(box.minY, Math.min(point.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(point.z, box.maxZ));
        return point.distanceToSqr(x, y, z);
    }

    public boolean anchored() {
        return anchor != null && anchorFace != null;
    }
}
