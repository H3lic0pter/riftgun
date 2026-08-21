package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/** 26.1.2 entity-render-state adapter for relocation gate visuals. */
public final class EntityRelocationPortalRenderer extends EntityRenderer<EntityRelocationPortalEntity, PortalRenderState> {
    public EntityRelocationPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public PortalRenderState createRenderState() {
        return new PortalRenderState();
    }

    @Override
    public void extractRenderState(EntityRelocationPortalEntity entity, PortalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.portal = entity;
        state.partialTick = partialTicks;
    }

    @Override
    public void submit(PortalRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        PortalVisualType type = PortalVisualPreferences.selected();
        type.renderer().submit(new PortalVisualRenderContext(state.portal, state.partialTick,
            poseStack, collector, PortalRenderFrameState.current().surfaceRenderPath(),
            PortalVisualStyles.resolve(state.portal)));
        super.submit(state, poseStack, collector, camera);
    }
}
