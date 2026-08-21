package dev.riftgun.service;

import java.util.function.DoubleSupplier;

/** Area-uniform sampling in an annulus around the player. */
public final class RandomRiftGeometry {
    public static Offset sample(int minimumRadius, int maximumRadius, DoubleSupplier random) {
        double inner = Math.min(minimumRadius, maximumRadius);
        double outer = Math.max(minimumRadius, maximumRadius);
        double angle = random.getAsDouble() * Math.PI * 2.0;
        double radius = Math.sqrt(random.getAsDouble()
            * (outer * outer - inner * inner) + inner * inner);
        return new Offset(Math.cos(angle) * radius, Math.sin(angle) * radius);
    }

    public record Offset(double x, double z) {}

    private RandomRiftGeometry() {}
}
