package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.internal.shader.ShaderPackProfile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * PortalGun-style portal face: the vanilla animated end-portal star framed by
 * the PortalGun overlay ring tinted with the portal fluid colour. On the custom
 * shader path the ring spins on the GPU like a vortex; under a shader pack a
 * standard entity pipeline carries CPU-rotated UVs plus a restrained glow.
 * Registered shader packs may opt into a native Iris block-entity material for
 * the inner disc; unregistered packs leave it empty.
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
    public void render(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalVisualSource portal = context.portal();
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        PortalSurfaceRenderPath path = context.surfaceRenderPath();
        if (path == PortalSurfaceRenderPath.SKIP_SURFACE) return;
        PoseStack.Pose pose = context.poseStack().last();
        Matrix4f matrix = pose.pose();
        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = portal.portalWidth() * eased;
        float height = portal.portalHeight() * eased;

        if (path == PortalSurfaceRenderPath.CUSTOM) {
            drawStar(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.endframeStar(path)),
                width, height);
        } else {
            drawRegisteredShaderCenter(context, pose, basis, width, height);
        }

        float rotation = rotationRadians(context);
        if (path == PortalSurfaceRenderPath.CUSTOM) {
            int front = encodeRotation(rotation);
            // The back face mirrors the rotation so the liquid keeps spinning
            // the same way when viewed from the other side.
            int back = encodeRotation(-rotation);
            drawSlab(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.endframeFrameRotating()),
                width, height, RING_LAYER_OFFSET, context.style().surfaceColor(),
                1.0F, true, front, back, 1.0, 0.0);
        } else {
            double cosine = Math.cos(rotation);
            double sine = Math.sin(rotation);
            drawSlab(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.endframeFrame()),
                width, height, RING_LAYER_OFFSET, context.style().surfaceColor(),
                1.0F, false, 0, 0, cosine, sine);
            drawSlab(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.endframeFrameGlow()),
                width, height, RING_LAYER_OFFSET, context.style().surfaceColor(),
                FALLBACK_GLOW_BRIGHTNESS, false, 0, 0, cosine, sine);
        }
    }

    private static void drawRegisteredShaderCenter(PortalVisualRenderContext context,
                                                    PoseStack.Pose pose,
                                                    PortalRenderBasis basis, float width, float height) {
        ShaderPackProfile.EndframeCenter center = context.shaderPackProfile().endframeCenter();
        if (center.mode() != ShaderPackProfile.EndframeCenter.Mode.IRIS_BLOCK_ENTITY) return;

        IrisBlockEntityMaterialBridge bridge = IrisBlockEntityMaterialBridge.instance();
        RenderType wrapped = bridge.wrap(PortalRenderTypes.endframeNativeShaderCenter());
        if (wrapped == null) return;
        MultiBufferSource buffers = bridge.originalBufferSource(context.buffers());
        if (buffers == null) return;
        bridge.renderWithMaterial(center.materialId(), () ->
            drawNativeShaderStar(pose, basis, buffers.getBuffer(wrapped), width, height));
    }

    private static void drawNativeShaderStar(PoseStack.Pose pose, PortalRenderBasis basis,
                                             VertexConsumer vertices, float width, float height) {
        float hw = width * 0.5F * STAR_RADIUS_SCALE;
        float hh = height * 0.5F * STAR_RADIUS_SCALE;
        float half = STAR_DEPTH * 0.5F;
        nativeShaderStarFan(vertices, pose, basis, hw, hh, half, false);
        nativeShaderStarFan(vertices, pose, basis, hw, hh, -half, true);
    }

    private static void nativeShaderStarFan(VertexConsumer vertices, PoseStack.Pose pose,
                                            PortalRenderBasis basis, float hw, float hh,
                                            float z, boolean reversed) {
        float normalSign = reversed ? -1.0F : 1.0F;
        for (int segment = 0; segment < EndframeVisualGeometry.STAR_SEGMENTS; segment++) {
            int first = reversed ? segment + 1 : segment;
            int second = reversed ? segment : segment + 1;
            nativeShaderStarVertex(vertices, pose, basis, normalSign,
                0.0F, 0.0F, z, 0.1F, 0.1F);
            nativeShaderStarRimVertex(vertices, pose, basis, normalSign, first, hw, hh, z);
            nativeShaderStarRimVertex(vertices, pose, basis, normalSign, second, hw, hh, z);
            nativeShaderStarVertex(vertices, pose, basis, normalSign,
                0.0F, 0.0F, z, 0.1F, 0.1F);
        }
    }

    private static void nativeShaderStarRimVertex(VertexConsumer vertices, PoseStack.Pose pose,
                                                  PortalRenderBasis basis, float normalSign, int rim,
                                                  float hw, float hh, float z) {
        float rimX = EndframeVisualGeometry.rimX(rim);
        float rimY = EndframeVisualGeometry.rimY(rim);
        nativeShaderStarVertex(vertices, pose, basis, normalSign, rimX * hw, rimY * hh, z,
            0.1F + rimX * 0.1F, 0.1F - rimY * 0.1F);
    }

    private static void nativeShaderStarVertex(VertexConsumer vertices, PoseStack.Pose pose,
                                               PortalRenderBasis basis, float normalSign,
                                               float x, float y, float z, float u, float v) {
        float worldX = (float) (basis.right().x * x + basis.up().x * y + basis.normal().x * z);
        float worldY = (float) (basis.right().y * x + basis.up().y * y + basis.normal().y * z);
        float worldZ = (float) (basis.right().z * x + basis.up().z * y + basis.normal().z * z);
        vertices.addVertex(pose, worldX, worldY, worldZ)
            .setColor(0.075F, 0.15F, 0.2F, 1.0F)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose,
                (float) basis.normal().x * normalSign,
                (float) basis.normal().y * normalSign,
                (float) basis.normal().z * normalSign);
    }

    private static float rotationRadians(PortalVisualRenderContext context) {
        var config = RiftConfigs.client();
        if (!config.endframeRotationEnabled()) return 0.0F;
        float sign = config.endframeRotationReverse() ? -1.0F : 1.0F;
        float seconds = context.age() / 20.0F;
        float turns = seconds / (float) Math.max(config.endframeRotationPeriod(), 0.1);
        return sign * turns * TAU;
    }

    /** Encodes a rotation angle onto the 0..65535 range carried by the UV2
     *  lightmap attribute; the shader reads it back unsigned to recover 0..TAU. */
    private static int encodeRotation(float radians) {
        float turns = radians / TAU;
        turns -= (float) Math.floor(turns);
        return (int) (turns * 65535.0F);
    }

    private static void drawStar(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                 float width, float height) {
        float hw = width * 0.5F * STAR_RADIUS_SCALE;
        float hh = height * 0.5F * STAR_RADIUS_SCALE;
        float half = STAR_DEPTH * 0.5F;
        starFan(vertices, matrix, basis, hw, hh, half, false);
        starFan(vertices, matrix, basis, hw, hh, -half, true);
    }

    private static void starFan(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                                float hw, float hh, float z, boolean reversed) {
        for (int segment = 0; segment < EndframeVisualGeometry.STAR_SEGMENTS; segment++) {
            int first = reversed ? segment + 1 : segment;
            int second = reversed ? segment : segment + 1;
            float firstX = EndframeVisualGeometry.rimX(first) * hw;
            float firstY = EndframeVisualGeometry.rimY(first) * hh;
            float secondX = EndframeVisualGeometry.rimX(second) * hw;
            float secondY = EndframeVisualGeometry.rimY(second) * hh;
            starVertex(vertices, matrix, basis, 0.0F, 0.0F, z);
            starVertex(vertices, matrix, basis, firstX, firstY, z);
            starVertex(vertices, matrix, basis, secondX, secondY, z);
            starVertex(vertices, matrix, basis, 0.0F, 0.0F, z);
        }
    }

    private static void starVertex(VertexConsumer vertices, Matrix4f matrix, PortalRenderBasis basis,
                                   float x, float y, float z) {
        float worldX = (float) (basis.right().x * x + basis.up().x * y + basis.normal().x * z);
        float worldY = (float) (basis.right().y * x + basis.up().y * y + basis.normal().y * z);
        float worldZ = (float) (basis.right().z * x + basis.up().z * y + basis.normal().z * z);
        vertices.addVertex(matrix, worldX, worldY, worldZ);
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
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        }
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }
}
