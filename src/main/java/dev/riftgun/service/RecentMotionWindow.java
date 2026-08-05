package dev.riftgun.service;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Fixed-size position history with a newest-biased velocity estimate. */
final class RecentMotionWindow {
    static final int CAPACITY = 4;

    private final double[] x = new double[CAPACITY];
    private final double[] y = new double[CAPACITY];
    private final double[] z = new double[CAPACITY];
    private final long[] ticks = new long[CAPACITY];
    private int next;
    private int size;
    private ResourceKey<Level> dimension;

    void record(ResourceKey<Level> nextDimension, Vec3 position, long tick,
                double teleportDistancePerTick) {
        if (dimension != null && !dimension.equals(nextDimension)) clear();
        dimension = nextDimension;

        if (size > 0) {
            int latest = index(size - 1);
            long elapsed = tick - ticks[latest];
            if (elapsed < 0) {
                clear();
                dimension = nextDimension;
            } else if (elapsed == 0) {
                write(latest, position, tick);
                return;
            } else if (distance(latest, position) > Math.max(0.0, teleportDistancePerTick) * elapsed) {
                clear();
                dimension = nextDimension;
            }
        }

        write(next, position, tick);
        next = (next + 1) % CAPACITY;
        if (size < CAPACITY) size++;
    }

    Optional<Vec3> estimatedVelocity() {
        if (size < 2) return Optional.empty();
        double vx = 0.0;
        double vy = 0.0;
        double vz = 0.0;
        double weights = 0.0;
        for (int sample = 1; sample < size; sample++) {
            int previous = index(sample - 1);
            int current = index(sample);
            long elapsed = ticks[current] - ticks[previous];
            if (elapsed <= 0) continue;
            double weight = sample;
            vx += (x[current] - x[previous]) / elapsed * weight;
            vy += (y[current] - y[previous]) / elapsed * weight;
            vz += (z[current] - z[previous]) / elapsed * weight;
            weights += weight;
        }
        return weights == 0.0 ? Optional.empty() : Optional.of(new Vec3(
            vx / weights, vy / weights, vz / weights));
    }

    void clear() {
        next = 0;
        size = 0;
        dimension = null;
    }

    int size() {
        return size;
    }

    private int index(int chronologicalIndex) {
        int oldest = (next - size + CAPACITY) % CAPACITY;
        return (oldest + chronologicalIndex) % CAPACITY;
    }

    private void write(int index, Vec3 position, long tick) {
        x[index] = position.x;
        y[index] = position.y;
        z[index] = position.z;
        ticks[index] = tick;
    }

    private double distance(int index, Vec3 position) {
        double dx = position.x - x[index];
        double dy = position.y - y[index];
        double dz = position.z - z[index];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
