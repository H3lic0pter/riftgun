package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.core.visual.WaterSplashGeometry;
import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** The animated vanilla water atlas is clipped by a fixed, per-portal geometric mask. */
final class WaterSplashPortalVisualRenderer implements PortalVisualRenderer {
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final float SURFACE_OFFSET = 0.006F;
    // A disc stays inside the sprite at every UV angle; the margin avoids atlas bleed.
    private static final float UV_RADIUS = 0.48F;

    @Override public boolean usesSplashParticles() { return false; }

    @Override
    public void render(PortalVisualRenderContext context) {
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
        float rotationAge = phase == PortalLifecycle.Phase.OPENING ? 0.0F
            : phase == PortalLifecycle.Phase.OPEN
                ? Math.max(0, openingAge - WaterSplashGeometry.OPENING_TICKS)
                : Math.max(0, context.age() - PortalLifecycle.CHARGE_TICKS - WaterSplashGeometry.OPENING_TICKS);
        float rotation = (rotationAge % WaterSplashGeometry.ROTATION_TICKS)
            / WaterSplashGeometry.ROTATION_TICKS * Mth.TWO_PI;
        // Circular water disc; this only changes the visual, not the transit aperture.
        float radius = Math.max(portal.portalWidth(), portal.portalHeight()) * 0.5F * scale;
        var mesh = WaterSplashGeometry.forPortal(portal.visualId());
        var basis = PortalRenderBasis.from(portal);
        var client = Minecraft.getInstance();
        // Resolve the live sprite each frame so resource-pack reloads replace the material safely.
        TextureAtlasSprite sprite = client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER_STILL);
        var camera = client.gameRenderer.getMainCamera().getPosition();
        int face = camera.subtract(portal.placement().center()).dot(basis.normal()) < 0 ? -1 : 1;
        int color = context.style().splashRgb();
        int light = context.packedLight();
        double cosine = Math.cos(rotation);
        double sine = Math.sin(rotation);
        // Shader packs use this same vanilla-material fallback until a native water path is verified.
        var material = RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS, false);
        draw(context.poseStack().last(), context.buffers().getBuffer(material), basis,
            mesh, sprite, radius, color, light, face, cosine, sine);
    }

    private static void draw(PoseStack.Pose pose, VertexConsumer vertices, PortalRenderBasis basis,
                             WaterSplashGeometry.Mesh mesh, TextureAtlasSprite sprite, float radius,
                             int color, int light, int face, double cosine, double sine) {
        for (int i = 0; i < mesh.vertexCount(); i++) {
            float localX = mesh.x(i);
            float localY = mesh.y(i);
            float x = localX * radius;
            float y = localY * radius;
            float z = SURFACE_OFFSET * face;
            // Rotate the sampling coordinates only: neither the silhouette nor the droplets move.
            float u = sprite.getU(0.5F + (float) (cosine * localX - sine * localY) * UV_RADIUS);
            float v = sprite.getV(0.5F - (float) (sine * localX + cosine * localY) * UV_RADIUS);
            vertices.addVertex(pose,
                    (float) (basis.right().x * x + basis.up().x * y + basis.normal().x * z),
                    (float) (basis.right().y * x + basis.up().y * y + basis.normal().y * z),
                    (float) (basis.right().z * x + basis.up().z * y + basis.normal().z * z))
                .setColor((color >> 16) & 255, (color >> 8) & 255, color & 255, 220)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, (float) basis.normal().x * face,
                    (float) basis.normal().y * face, (float) basis.normal().z * face);
        }
    }
}
