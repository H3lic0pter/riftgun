package dev.riftgun.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;

/** Stable render types used only while a shader pack is active. */
final class PortalGunRenderTypes extends RenderType {
    private static final RenderType OPAQUE = create(
        "riftgun_portal_gun_opaque",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        1536,
        true,
        false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(new TextureStateShard(
                TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(NO_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(true)
    );

    private static final RenderType TRANSLUCENT = create(
        "riftgun_portal_gun_translucent",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        1536,
        true,
        false,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER)
            .setTextureState(new TextureStateShard(
                TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(true)
    );

    private PortalGunRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                                 int bufferSize, boolean affectsCrumbling,
                                 boolean sortOnUpload, Runnable setupState,
                                 Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload,
            setupState, clearState);
    }

    static RenderType opaque() {
        return OPAQUE;
    }

    static RenderType translucentLayer() {
        return TRANSLUCENT;
    }
}
