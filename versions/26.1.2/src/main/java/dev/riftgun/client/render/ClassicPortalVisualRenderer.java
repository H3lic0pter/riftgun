package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalVisualSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class ClassicPortalVisualRenderer implements PortalVisualRenderer {
    @Override
    public void submit(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalVisualSource portal = context.portal();
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = portal.portalWidth() * eased;
        float height = portal.portalHeight() * eased;
        float shimmer = 0.96F + Mth.sin(context.age() * 0.18F) * 0.04F;
        PortalSurfaceRenderPath path = context.surfaceRenderPath();
        // During a shader-pack shadow pass the whole portal body is skipped (surface, border and
        // edge): Iris 26.1.2 has no override for the lines pipeline used by the border and would
        // fatal-compile it. Skipping the portal in shadow passes is also cheaper.
        if (path == PortalSurfaceRenderPath.SKIP_SURFACE) return;
        if (path == PortalSurfaceRenderPath.CUSTOM) {
            context.submit(PortalRenderTypes.portal(), (pose, vertices) -> drawVolume(pose.pose(), basis, vertices,
                width, height, PortalEntity.DEPTH, context.style().surfaceColor(), shimmer));
        } else if (path == PortalSurfaceRenderPath.VANILLA_FALLBACK) {
            context.submit(PortalRenderTypes.classicFallback(), (pose, vertices) -> drawFallbackVolume(
                pose.pose(), basis, vertices, width, height, PortalEntity.DEPTH,
                context.style().surfaceColor(), shimmer));
        }
        context.submit(PortalRenderTypes.border(), (pose, vertices) -> drawBorder(pose.pose(), pose, basis, vertices,
            width, height, PortalEntity.DEPTH, context.style().borderColor()));
    }

    private static void drawVolume(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                   float width, float height, float depth, int color, float shimmer) {
        float red = red(color) * shimmer;
        float green = green(color) * shimmer;
        float blue = blue(color) * shimmer;
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float hd = depth * 0.5F;

        quad(vertices, matrix, basis, -hw, hh, -hd, hw, hh, -hd, hw, -hh, -hd, -hw, -hh, -hd,
            red, green, blue);
        quad(vertices, matrix, basis, hw, hh, hd, -hw, hh, hd, -hw, -hh, hd, hw, -hh, hd,
            red, green, blue);
        quad(vertices, matrix, basis, -hw, hh, hd, hw, hh, hd, hw, hh, -hd, -hw, hh, -hd,
            red, green, blue);
        quad(vertices, matrix, basis, -hw, -hh, -hd, hw, -hh, -hd, hw, -hh, hd, -hw, -hh, hd,
            red, green, blue);
        quad(vertices, matrix, basis, -hw, hh, hd, -hw, hh, -hd, -hw, -hh, -hd, -hw, -hh, hd,
            red, green, blue);
        quad(vertices, matrix, basis, hw, hh, -hd, hw, hh, hd, hw, -hh, hd, hw, -hh, -hd,
            red, green, blue);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float red, float green, float blue) {
        vertex(vertices, matrix, basis.at(x1, y1, z1), red, green, blue, 0, 1);
        vertex(vertices, matrix, basis.at(x2, y2, z2), red, green, blue, 1, 1);
        vertex(vertices, matrix, basis.at(x3, y3, z3), red, green, blue, 1, 0);
        vertex(vertices, matrix, basis.at(x4, y4, z4), red, green, blue, 0, 0);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               float red, float green, float blue, float u, float v) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 1.0F).setUv(u, v).setUv2(240, 240);
    }

    private static void drawFallbackVolume(Matrix4f matrix, PortalRenderBasis basis,
                                           VertexConsumer vertices, float width, float height,
                                           float depth, int color, float shimmer) {
        float red = red(color) * shimmer;
        float green = green(color) * shimmer;
        float blue = blue(color) * shimmer;
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float hd = depth * 0.5F;

        fallbackQuad(vertices, matrix, basis.normal().scale(-1.0), basis,
            -hw, hh, -hd, hw, hh, -hd, hw, -hh, -hd, -hw, -hh, -hd, red, green, blue);
        fallbackQuad(vertices, matrix, basis.normal(), basis,
            hw, hh, hd, -hw, hh, hd, -hw, -hh, hd, hw, -hh, hd, red, green, blue);
        fallbackQuad(vertices, matrix, basis.up(), basis,
            -hw, hh, hd, hw, hh, hd, hw, hh, -hd, -hw, hh, -hd, red, green, blue);
        fallbackQuad(vertices, matrix, basis.up().scale(-1.0), basis,
            -hw, -hh, -hd, hw, -hh, -hd, hw, -hh, hd, -hw, -hh, hd, red, green, blue);
        fallbackQuad(vertices, matrix, basis.right().scale(-1.0), basis,
            -hw, hh, hd, -hw, hh, -hd, -hw, -hh, -hd, -hw, -hh, hd, red, green, blue);
        fallbackQuad(vertices, matrix, basis.right(), basis,
            hw, hh, -hd, hw, hh, hd, hw, -hh, hd, hw, -hh, -hd, red, green, blue);
    }

    private static void fallbackQuad(VertexConsumer vertices, Matrix4f matrix, Vec3 normal,
                                     PortalRenderBasis basis,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     float x3, float y3, float z3, float x4, float y4, float z4,
                                     float red, float green, float blue) {
        fallbackVertex(vertices, matrix, basis.at(x1, y1, z1), normal, red, green, blue, 0, 1);
        fallbackVertex(vertices, matrix, basis.at(x2, y2, z2), normal, red, green, blue, 1, 1);
        fallbackVertex(vertices, matrix, basis.at(x3, y3, z3), normal, red, green, blue, 1, 0);
        fallbackVertex(vertices, matrix, basis.at(x4, y4, z4), normal, red, green, blue, 0, 0);
    }

    private static void fallbackVertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                                       Vec3 normal, float red, float green, float blue,
                                       float u, float v) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 0.72F)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void drawBorder(Matrix4f matrix, PoseStack.Pose pose, PortalRenderBasis basis,
                                   VertexConsumer vertices, float width, float height,
                                   float depth, int color) {
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float hd = depth * 0.5F;
        Vec3 a = basis.at(-hw, -hh, -hd);
        Vec3 b = basis.at(hw, -hh, -hd);
        Vec3 c = basis.at(hw, hh, -hd);
        Vec3 d = basis.at(-hw, hh, -hd);
        line(vertices, pose, matrix, a, b, color);
        line(vertices, pose, matrix, b, c, color);
        line(vertices, pose, matrix, c, d, color);
        line(vertices, pose, matrix, d, a, color);
    }

    private static void line(VertexConsumer vertices, PoseStack.Pose pose, Matrix4f matrix,
                             Vec3 from, Vec3 to, int color) {
        Vec3 direction = to.subtract(from).normalize();
        vertices.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
            .setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z)
            .setLineWidth(3.0F);
        vertices.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
            .setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z)
            .setLineWidth(3.0F);
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }
}
