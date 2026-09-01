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
 * surface and restrained glow run on custom {@link Pipelines#SWIRL} and
 * {@link Pipelines#SWIRL_GLOW} pipelines, with per-frame rotation baked into the lightmap
 * attribute, while the rim stays on the position-color {@link Pipelines#SWIRL_EDGE} pipeline.
 */
public final class PortalRenderTypes {
    private static final Identifier SWIRL_TEXTURE =
        Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/entity/portal_surface.png");
    private static final Identifier WHITE_TEXTURE =
        Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/misc/white.png");
    private static final Identifier ENDFRAME_FRAME_TEXTURE =
        Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/entity/portal_frame.png");
    private static final Identifier END_SKY_LOCATION =
        Identifier.withDefaultNamespace("textures/environment/end_sky.png");
    private static final Identifier END_PORTAL_LOCATION =
        Identifier.withDefaultNamespace("textures/entity/end_portal/end_portal.png");

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

        /** Rotating swirl material under a fixed aperture: opaque inside, feathered at the rim. */
        public static final RenderPipeline SWIRL = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/rift_portal_swirl"))
            .withVertexShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal_swirl"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_portal_swirl"))
            .withSampler("Sampler0")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(true)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
            .build();

        /** Low-intensity additive mask over the custom swirl surface. */
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

        /** Vanilla end-portal star without face culling, so both portal faces
         *  render regardless of vertex winding (the vanilla end portal culls). */
        public static final RenderPipeline ENDFRAME_STAR = RenderPipeline.builder(
                RenderPipelines.MATRICES_PROJECTION_SNIPPET,
                RenderPipelines.FOG_SNIPPET,
                RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/riftgun_endframe_star"))
            .withVertexShader("core/rendertype_end_portal")
            .withFragmentShader("core/rendertype_end_portal")
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .withShaderDefine("PORTAL_LAYERS", 15)
            .build();

        /** Depth-writing cutout end-frame liquid. The node-specific fragment
         *  shader discards transparent texels so the native star remains visible. */
        public static final RenderPipeline ENDFRAME_FRAME = RenderPipeline.builder(
                RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "pipeline/riftgun_endframe_frame"))
            .withVertexShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_endframe"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "core/rendertype_rift_endframe"))
            .withSampler("Sampler0")
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
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
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType SWIRL_GLOW = RenderType.create(
        "rift_portal_swirl_glow",
        RenderSetup.builder(Pipelines.SWIRL_GLOW)
            .withTexture("Sampler0", SWIRL_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
            .sortOnUpload()
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType SWIRL_FALLBACK_GLOW = RenderType.create(
        "rift_portal_swirl_fallback_glow",
        RenderSetup.builder(Pipelines.SWIRL_FALLBACK_GLOW)
            .withTexture("Sampler0", SWIRL_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType SWIRL_FALLBACK = RenderType.create(
        "rift_portal_swirl_fallback",
        RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT)
            .withTexture("Sampler0", SWIRL_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
            .bufferSize(256)
            .createRenderSetup()
    );

    /** View-offset cutout keeps the liquid ring in front of shader-native end-portal material. */
    private static final RenderType ENDFRAME_FALLBACK = RenderType.create(
        "riftgun_endframe_fallback",
        RenderSetup.builder(RenderPipelines.ENTITY_CUTOUT_Z_OFFSET)
            .withTexture("Sampler0", ENDFRAME_FRAME_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType ENDFRAME_FALLBACK_GLOW = RenderType.create(
        "riftgun_endframe_fallback_glow",
        RenderSetup.builder(Pipelines.SWIRL_FALLBACK_GLOW)
            .withTexture("Sampler0", ENDFRAME_FRAME_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            .bufferSize(256)
            .createRenderSetup()
    );

    private static final RenderType ENDFRAME_STAR = RenderType.create(
        "riftgun_endframe_star",
        RenderSetup.builder(Pipelines.ENDFRAME_STAR)
            .withTexture("Sampler0", END_SKY_LOCATION)
            .withTexture("Sampler1", END_PORTAL_LOCATION)
            .bufferSize(1536)
            .createRenderSetup()
    );

    private static final RenderType ENDFRAME_FRAME = RenderType.create(
        "riftgun_endframe_frame",
        RenderSetup.builder(Pipelines.ENDFRAME_FRAME)
            .withTexture("Sampler0", ENDFRAME_FRAME_TEXTURE,
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
            .bufferSize(256)
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
        return SWIRL_FALLBACK;
    }

    public static RenderType endframeFrame() {
        return ENDFRAME_FALLBACK;
    }

    public static RenderType endframeFrameRotating() {
        return ENDFRAME_FRAME;
    }

    public static RenderType endframeStar(PortalSurfaceRenderPath path) {
        return path == PortalSurfaceRenderPath.CUSTOM ? ENDFRAME_STAR : RenderTypes.endPortal();
    }

    /** Iris replaces its own end-portal renderer with this full entity pipeline under shaders. */
    public static RenderType endframeNativeShaderCenter() {
        return RenderTypes.entitySolid(END_PORTAL_LOCATION);
    }

    public static RenderType endframeFrameGlow() {
        return ENDFRAME_FALLBACK_GLOW;
    }

    public static RenderType swirlFallbackGlow() {
        return SWIRL_FALLBACK_GLOW;
    }
}
