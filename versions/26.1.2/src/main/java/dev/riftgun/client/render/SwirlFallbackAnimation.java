package dev.riftgun.client.render;

final class SwirlFallbackAnimation {
    private static final double TAU = Math.PI * 2.0;
    private static final float TICKS_PER_SECOND = 20.0F;

    static Uv rotate(float u, float v, float ageTicks, float periodSeconds,
                     float phase, boolean animated) {
        return rotate(u, v, ageTicks, periodSeconds, phase, animated, 1.0F);
    }

    static Uv rotate(float u, float v, float ageTicks, float periodSeconds,
                     float phase, boolean animated, float rotationDirection) {
        if (!animated) return new Uv(u, v);

        double angle = ageTicks / (Math.max(periodSeconds, 0.1F) * TICKS_PER_SECOND) * TAU * rotationDirection
            + phase * TAU;
        double cosine = Math.cos(angle);
        double sine = Math.sin(angle);
        double centeredU = u - 0.5;
        double centeredV = v - 0.5;
        return new Uv(
            (float) (0.5 + centeredU * cosine - centeredV * sine),
            (float) (0.5 + centeredU * sine + centeredV * cosine)
        );
    }

    record Uv(float u, float v) {}

    private SwirlFallbackAnimation() {}
}
