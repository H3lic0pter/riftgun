package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.portal.PortalVisualSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * PortalGun-style portal face: the vanilla animated end-portal star framed by
 * the PortalGun overlay ring tinted with the portal fluid colour. On the custom
 * pipeline the ring spins on the GPU like a vortex; under a shader pack a
 * standard entity pipeline carries CPU-rotated UVs plus a restrained glow.
 * Shader-pack rendering intentionally leaves the inner disc empty.
 */
final class EndframePortalVisualRenderer implements PortalVisualRenderer {
    /** Half-thickness of the two-faced star slab. Kept small so the overlay
     *  layers sit close to the star. */
    private static final float STAR_DEPTH = 0.004F;
    /** Keeps the ring in front of the star slab on each face. */
    private static final float RING_LAYER_OFFSET = 0.004F;
    /** The frame artwork is a full-canvas rift-liquid texture; sample it over
     *  the whole face. */
    private static final float RING_UV_MIN_U = 0.0F;
    private static final float RING_UV_MAX_U = 1.0F;
    private static final float RING_UV_MIN_V = 0.0F;
    private static final float RING_UV_MAX_V = 1.0F;
    /** The star disc is an ellipse that stops at the liquid frame's hollowed
     *  centre (the region the artwork marks for the star: radius ~100/128 of
     *  the face half-extent), so the star fills exactly the marked hole. */
    private static final float STAR_RADIUS_SCALE = 0.7773F;
    private static final float FALLBACK_GLOW_BRIGHTNESS = 0.45F;
    private static final float TAU = (float) (Math.PI * 2.0);

    @Override
    public void submit(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalVisualSource portal = context.portal();
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        // During a shader-pack shadow pass the whole body is skipped (see the
        // classic renderer); the end-portal star and ring get dropped there too.
        PortalSurfaceRenderPath path = context.surfaceRenderPath();
        if (path == PortalSurfaceRenderPath.SKIP_SURFACE) return;

        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = portal.portalWidth() * eased;
        float height = portal.portalHeight() * eased;
        int ringColor = context.style().surfaceColor();

        if (path == PortalSurfaceRenderPath.CUSTOM) {
            context.submit(PortalRenderTypes.endframeStar(path),
                (pose, vertices) -> drawStar(pose, basis, vertices, width, height));
        }

        boolean rotating = path == PortalSurfaceRenderPath.CUSTOM;
        if (rotating) {
            float rotation = rotationRadians(context);
            int front = SwirlPortalVisualRenderer.encodeRotation(rotation);
            // The back face mirrors the rotation so the liquid keeps spinning
            // the same way when viewed from the other side.
            int back = SwirlPortalVisualRenderer.encodeRotation(-rotation);
            context.submit(PortalRenderTypes.endframeFrameRotating(),
                (pose, vertices) -> drawSlab(pose.pose(), basis, vertices, width, height,
                    RING_LAYER_OFFSET, ringColor, 1.0F, true, front, back, 1.0, 0.0));
        } else {
            float rotation = rotationRadians(context);
            double cosine = Math.cos(rotation);
            double sine = Math.sin(rotation);
            context.submit(PortalRenderTypes.endframeFrame(),
                (pose, vertices) -> drawSlab(pose.pose(), basis, vertices, width, height,
                    RING_LAYER_OFFSET, ringColor, 1.0F, false, 0, 0, cosine, sine));
            context.submit(PortalRenderTypes.endframeFrameGlow(),
                (pose, vertices) -> drawSlab(pose.pose(), basis, vertices, width, height,
                    RING_LAYER_OFFSET, ringColor, FALLBACK_GLOW_BRIGHTNESS,
                    false, 0, 0, cosine, sine));
        }
    }

    private static float rotationRadians(PortalVisualRenderContext context) {
        var config = RiftConfigs.client();
        if (!config.endframeRotationEnabled()) return 0.0F;
        float sign = config.endframeRotationReverse() ? -1.0F : 1.0F;
        float seconds = context.age() / 20.0F;
        float turns = seconds / (float) Math.max(config.endframeRotationPeriod(), 0.1);
        return sign * turns * TAU;
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
        for (int segment = 0; segment < EndframeVisualGeometry.STAR_SEGMENTS; segment++) {
            int first = reversed ? segment + 1 : segment;
            int second = reversed ? segment : segment + 1;
            float firstX = EndframeVisualGeometry.rimX(first) * hw;
            float firstY = EndframeVisualGeometry.rimY(first) * hh;
            float secondX = EndframeVisualGeometry.rimX(second) * hw;
            float secondY = EndframeVisualGeometry.rimY(second) * hh;
            starVertex(vertices, pose, basis, 0.0F, 0.0F, z);
            starVertex(vertices, pose, basis, firstX, firstY, z);
            starVertex(vertices, pose, basis, secondX, secondY, z);
            starVertex(vertices, pose, basis, 0.0F, 0.0F, z);
        }
    }

    private static void starVertex(VertexConsumer vertices, PoseStack.Pose pose, PortalRenderBasis basis,
                                   float x, float y, float z) {
        float worldX = (float) (basis.right().x * x + basis.up().x * y + basis.normal().x * z);
        float worldY = (float) (basis.right().y * x + basis.up().y * y + basis.normal().y * z);
        float worldZ = (float) (basis.right().z * x + basis.up().z * y + basis.normal().z * z);
        vertices.addVertex(pose, worldX, worldY, worldZ);
    }

    private static void drawSlab(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                 float width, float height, float offset, int color,
                                 float brightness, boolean gpuRotating,
                                 int frontEncoded, int backEncoded,
                                 double cosine, double sine) {
        float red = red(color) * brightness;
        float green = green(color) * brightness;
        float blue = blue(color) * brightness;
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float slabHalf = STAR_DEPTH * 0.5F;

        Vec3 frontNormal = basis.normal();
        float frontZ = slabHalf + offset;
        quad(vertices, matrix, basis, frontNormal,
            -hw, hh, frontZ, hw, hh, frontZ, hw, -hh, frontZ, -hw, -hh, frontZ,
            red, green, blue, gpuRotating, frontEncoded, cosine, sine);

        Vec3 backNormal = basis.normal().scale(-1.0F);
        float backZ = -slabHalf - offset;
        quad(vertices, matrix, basis, backNormal,
            hw, hh, backZ, -hw, hh, backZ, -hw, -hh, backZ, hw, -hh, backZ,
            red, green, blue, gpuRotating, backEncoded, cosine, -sine);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                             Vec3 normal, float x1, float y1, float z1, float x2, float y2, float z2,
                              float x3, float y3, float z3, float x4, float y4, float z4,
                              float red, float green, float blue, boolean gpuRotating, int encoded,
                              double cosine, double sine) {
        frameVertex(vertices, matrix, basis, normal, x1, y1, z1, 0.0F, 0.0F, red, green, blue, gpuRotating, encoded, cosine, sine);
        frameVertex(vertices, matrix, basis, normal, x2, y2, z2, 1.0F, 0.0F, red, green, blue, gpuRotating, encoded, cosine, sine);
        frameVertex(vertices, matrix, basis, normal, x3, y3, z3, 1.0F, 1.0F, red, green, blue, gpuRotating, encoded, cosine, sine);
        frameVertex(vertices, matrix, basis, normal, x4, y4, z4, 0.0F, 1.0F, red, green, blue, gpuRotating, encoded, cosine, sine);
    }

    private static void frameVertex(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                                     Vec3 normal, float x, float y, float z,
                                     float faceU, float faceV, float red, float green, float blue,
                                     boolean gpuRotating, int encoded,
                                     double cosine, double sine) {
        Vec3 point = basis.at(x, y, z);
        float sourceU = RING_UV_MIN_U + faceU * (RING_UV_MAX_U - RING_UV_MIN_U);
        float sourceV = RING_UV_MIN_V + faceV * (RING_UV_MAX_V - RING_UV_MIN_V);
        float u = gpuRotating ? sourceU : EndframeVisualGeometry.rotatedU(sourceU, sourceV, cosine, sine);
        float v = gpuRotating ? sourceV : EndframeVisualGeometry.rotatedV(sourceU, sourceV, cosine, sine);
        if (gpuRotating) {
            // POSITION_COLOR_TEX_LIGHTMAP with the angle packed into the lightmap.
            vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, 1.0F)
                .setUv(u, v)
                .setUv2(encoded, 0);
        } else {
            vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
                .setColor(red, green, blue, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }
}
