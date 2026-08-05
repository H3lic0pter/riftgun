package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.riftgun.RiftGun;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public final class PortalRenderTypes extends RenderType {
    private static final ResourceLocation SWIRL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/entity/portal_surface.png");
    private static ShaderInstance portalShader;
    private static ShaderInstance swirlShader;

    private static final RenderType PORTAL = create(
        "rift_portal",
        DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setLayeringState(NO_LAYERING)
            .setShaderState(new ShaderStateShard(() -> portalShader))
            .setOutputState(PARTICLES_TARGET)
            .createCompositeState(true)
    );

    private static final RenderType BORDER = create(
        "rift_portal_border",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        256,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(OptionalDouble.of(3.0)))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .createCompositeState(false)
    );

    private static final RenderType SWIRL = create(
        "rift_portal_swirl",
        DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        CompositeState.builder()
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(CULL)
            .setLayeringState(NO_LAYERING)
            .setShaderState(new ShaderStateShard(() -> swirlShader))
            .setTextureState(new TextureStateShard(SWIRL_TEXTURE, false, false))
            .setOutputState(PARTICLES_TARGET)
            .createCompositeState(true)
    );

    private PortalRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                              boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState,
                              Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType portal() {
        return PORTAL;
    }

    public static RenderType border() {
        return BORDER;
    }

    public static RenderType swirl() {
        return SWIRL;
    }

    public static void setPortalShader(ShaderInstance shader) {
        portalShader = shader;
    }

    public static void setSwirlShader(ShaderInstance shader) {
        swirlShader = shader;
    }
}
