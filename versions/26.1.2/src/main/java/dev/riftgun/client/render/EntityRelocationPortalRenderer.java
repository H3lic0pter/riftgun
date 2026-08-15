package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.RiftGun;
import dev.riftgun.relocation.EntityRelocationPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

public final class EntityRelocationPortalRenderer extends EntityRenderer<EntityRelocationPortalEntity> {
    private static final Identifier EMPTY_TEXTURE = Identifier.fromNamespaceAndPath(
        RiftGun.MOD_ID, "textures/misc/empty.png");

    public EntityRelocationPortalRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public Identifier getTextureLocation(EntityRelocationPortalEntity entity) {
        return EMPTY_TEXTURE;
    }

    @Override
    public void render(EntityRelocationPortalEntity entity, float yaw, float partialTick,
                       PoseStack poses, MultiBufferSource buffers, int packedLight) {
        PortalVisualType type = PortalVisualPreferences.selected();
        type.renderer().render(new PortalVisualRenderContext(entity, partialTick, poses, buffers,
            packedLight, PortalVisualStyles.resolve(entity)));
        super.render(entity, yaw, partialTick, poses, buffers, packedLight);
    }
}
