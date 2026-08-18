package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalVisualSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * PortalGun-style portal face: the vanilla animated end-portal star framed by
 * the PortalGun overlay ring tinted with the portal fluid colour. The star is
 * drawn through the vanilla end-portal shader so texture and shader packs that
 * override the end portal keep working; the ring hugs the portal edge.
 */
final class EndframePortalVisualRenderer implements PortalVisualRenderer {
    /** Half-thickness of the two-faced star slab. */
    private static final float STAR_DEPTH = 0.02F;
    /** Keeps the ring in front of the star slab on each face. */
    private static final float RING_LAYER_OFFSET = 0.02F;
    /** The ring artwork is a 64x64 circle in the top-left band of its 128x128
     *  canvas (u 0..0.5, v 0.25..0.75); stretch that crop over the full face so
     *  the ring circumscribes the portal and frames the star. */
    private static final float RING_UV_MIN_U = 0.0F;
    private static final float RING_UV_MAX_U = 0.5F;
    private static final float RING_UV_MIN_V = 0.25F;
    private static final float RING_UV_MAX_V = 0.75F;
    /** The star disc is an ellipse that stops at the ring's inner edge (~90% of
     *  the face half-extent), so no star pokes past the circular ring. */
    private static final float STAR_RADIUS_SCALE = 0.9F;
    private static final int STAR_SEGMENTS = 48;

    @Override
    public void submit(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalVisualSource portal = context.portal();
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        // During a shader-pack shadow pass the whole body is skipped (see the
        // classic renderer); the end-portal star and ring get dropped there too.
        if (PortalShaderCompatibility.currentPath() == PortalSurfaceRenderPath.SKIP_SURFACE) return;

        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = portal.portalWidth() * eased;
        float height = portal.portalHeight() * eased;
        int ringColor = context.style().surfaceColor();

        // Vanilla animated end-portal star (unculled so both faces render).
        context.submit(PortalRenderTypes.endframeStar(),
            (pose, vertices) -> drawStar(pose, basis, vertices, width, height));
        context.submit(PortalRenderTypes.endframeFrame(),
            (pose, vertices) -> drawSlab(pose.pose(), basis, vertices, width, height,
                RING_LAYER_OFFSET, ringColor));
    }

    private static void drawStar(PoseStack.Pose pose, PortalRenderBasis basis, VertexConsumer vertices,
                                 float width, float height) {
        float hw = width * 0.5F * STAR_RADIUS_SCALE;
        float hh = height * 0.5F * STAR_RADIUS_SCALE;
        float half = STAR_DEPTH * 0.5F;
        starFan(vertices, pose, basis, hw, hh, half, false);
        starFan(vertices, pose, basis, hw, hh, -half, true);
    }

    private static void starFan(VertexConsumer vertices, PoseStack.Pose pose, PortalRenderBasis basis,
                                float hw, float hh, float z, boolean reversed) {
        Vec3 center = basis.at(0.0F, 0.0F, z);
        for (int segment = 0; segment < STAR_SEGMENTS; segment++) {
            float first = (float) (Math.PI * 2.0 * segment / STAR_SEGMENTS);
            float second = (float) (Math.PI * 2.0 * (segment + 1) / STAR_SEGMENTS);
            Vec3 firstPoint = basis.at((float) Math.cos(first) * hw, (float) Math.sin(first) * hh, z);
            Vec3 secondPoint = basis.at((float) Math.cos(second) * hw, (float) Math.sin(second) * hh, z);
            if (reversed) {
                starVertex(vertices, pose, center);
                starVertex(vertices, pose, secondPoint);
                starVertex(vertices, pose, firstPoint);
            } else {
                starVertex(vertices, pose, center);
                starVertex(vertices, pose, firstPoint);
                starVertex(vertices, pose, secondPoint);
            }
        }
    }

    private static void starVertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 point) {
        vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z);
    }

    private static void drawSlab(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                 float width, float height, float offset, int color) {
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float slabHalf = STAR_DEPTH * 0.5F;

        Vec3 frontNormal = basis.normal();
        float frontZ = slabHalf + offset;
        quad(vertices, matrix, basis, frontNormal,
            -hw, hh, frontZ, hw, hh, frontZ, hw, -hh, frontZ, -hw, -hh, frontZ,
            red, green, blue);

        Vec3 backNormal = basis.normal().scale(-1.0F);
        float backZ = -slabHalf - offset;
        quad(vertices, matrix, basis, backNormal,
            hw, hh, backZ, -hw, hh, backZ, -hw, -hh, backZ, hw, -hh, backZ,
            red, green, blue);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                             Vec3 normal, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float red, float green, float blue) {
        ringVertex(vertices, matrix, basis, normal, x1, y1, z1, 0.0F, 0.0F, red, green, blue);
        ringVertex(vertices, matrix, basis, normal, x2, y2, z2, 1.0F, 0.0F, red, green, blue);
        ringVertex(vertices, matrix, basis, normal, x3, y3, z3, 1.0F, 1.0F, red, green, blue);
        ringVertex(vertices, matrix, basis, normal, x4, y4, z4, 0.0F, 1.0F, red, green, blue);
    }

    private static void ringVertex(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                                   Vec3 normal, float x, float y, float z,
                                   float faceU, float faceV, float red, float green, float blue) {
        Vec3 point = basis.at(x, y, z);
        float u = RING_UV_MIN_U + faceU * (RING_UV_MAX_U - RING_UV_MIN_U);
        float v = RING_UV_MIN_V + faceV * (RING_UV_MAX_V - RING_UV_MIN_V);
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 1.0F)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT)
            .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }
}
