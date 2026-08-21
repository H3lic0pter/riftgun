package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.RiftGun;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class EntityRelocationPortalRenderer extends EntityRenderer<EntityRelocationPortalEntity> {
    private static final ResourceLocation EMPTY_TEXTURE = ResourceLocation.fromNamespaceAndPath(
        RiftGun.MOD_ID, "textures/misc/empty.png");

    public EntityRelocationPortalRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(EntityRelocationPortalEntity entity) {
        return EMPTY_TEXTURE;
    }

    @Override
    public void render(EntityRelocationPortalEntity entity, float yaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int packedLight) {
        PortalVisualType type = PortalVisualPreferences.selected();
        type.renderer().render(new PortalVisualRenderContext(entity, partialTick, poses, buffers,
            packedLight, PortalRenderFrameState.current().surfaceRenderPath(),
            PortalVisualStyles.resolve(entity)));
        super.render(entity, yaw, partialTick, poses, buffers, packedLight);
    }
}
