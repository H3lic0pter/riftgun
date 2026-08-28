package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.portal.PortalEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** Routes portal body rendering through the client-local visual type registry. */
public final class PortalRenderer extends EntityRenderer<PortalEntity, PortalRenderState> {
    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(PortalEntity entity, PortalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.portal = entity;
        state.partialTick = partialTicks;
    }

    @Override
    public void submit(PortalRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.portal instanceof PortalEntity entity && entity.pairingDormant()) return;
        PortalVisualType type = PortalVisualPreferences.selected();
        PortalVisualStyle style = PortalVisualStyles.resolve(state.portal);
        PortalVisualRenderContext context = new PortalVisualRenderContext(state.portal, state.partialTick,
            poseStack, collector, PortalRenderFrameState.current().surfaceRenderPath(),
            style);
        type.renderer().submit(context);
        super.submit(state, poseStack, collector, camera);
    }
}
