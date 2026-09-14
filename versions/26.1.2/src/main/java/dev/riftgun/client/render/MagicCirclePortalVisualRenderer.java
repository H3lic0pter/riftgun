package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.visual.MagicCircleAnimation;
import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import net.minecraft.util.LightCoordsUtil;

/** Full-canvas layers preserve the supplied registration and transparent margins. */
final class MagicCirclePortalVisualRenderer implements PortalVisualRenderer {
    @Override public boolean usesSplashParticles() { return false; }
    private static final String[] TEXTURES = {"outer", "tri1", "tri2", "tri3", "tri4", "tri5", "inner1", "inner2"};
    private static final class Materials {
        private static final RenderType[] BODY = materials(false, false);
        private static final RenderType[] GLOW = materials(true, false);
    }
    private static final class ShaderMaterials {
        private static final RenderType[] BODY = materials(false, true);
    }
    private static final float GLOW_UV_OFFSET = 1.5F / 1024.0F;
    private static final float[] CORNER_X = {-1, -1, 1, 1};
    private static final float[] CORNER_Y = {1, -1, -1, 1};

    private static RenderType[] materials(boolean glow, boolean shaderPack) {
        RenderType[] materials = new RenderType[TEXTURES.length];
        for (int index = 0; index < TEXTURES.length; index++) {
            var texture = Identifier.fromNamespaceAndPath(
                "riftgun", "textures/entity/magic_circle/" + TEXTURES[index] + ".png");
            materials[index] = shaderPack ? PortalRenderTypes.magicCircleShaderLayer(texture)
                : PortalRenderTypes.magicCircleLayer(texture, glow);
        }
        return materials;
    }

    @Override
    public void submit(PortalVisualRenderContext context) {
        if (context.surfaceRenderPath() == PortalSurfaceRenderPath.SKIP_SURFACE) return;
        var portal = context.portal();
        var phase = portal.phase();
        if (phase == PortalLifecycle.Phase.CLOSED) return;
        var frame = MagicCircleAnimation.frame(phase, portal.phaseTicks() + context.partialTick(),
            context.visibleProgress());
        var config = RiftConfigs.client();
        // Outer layers start as charging ends and keep the same clock through opening.
        float outerRotationTicks = phase == PortalLifecycle.Phase.CHARGING ? 0.0F
            : Math.max(0.0F, context.age() - PortalLifecycle.CHARGE_TICKS);
        float innerRotationTicks = phase == PortalLifecycle.Phase.OPEN || phase == PortalLifecycle.Phase.CLOSING
            ? Math.max(0.0F, context.age() - PortalLifecycle.CHARGE_TICKS - portal.openingTicks()) : 0.0F;
        float outer = MagicCircleAnimation.rotation(outerRotationTicks, config.magicOuterPeriod(), config.magicOuterCounterclockwise());
        float inner = MagicCircleAnimation.rotation(innerRotationTicks, config.magicInnerPeriod(), config.magicInnerCounterclockwise());
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        drawLayer(context, basis, 0, frame.scale(), frame.outerAlpha(), outer);
        drawLayer(context, basis, frame.triangle() + 1, frame.scale(), frame.alpha(), outer);
        drawLayer(context, basis, 6, frame.scale() * frame.inner1Scale(), frame.alpha(), inner);
        drawLayer(context, basis, 7, frame.scale() * frame.inner2Scale(), frame.alpha(), inner);
    }

    private static void drawLayer(PortalVisualRenderContext context, PortalRenderBasis basis,
                                   int layer, float scale, float alpha, float rotation) {
        if (scale <= 0.0F || alpha <= 0.0F) return;
        float width = context.portal().portalWidth() * scale;
        float height = context.portal().portalHeight() * scale;
        int color = context.style().splashRgb();
        double cosine = Math.cos(rotation);
        double sine = Math.sin(rotation);
        if (context.surfaceRenderPath() == PortalSurfaceRenderPath.VANILLA_FALLBACK) {
            // Vanilla emissive material lets Iris supply its lighting/bloom. It has no culling,
            // so emit only the viewer-facing side to avoid blending transparent layers twice.
            var camera = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().position();
            int face = camera.subtract(context.portal().placement().center()).dot(basis.normal()) < 0 ? -1 : 1;
            context.submit(ShaderMaterials.BODY[layer], (pose, vertices) ->
                draw(pose.pose(), vertices, basis, width, height, color, alpha, cosine, sine, false, face));
            return;
        }
        context.submit(Materials.BODY[layer], (pose, vertices) ->
            draw(pose.pose(), vertices, basis, width, height, color, alpha, cosine, sine, false, 0));
        context.submit(Materials.GLOW[layer], (pose, vertices) ->
            draw(pose.pose(), vertices, basis, width, height, color, alpha, cosine, sine, true, 0));
    }

    private static void draw(Matrix4f matrix, VertexConsumer vertices, PortalRenderBasis basis,
                              float width, float height, int color, float alpha, double cosine, double sine,
                              boolean glow, int visibleFace) {
        // Four bilinear taps produce a restrained halo without modifying or regenerating the artwork.
        for (int tap = 0; tap < (glow ? 4 : 1); tap++) {
            float offsetU = glow ? ((tap & 1) == 0 ? -GLOW_UV_OFFSET : GLOW_UV_OFFSET) : 0.0F;
            float offsetV = glow ? ((tap & 2) == 0 ? -GLOW_UV_OFFSET : GLOW_UV_OFFSET) : 0.0F;
            float brightness = glow ? 0.13F : 1.0F;
            float red = (((color >> 16) & 255) / 255.0F * 0.75F + 0.25F) * brightness;
            float green = (((color >> 8) & 255) / 255.0F * 0.75F + 0.25F) * brightness;
            float blue = ((color & 255) / 255.0F * 0.75F + 0.25F) * brightness;
            for (int face = -1; face <= 1; face += 2) {
                if (visibleFace != 0 && face != visibleFace) continue;
                for (int corner = 0; corner < 4; corner++) {
                    float localX = CORNER_X[corner];
                    float localY = CORNER_Y[corner];
                    float x = (float) (cosine * localX + sine * localY) * width * 0.5F * face;
                    float y = (float) (-sine * localX + cosine * localY) * height * 0.5F;
                    float z = 0.004F * face;
                    float du = CORNER_X[corner] * 0.5F;
                    float dv = -CORNER_Y[corner] * 0.5F;
                    float u = 0.5F + du + offsetU;
                    float v = 0.5F + dv + offsetV;
                    vertices.addVertex(matrix,
                            (float) (basis.right().x * x + basis.up().x * y + basis.normal().x * z),
                            (float) (basis.right().y * x + basis.up().y * y + basis.normal().y * z),
                            (float) (basis.right().z * x + basis.up().z * y + basis.normal().z * z))
                        .setColor(red, green, blue, alpha)
                        .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT)
                        .setNormal((float) basis.normal().x * face, (float) basis.normal().y * face,
                            (float) basis.normal().z * face);
                }
            }
        }
    }
}
