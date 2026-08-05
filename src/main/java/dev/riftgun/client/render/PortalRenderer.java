package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.RiftGun;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Procedural portal body. Transient splash particles are emitted from the client tick. */
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
        PortalVisualStyle style = PortalVisualStyles.resolve(entity);
        Basis basis = new Basis(entity.right(), entity.up(), entity.normal());
        Matrix4f matrix = poseStack.last().pose();

        float progress = PortalLifecycle.visibleProgress(entity.phase(), entity.phaseTicks(), partialTick);
        if (progress > 0.0F) {
            float eased = Mth.sin(progress * Mth.HALF_PI);
            float width = entity.portalWidth() * eased;
            float height = entity.portalHeight() * eased;
            float shimmer = 0.96F + Mth.sin((entity.tickCount + partialTick) * 0.18F) * 0.04F;
            drawVolume(matrix, basis, buffers.getBuffer(PortalRenderTypes.portal()), width, height,
                PortalEntity.DEPTH, style.surfaceColor(), shimmer);
            drawBorder(matrix, poseStack.last(), basis, buffers.getBuffer(PortalRenderTypes.border()),
                width, height, PortalEntity.DEPTH, entity.tickCount + partialTick, style.borderColor());
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void drawVolume(Matrix4f matrix, Basis basis, VertexConsumer vertices,
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

    private static void quad(VertexConsumer vertices, Matrix4f matrix, Basis basis,
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

    private static void drawBorder(Matrix4f matrix, PoseStack.Pose pose, Basis basis,
                                   VertexConsumer vertices, float width, float height,
                                   float depth, float age, int color) {
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

        float perimeter = 2.0F * (width + height);
        float cursor = age % Math.max(perimeter, 0.001F);
        float[] point = perimeterPoint(cursor, width, height);
        Vec3 pulseCenter = basis.at(point[0] - hw, point[1] - hh, -hd - 0.004F);
        line(vertices, pose, matrix, pulseCenter.subtract(basis.right.scale(0.06)),
            pulseCenter.add(basis.right.scale(0.06)), 0xFFFFFFFF);
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
                             Vec3 from, Vec3 to, int color) {
        Vec3 direction = to.subtract(from).normalize();
        vertices.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
            .setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
        vertices.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
            .setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }

    private record Basis(Vec3 right, Vec3 up, Vec3 normal) {
        Vec3 at(float x, float y, float z) {
            return right.scale(x).add(up.scale(y)).add(normal.scale(z));
        }
    }
}
