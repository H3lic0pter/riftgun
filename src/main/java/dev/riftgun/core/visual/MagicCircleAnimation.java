package dev.riftgun.core.visual;

import dev.riftgun.portal.PortalLifecycle;

/** Stateless phase animation; all layers retain their shared full-canvas origin. */
public final class MagicCircleAnimation {
    public static Frame frame(PortalLifecycle.Phase phase, float phaseTicks, float visibleProgress) {
        float progress = clamp(visibleProgress);
        return switch (phase) {
            case CHARGING -> {
                float charge = clamp(phaseTicks / PortalLifecycle.CHARGE_TICKS);
                float eased = smooth(charge);
                yield new Frame(0.9F + 0.1F * eased, eased, 1.0F,
                    Math.min(4, (int) (charge * 5)), 0.0F, 0.0F);
            }
            case OPENING -> new Frame(1.0F, 1.0F, 1.0F, 4,
                grow(progress / 0.75F), grow((progress - 0.25F) / 0.75F));
            case OPEN -> new Frame(1.0F, 1.0F, 1.0F, 4, 1.0F, 1.0F);
            case CLOSING -> {
                float closing = smooth(1.0F - progress);
                yield new Frame(1.0F + 0.2F * closing, 1.0F - closing,
                    1.0F - closing, 4, 1.0F, 1.0F);
            }
            case CLOSED -> new Frame(1.2F, 0.0F, 0.0F, 4, 1.0F, 1.0F);
        };
    }

    public static float rotation(float ticks, double periodSeconds, boolean counterclockwise) {
        double turns = Math.max(0.0, ticks) / (20.0 * Math.max(2.0, periodSeconds));
        return (float) ((turns % 1.0) * Math.PI * 2.0 * (counterclockwise ? -1.0 : 1.0));
    }

    private static float grow(float progress) {
        float remaining = 1.0F - clamp(progress);
        return 1.0F - remaining * remaining * remaining;
    }

    private static float smooth(float progress) { return progress * progress * (3.0F - 2.0F * progress); }
    private static float clamp(float value) { return Math.max(0.0F, Math.min(1.0F, value)); }

    public record Frame(float scale, float outerAlpha, float alpha, int triangle,
                        float inner1Scale, float inner2Scale) {}

    private MagicCircleAnimation() {}
}
