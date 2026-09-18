package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.core.visual.WaterSplashGeometry;
import dev.riftgun.client.particle.ParticleEffectRegistry;
import net.minecraft.util.LightCoordsUtil;
import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/** Fuel-colored liquid with native animated water detail and a gently moving geometric rim. */
final class WaterSplashPortalVisualRenderer implements PortalVisualRenderer {
    private static final Identifier WATER_STILL = Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("riftgun", "textures/misc/water_splash_base.png");
    private static final float SURFACE_OFFSET = 0.006F;
    // Keep sampling inside the live atlas sprite, including the small rim waves.
    private static final float UV_RADIUS = 0.48F;

    @Override public String particleEffectId() { return ParticleEffectRegistry.WATER_SPLASH; }

    @Override
    public void submit(PortalVisualRenderContext context) {
        if (context.surfaceRenderPath() == PortalSurfaceRenderPath.SKIP_SURFACE) return;
        var portal = context.portal();
        var phase = portal.phase();
        if (phase == PortalLifecycle.Phase.CHARGING || phase == PortalLifecycle.Phase.CLOSED) return;
        float phaseAge = portal.phaseTicks() + context.partialTick();
        float openingAge = phase == PortalLifecycle.Phase.OPENING ? phaseAge
            : portal.openingTicks() + phaseAge;
        float progress = phase == PortalLifecycle.Phase.CLOSING ? context.visibleProgress()
            : Mth.clamp(openingAge / WaterSplashGeometry.OPENING_TICKS, 0.0F, 1.0F);
        if (progress <= 0.0F) return;
        float scale = progress * progress * (3.0F - 2.0F * progress);
        float age = context.age();
        // Circular water disc; this only changes the visual, not the transit aperture.
        float radius = Math.max(portal.portalWidth(), portal.portalHeight()) * 0.5F * scale;
        var mesh = WaterSplashGeometry.forPortal(portal.visualId());
        var basis = PortalRenderBasis.from(portal);
        var client = Minecraft.getInstance();
        // Resolve the live sprite each frame so resource-pack reloads replace the material safely.
        TextureAtlasSprite sprite = client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(WATER_STILL);
        var camera = client.gameRenderer.getMainCamera().position();
        int face = camera.subtract(portal.placement().center()).dot(basis.normal()) < 0 ? -1 : 1;
        int color = context.style().splashRgb();
        context.submit(RenderTypes.entityCutout(BASE), (pose, vertices) ->
            draw(pose, vertices, basis, mesh, null, radius, color, face, age));
        context.submit(RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS, false), (pose, vertices) ->
            draw(pose, vertices, basis, mesh, sprite, radius, color, face, age));
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer vertices, PortalRenderBasis basis,
                             WaterSplashGeometry.Mesh mesh, TextureAtlasSprite sprite, float radius,
                             int color, int face, float age) {
        boolean water = sprite != null;
        float depth = SURFACE_OFFSET + (water ? 0.002F : 0);
        for (int i = 0; i < mesh.vertexCount(); i++) {
            float wave = mesh.vertexWave(i, age);
            float x = mesh.x(i) * wave;
            float y = mesh.y(i) * wave;
            // UVs stay anchored; only Minecraft's native water animation changes the texture.
            float u = water ? sprite.getU(0.5F + x * UV_RADIUS) : 0.5F;
            float v = water ? sprite.getV(0.5F - y * UV_RADIUS) : 0.5F;
            vertex(pose, vertices, basis, x * radius, y * radius, depth, face,
                color, water ? 85 : 255, u, v);
        }
        if (water) return;
        // Opaque, narrow highlight arcs share the backing material and depth buffer.
        for (int i = 0; i < mesh.rimCount(); i++) {
            int next = (i + 1) % mesh.rimCount();
            for (int corner = 0; corner < 4; corner++) {
                int index = corner < 2 ? i : next;
                float x = mesh.rimX(index);
                float y = mesh.rimY(index);
                float wave = mesh.wave(x, y, age);
                float inset = corner == 0 || corner == 3 ? 0.975F : 1;
                float highlight = mesh.highlight(index, age) * 0.7F;
                int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
                int bright = ((r + (int) ((255 - r) * highlight)) << 16)
                    | ((g + (int) ((255 - g) * highlight)) << 8)
                    | (b + (int) ((255 - b) * highlight));
                vertex(pose, vertices, basis, x * wave * radius * inset, y * wave * radius * inset,
                    SURFACE_OFFSET + 0.001F, face, bright, 255, 0.5F, 0.5F);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, PortalRenderBasis basis,
                               float x, float y, float depth, int face, int color, int alpha, float u, float v) {
        float z = depth * face;
        vertices.addVertex(pose,
                (float) (basis.right().x * x + basis.up().x * y + basis.normal().x * z),
                (float) (basis.right().y * x + basis.up().y * y + basis.normal().y * z),
                (float) (basis.right().z * x + basis.up().z * y + basis.normal().z * z))
            .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal(pose, (float) basis.normal().x * face,
                (float) basis.normal().y * face, (float) basis.normal().z * face);
    }
}
