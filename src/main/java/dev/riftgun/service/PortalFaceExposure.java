package dev.riftgun.service;

import dev.riftgun.portal.PortalPlacement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

final class PortalFaceExposure {
    private static final int SAMPLES_PER_BLOCK = 16;
    private static final double FACE_OFFSET = PortalPlacement.DEPTH * 0.5;
    private static final double INSIDE_EPSILON = 1.0E-5;

    private PortalFaceExposure() {}

    static boolean hasMinimumExposure(ServerLevel level, PortalPlacement placement,
                                      double minimumExposedFraction) {
        List<AABB> collisionBoxes = new ArrayList<>();
        for (VoxelShape shape : level.getBlockCollisions(null, placement.bounds().inflate(0.002))) {
            collisionBoxes.addAll(shape.toAabbs());
        }
        return hasMinimumExposure(placement, collisionBoxes, minimumExposedFraction);
    }

    static boolean hasMinimumExposure(PortalPlacement placement, List<AABB> collisionBoxes,
                                      double minimumExposedFraction) {
        double required = Math.clamp(minimumExposedFraction, 0.0, 1.0);
        if (required <= 0.0 || collisionBoxes.isEmpty()) return true;

        int columns = Math.max(1, (int) Math.ceil(placement.geometry().width() * SAMPLES_PER_BLOCK));
        int rows = Math.max(1, (int) Math.ceil(placement.geometry().height() * SAMPLES_PER_BLOCK));
        int total = columns * rows;
        int requiredExposed = (int) Math.ceil(required * total - 1.0E-9);
        int exposed = 0;
        int tested = 0;
        Vec3 faceCenter = placement.center().add(placement.normal().scale(FACE_OFFSET));
        Vec3 up = placement.up();
        Vec3 right = placement.right();

        for (int row = 0; row < rows; row++) {
            double upOffset = sampleOffset(row, rows, placement.geometry().height());
            for (int column = 0; column < columns; column++) {
                double rightOffset = sampleOffset(column, columns, placement.geometry().width());
                Vec3 point = faceCenter
                    .add(up.scale(upOffset))
                    .add(right.scale(rightOffset));
                tested++;
                if (!insideAny(point, collisionBoxes)) exposed++;

                if (exposed >= requiredExposed) return true;
                if (exposed + total - tested < requiredExposed) return false;
            }
        }
        return exposed >= requiredExposed;
    }

    private static double sampleOffset(int index, int count, double size) {
        return ((index + 0.5) / count - 0.5) * size;
    }

    private static boolean insideAny(Vec3 point, List<AABB> boxes) {
        for (AABB box : boxes) {
            if (point.x > box.minX + INSIDE_EPSILON && point.x < box.maxX - INSIDE_EPSILON
                && point.y > box.minY + INSIDE_EPSILON && point.y < box.maxY - INSIDE_EPSILON
                && point.z > box.minZ + INSIDE_EPSILON && point.z < box.maxZ - INSIDE_EPSILON) {
                return true;
            }
        }
        return false;
    }
}
