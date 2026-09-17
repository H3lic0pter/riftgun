package dev.riftgun.client.render;

@FunctionalInterface
public interface PortalVisualRenderer {
    default boolean usesSplashParticles() { return true; }
    default String particleEffectId() {
        return usesSplashParticles() ? dev.riftgun.client.particle.ParticleEffectRegistry.PORTAL_SPLASH
            : dev.riftgun.client.particle.ParticleEffectRegistry.NONE;
    }
    void submit(PortalVisualRenderContext context);
}
