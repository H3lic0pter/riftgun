package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.RiftGun;
import dev.riftgun.portal.PortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Routes portal body rendering through the client-local visual type registry. */
public final class PortalRenderer extends EntityRenderer<PortalEntity> {
    private static final ResourceLocation EMPTY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/misc/empty.png");

    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity entity) {
        return EMPTY_TEXTURE;
    }

    @Override
    public void render(PortalEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        PortalVisualType type = PortalVisualPreferences.selected();
        type.renderer().render(new PortalVisualRenderContext(entity, partialTick, poseStack, buffers,
            packedLight, PortalRenderFrameState.current().surfaceRenderPath(),
            PortalVisualStyles.resolve(entity)));
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }
}
