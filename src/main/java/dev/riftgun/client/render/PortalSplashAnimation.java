package dev.riftgun.client.render;

import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.util.Mth;

final class PortalSplashAnimation {
    static Frame sample(PortalLifecycle.Phase phase, int phaseTicks, float partialTick) {
        return switch (phase) {
            case CHARGING -> openingFrame((phaseTicks + partialTick + 1.0F)
                / (PortalLifecycle.CHARGE_TICKS + PortalLifecycle.ANIMATION_TICKS));
            case OPENING -> openingFrame((PortalLifecycle.CHARGE_TICKS + phaseTicks + partialTick + 1.0F)
                / (PortalLifecycle.CHARGE_TICKS + PortalLifecycle.ANIMATION_TICKS));
            case CLOSING -> closingFrame((phaseTicks + partialTick + 1.0F)
                / PortalLifecycle.ANIMATION_TICKS);
            case OPEN, CLOSED -> Frame.HIDDEN;
        };
    }

    private static Frame openingFrame(float rawProgress) {
        float progress = Mth.clamp(rawProgress, 0.0F, 1.0F);
        float pulse = Mth.sin(progress * Mth.PI);
        return new Frame(true, true, progress, 0.45F + pulse * 0.45F,
            0.18F + pulse * 0.16F, 0.08F + progress * 0.28F, 16);
    }

    private static Frame closingFrame(float rawProgress) {
        float progress = Mth.clamp(rawProgress, 0.0F, 1.0F);
        float pulse = Mth.sin(progress * Mth.PI);
        return new Frame(true, false, progress, 0.30F + pulse * 0.35F,
            0.14F + pulse * 0.10F, 0.05F + progress * 0.12F, 10);
    }

    record Frame(boolean visible, boolean outward, float progress, float alpha,
                 float dropletLength, float travel, int droplets) {
        private static final Frame HIDDEN = new Frame(false, true, 0.0F, 0.0F, 0.0F, 0.0F, 0);
    }

    private PortalSplashAnimation() {}
}
