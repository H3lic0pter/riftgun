package dev.riftgun.client.render;

import dev.riftgun.portal.PortalVisualSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Extracted portal state carried from {@code extractRenderState} into {@code submit}. */
public final class PortalRenderState extends EntityRenderState {
    public PortalVisualSource portal;
    public float partialTick;
}
