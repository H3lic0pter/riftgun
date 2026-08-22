package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.RiftGun;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Fuel-tinted loading fill and persistent frame around the IP view. */
final class ImmersivePortalOverlayRenderer {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(
        RiftGun.MOD_ID, "textures/entity/immersive_portal_frame.png");
    private static final int DISC_SEGMENTS = 56;
    private static final float DISC_RADIUS = 0.91875F;
    private static final float FACE_OFFSET = 0.001F;

    void render(PortalVisualRenderContext context, float readiness) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;
        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = context.portal().portalWidth() * eased;
        float height = context.portal().portalHeight() * eased;
        int rgb = context.portal().fuelRgb();
        Matrix4f matrix = context.poseStack().last().pose();
        PortalRenderBasis basis = PortalRenderBasis.from(context.portal());

        float fillAlpha = 1.0F - Mth.clamp(readiness, 0.0F, 1.0F);
        if (fillAlpha > 0.0F) {
            drawSolidDisc(context.buffers().getBuffer(PortalRenderTypes.immersiveFill()),
                matrix, basis, width, height, rgb, fillAlpha * progress);
        }
        drawBothFaces(context.buffers().getBuffer(RenderType.entityTranslucent(FRAME)),
            matrix, basis, width, height, rgb, progress);
    }

    private static void drawSolidDisc(VertexConsumer vertices, Matrix4f matrix,
                                      PortalRenderBasis basis, float width, float height,
                                      int rgb, float alpha) {
        float halfWidth = width * 0.5F * DISC_RADIUS;
        float halfHeight = height * 0.5F * DISC_RADIUS;
        Vec3 center = basis.at(0.0F, 0.0F, 0.0F);
        for (int segment = 0; segment < DISC_SEGMENTS; segment++) {
            double first = Math.PI * 2.0 * segment / DISC_SEGMENTS;
            double second = Math.PI * 2.0 * (segment + 1) / DISC_SEGMENTS;
            solidVertex(vertices, matrix, center, rgb, alpha);
            solidVertex(vertices, matrix, basis.at(
                (float) Math.cos(first) * halfWidth,
                (float) Math.sin(first) * halfHeight, 0.0F), rgb, alpha);
            solidVertex(vertices, matrix, basis.at(
                (float) Math.cos(second) * halfWidth,
                (float) Math.sin(second) * halfHeight, 0.0F), rgb, alpha);
        }
    }

    private static void solidVertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                                    int rgb, float alpha) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F));
    }

    private static void drawBothFaces(VertexConsumer vertices, Matrix4f matrix,
                                      PortalRenderBasis basis, float width, float height,
                                      int rgb, float alpha) {
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;
        Vec3 normal = basis.normal();
        drawQuad(vertices, matrix, basis, halfWidth, halfHeight, FACE_OFFSET,
            normal, rgb, alpha, false);
        drawQuad(vertices, matrix, basis, halfWidth, halfHeight, -FACE_OFFSET,
            normal.scale(-1.0), rgb, alpha, true);
    }

    private static void drawQuad(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                                 float halfWidth, float halfHeight, float offset, Vec3 normal,
                                 int rgb, float alpha, boolean back) {
        if (back) {
            vertex(vertices, matrix, basis.at(halfWidth, -halfHeight, offset), normal, rgb, alpha, 0, 1);
            vertex(vertices, matrix, basis.at(-halfWidth, -halfHeight, offset), normal, rgb, alpha, 1, 1);
            vertex(vertices, matrix, basis.at(-halfWidth, halfHeight, offset), normal, rgb, alpha, 1, 0);
            vertex(vertices, matrix, basis.at(halfWidth, halfHeight, offset), normal, rgb, alpha, 0, 0);
        } else {
            vertex(vertices, matrix, basis.at(-halfWidth, -halfHeight, offset), normal, rgb, alpha, 0, 1);
            vertex(vertices, matrix, basis.at(halfWidth, -halfHeight, offset), normal, rgb, alpha, 1, 1);
            vertex(vertices, matrix, basis.at(halfWidth, halfHeight, offset), normal, rgb, alpha, 1, 0);
            vertex(vertices, matrix, basis.at(-halfWidth, halfHeight, offset), normal, rgb, alpha, 0, 0);
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point, Vec3 normal,
                               int rgb, float alpha, float u, float v) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255,
                Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F))
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
