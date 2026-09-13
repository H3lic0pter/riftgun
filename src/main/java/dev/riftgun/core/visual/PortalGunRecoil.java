package dev.riftgun.core.visual;

import dev.riftgun.core.config.GunRecoilConfig;

/** A bounded, continuous shot envelope. Callers supply monotonic time in nanoseconds. */
public final class PortalGunRecoil {
    private long kickNanos;
    private long recoveryNanos;
    private long startedAt;
    private double start;
    private double peak;
    private boolean active;

    public void fire(long now, GunRecoilConfig parameters) {
        start = sample(now);
        peak = Math.min(1.0, start + parameters.shotStrength());
        // Capture timing per shot so a config reload cannot reshape an in-flight animation.
        kickNanos = parameters.kickMillis() * 1_000_000L;
        recoveryNanos = parameters.recoveryMillis() * 1_000_000L;
        startedAt = now;
        active = true;
    }

    public double sample(long now) {
        if (!active) return 0.0;
        long elapsed = Math.max(0L, now - startedAt);
        if (elapsed >= kickNanos + recoveryNanos) return 0.0;
        if (elapsed < kickNanos) {
            double progress = smoothstep((double) elapsed / kickNanos);
            return start + (peak - start) * progress;
        }
        double progress = smoothstep((double) (elapsed - kickNanos) / recoveryNanos);
        return peak * (1.0 - progress);
    }

    public void reset() {
        active = false;
        start = 0.0;
        peak = 0.0;
    }

    private static double smoothstep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }
}
