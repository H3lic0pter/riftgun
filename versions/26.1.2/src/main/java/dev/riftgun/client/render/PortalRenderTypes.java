package dev.riftgun.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.riftgun.RiftGun;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * 26.1.2 render pipelines and render types for portal surfaces.
 *
 * <p>The 1.21.x composite-state shaders were replaced by declarative render pipelines. The
 * portal pixelation shader survives as a custom pipeline ({@link Pipelines#PORTAL}); the swirl
 * surface and its additive glow now run on custom pipelines ({@link Pipelines#SWIRL} /
 * {@link Pipelines#SWIRL_GLOW}) that reproduce the 1.21.x shader-pack fallback look — a
 * rotating cutout disc — with the per-frame rotation baked into the lightmap attribute, and
 * the rim stays on the position-color {@link Pipelines#SWIRL_EDGE} pipeline.
 */
public final class PortalRenderTypes {
    private static final Identifier SWIRL_TEXTURE =
        Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/entity/portal_surface.png");
    private static final Identifier WHITE_TEXTURE =
        Identifier.withDefaultNamespace("textures/misc/white.png");

    private PortalRenderTypes() {}

    /** Registered into {@code RegisterRenderPipelinesEvent} by {@code ClientModEvents}. */
    public static final class Pipelines {
        public static final RenderPipeline PORTAL = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/rift_portal"))
            .withVertexShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

        /** Translucent position-color quads for the swirl rim, replacing the 1.21.x shader state. */
        public static final RenderPipeline SWIRL_EDGE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/rift_portal_swirl_edge"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

        /** Rotating cutout swirl disc: opaque, texture alpha trims the circle. */
        public static final RenderPipeline SWIRL = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/rift_portal_swirl"))
            .withVertexShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal_swirl"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal_swirl"))
            .withSampler("Sampler0")
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build();

        /** Additive swirl glow, same geometry and shader as the surface, brighter tint. */
        public static final RenderPipeline SWIRL_GLOW = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/rift_portal_swirl_glow"))
            .withVertexShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal_swirl"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal_swirl"))
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withCull(true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

        /**
         * Additive emissive swirl glow for the shader-pack fallback: vanilla core/entity is
         * rewritten by shader packs, so this layer stays visible (unlike the custom pipelines).
         */
        public static final RenderPipeline SWIRL_FALLBACK_GLOW = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/rift_portal_swirl_fallback_glow"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withShaderDefine("EMISSIVE")
            .withShaderDefine("NO_OVERLAY")
            .withShaderDefine("NO_CARDINAL_LIGHTING")
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .build();

        private Pipelines() {}
    }

    private static final RenderType PORTAL = RenderType.create(
        "rift_portal",
        RenderSetup.builder(Pipelines.PORTAL)
            .sortOnUpload()
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType BORDER = RenderType.create(
        "rift_portal_border",
        RenderSetup.builder(RenderPipelines.LINES_TRANSLUCENT)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType SWIRL_EDGE = RenderType.create(
        "rift_portal_swirl_edge",
        RenderSetup.builder(Pipelines.SWIRL_EDGE)
            .sortOnUpload()
            .bufferSize(4096)
            .createRenderSetup()
    );

    private static final RenderType SWIRL = RenderType.create(
        "rift_portal_swirl",
        RenderSetup.builder(Pipelines.SWIRL)
            .withTexture("Sampler0", SWIRL_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .sortOnUpload()
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType SWIRL_GLOW = RenderType.create(
        "rift_portal_swirl_glow",
        RenderSetup.builder(Pipelines.SWIRL_GLOW)
            .withTexture("Sampler0", SWIRL_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .sortOnUpload()
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType SWIRL_FALLBACK_GLOW = RenderType.create(
        "rift_portal_swirl_fallback_glow",
        RenderSetup.builder(Pipelines.SWIRL_FALLBACK_GLOW)
            .withTexture("Sampler0", SWIRL_TEXTURE)
            .bufferSize(4096)
            .createRenderSetup()
    );

    public static RenderType portal() {
        return PORTAL;
    }

    public static RenderType border() {
        return BORDER;
    }

    public static RenderType swirlEdge() {
        return SWIRL_EDGE;
    }

    public static RenderType classicFallback() {
        return RenderTypes.entityTranslucent(WHITE_TEXTURE);
    }

    public static RenderType swirl() {
        return SWIRL;
    }

    public static RenderType swirlGlow() {
        return SWIRL_GLOW;
    }

    public static RenderType swirlFallback() {
        return RenderTypes.entityCutout(SWIRL_TEXTURE);
    }

    public static RenderType swirlFallbackGlow() {
        return SWIRL_FALLBACK_GLOW;
    }
}
