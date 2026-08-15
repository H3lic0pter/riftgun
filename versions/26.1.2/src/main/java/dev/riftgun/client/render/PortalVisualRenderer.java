package dev.riftgun.client.render;

@FunctionalInterface
public interface PortalVisualRenderer {
    void submit(PortalVisualRenderContext context);
}
