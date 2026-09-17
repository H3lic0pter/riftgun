package dev.riftgun.core.particle;

/** Immutable lifetime animation. Size is quad half-width; spin is radians per tick.
 * Gravity uses vanilla's 0.04 blocks/tick² multiplier, and drag multiplies velocity each tick. */
public record ParticleDynamics(int lifetimeTicks, float startSize, float endSize,
                               int startArgb, int endArgb, float gravity, float drag,
                               float spin, boolean collision, boolean fullBright) {
    public ParticleDynamics {
        if (lifetimeTicks < 1 || lifetimeTicks > 1200) {
            throw new IllegalArgumentException("Particle lifetime must be 1..1200 ticks");
        }
        if (!Float.isFinite(startSize) || !Float.isFinite(endSize) || startSize < 0 || endSize < 0
                || !Float.isFinite(gravity) || !Float.isFinite(drag) || drag < 0 || drag > 1
                || !Float.isFinite(spin)) {
            throw new IllegalArgumentException("Invalid particle dynamics");
        }
    }

    public float size(float age) {
        return startSize + (endSize - startSize) * progress(age);
    }

    public int color(float age) {
        float t = progress(age);
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int start = (startArgb >>> shift) & 255;
            int end = (endArgb >>> shift) & 255;
            result |= Math.round(start + (end - start) * t) << shift;
        }
        return result;
    }

    private float progress(float age) {
        return Math.max(0, Math.min(1, age / lifetimeTicks));
    }
}
