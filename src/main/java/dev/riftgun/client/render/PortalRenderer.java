package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.riftgun.RiftGun;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Procedural portal adapted from Tempad's MIT-licensed TimedoorRenderer. */
public final class PortalRenderer extends EntityRenderer<PortalEntity> {
    private static final ResourceLocation EMPTY_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "textures/misc/empty.png");

    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PortalEntity entity) {
        return EMPTY_TEXTURE;
    }

    @Override
    public void render(PortalEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float progress = PortalLifecycle.visibleProgress(entity.phase(), entity.phaseTicks(), partialTick);
        if (progress <= 0.0F) return;

        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = PortalEntity.WIDTH * eased;
        float height = PortalEntity.HEIGHT * eased;
        float depth = PortalEntity.DEPTH;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(entity.getYRot() + 180.0F));
        poseStack.translate(-width * 0.5, (PortalEntity.HEIGHT - height) * 0.5, -depth * 0.5);

        float shimmer = 0.96F + Mth.sin((entity.tickCount + partialTick) * 0.18F) * 0.04F;
        drawVolume(poseStack, buffers.getBuffer(PortalRenderTypes.portal()), width, height, depth, shimmer);
        drawBorder(poseStack, buffers.getBuffer(PortalRenderTypes.border()), width, height, depth,
            entity.tickCount + partialTick);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void drawVolume(PoseStack stack, VertexConsumer vertices, float width, float height,
                                   float depth, float shimmer) {
        Matrix4f matrix = stack.last().pose();
        float red = 0.38F * shimmer;
        float green = 1.0F * shimmer;
        float blue = 0.28F * shimmer;

        quad(vertices, matrix, 0, height, 0, width, height, 0, width, 0, 0, 0, 0, 0,
            red, green, blue);
        quad(vertices, matrix, width, height, depth, 0, height, depth, 0, 0, depth, width, 0, depth,
            red, green, blue);
        quad(vertices, matrix, 0, height, depth, width, height, depth, width, height, 0, 0, height, 0,
            red, green, blue);
        quad(vertices, matrix, 0, 0, 0, width, 0, 0, width, 0, depth, 0, 0, depth, red, green, blue);
        quad(vertices, matrix, 0, height, depth, 0, height, 0, 0, 0, 0, 0, 0, depth, red, green, blue);
        quad(vertices, matrix, width, height, 0, width, height, depth, width, 0, depth, width, 0, 0,
            red, green, blue);
    }

    private static void quad(VertexConsumer vertices, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float red, float green, float blue) {
        vertex(vertices, matrix, x1, y1, z1, red, green, blue, 0, 1);
        vertex(vertices, matrix, x2, y2, z2, red, green, blue, 1, 1);
        vertex(vertices, matrix, x3, y3, z3, red, green, blue, 1, 0);
        vertex(vertices, matrix, x4, y4, z4, red, green, blue, 0, 0);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float y, float z,
                               float red, float green, float blue, float u, float v) {
        vertices.addVertex(matrix, x, y, z)
            .setColor(red, green, blue, 1.0F)
            .setUv(u, v)
            .setUv2((int) (x * 16.0F), (int) (y * 16.0F));
    }

    private static void drawBorder(PoseStack stack, VertexConsumer vertices, float width, float height,
                                   float depth, float age) {
        var pose = stack.last();
        Matrix4f matrix = pose.pose();
        int color = PortalEntity.COLOR | 0xFF000000;

        line(vertices, pose, matrix, 0, 0, 0, width, 0, 0, color);
        line(vertices, pose, matrix, width, 0, 0, width, height, 0, color);
        line(vertices, pose, matrix, width, height, 0, 0, height, 0, color);
        line(vertices, pose, matrix, 0, height, 0, 0, 0, 0, color);
        line(vertices, pose, matrix, 0, 0, depth, width, 0, depth, color);
        line(vertices, pose, matrix, width, 0, depth, width, height, depth, color);
        line(vertices, pose, matrix, width, height, depth, 0, height, depth, color);
        line(vertices, pose, matrix, 0, height, depth, 0, 0, depth, color);

        float perimeter = 2.0F * (width + height);
        float cursor = age % Math.max(perimeter, 0.001F);
        float[] point = perimeterPoint(cursor, width, height);
        float pulse = 0.08F;
        line(vertices, pose, matrix,
            Math.max(0, point[0] - pulse), point[1], -0.004F,
            Math.min(width, point[0] + pulse), point[1], -0.004F,
            0xFFFFFFFF);
    }

    private static float[] perimeterPoint(float distance, float width, float height) {
        if (distance < width) return new float[] {distance, height};
        distance -= width;
        if (distance < height) return new float[] {width, height - distance};
        distance -= height;
        if (distance < width) return new float[] {width - distance, 0};
        distance -= width;
        return new float[] {0, Math.min(height, distance)};
    }

    private static void line(VertexConsumer vertices, PoseStack.Pose pose, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0.0F) return;
        dx /= length;
        dy /= length;
        dz /= length;
        vertices.addVertex(matrix, x1, y1, z1).setColor(color).setNormal(pose, dx, dy, dz);
        vertices.addVertex(matrix, x2, y2, z2).setColor(color).setNormal(pose, dx, dy, dz);
    }
}
