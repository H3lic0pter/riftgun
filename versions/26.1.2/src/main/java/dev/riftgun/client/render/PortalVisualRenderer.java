package dev.riftgun.client.render;

@FunctionalInterface
public interface PortalVisualRenderer {
    default boolean usesSplashParticles() { return true; }
    void submit(PortalVisualRenderContext context);
}
