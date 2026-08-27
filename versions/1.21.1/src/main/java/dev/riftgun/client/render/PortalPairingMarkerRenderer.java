package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.pairing.PortalPairingEndpoint;
import dev.riftgun.portal.PortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Theme-independent endpoint grammar layered over every portal visual. */
final class PortalPairingMarkerRenderer {
    private static final int SEGMENTS = 48;
    private static final float NORMAL_OFFSET = PortalEntity.DEPTH * 0.65F;

    static void render(PortalEntity portal, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int color) {
        PortalPairingEndpoint endpoint = portal.pairingEndpoint();
        if (endpoint == PortalPairingEndpoint.NONE) return;
        float scale = Mth.sin(portal.visualProgress(partialTick) * Mth.HALF_PI);
        if (scale <= 0.0F) return;
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer vertices = buffers.getBuffer(PortalRenderTypes.border());
        float halfWidth = portal.portalWidth() * 0.5F * scale;
        float halfHeight = portal.portalHeight() * 0.5F * scale;
        switch (endpoint) {
            case A -> ellipse(vertices, pose, basis, halfWidth * 1.08F, halfHeight * 1.08F,
                NORMAL_OFFSET, color, false);
            case B -> {
                ellipse(vertices, pose, basis, halfWidth * 1.08F, halfHeight * 1.08F,
                    NORMAL_OFFSET, color, true);
                ellipse(vertices, pose, basis, halfWidth * 0.94F, halfHeight * 0.94F,
                    NORMAL_OFFSET, color, true);
            }
            case ENTITY_TARGET -> reticle(vertices, pose, basis, halfWidth, halfHeight, color);
            case NONE -> { }
        }
    }

    private static void ellipse(VertexConsumer vertices, PoseStack.Pose pose, PortalRenderBasis basis,
                                float halfWidth, float halfHeight, float z, int color,
                                boolean segmented) {
        for (int segment = 0; segment < SEGMENTS; segment++) {
            if (segmented && segment % 8 >= 5) continue;
            double first = Math.PI * 2.0 * segment / SEGMENTS;
            double second = Math.PI * 2.0 * (segment + 1) / SEGMENTS;
            line(vertices, pose, basis.at((float) Math.cos(first) * halfWidth,
                    (float) Math.sin(first) * halfHeight, z),
                basis.at((float) Math.cos(second) * halfWidth,
                    (float) Math.sin(second) * halfHeight, z), color);
        }
    }

    private static void reticle(VertexConsumer vertices, PoseStack.Pose pose, PortalRenderBasis basis,
                                float halfWidth, float halfHeight, int color) {
        ellipse(vertices, pose, basis, halfWidth * 0.45F, halfHeight * 0.45F,
            NORMAL_OFFSET, color, false);
        float innerX = halfWidth * 0.56F;
        float outerX = halfWidth * 0.86F;
        float innerY = halfHeight * 0.56F;
        float outerY = halfHeight * 0.86F;
        line(vertices, pose, basis.at(-outerX, 0.0F, NORMAL_OFFSET),
            basis.at(-innerX, 0.0F, NORMAL_OFFSET), color);
        line(vertices, pose, basis.at(innerX, 0.0F, NORMAL_OFFSET),
            basis.at(outerX, 0.0F, NORMAL_OFFSET), color);
        line(vertices, pose, basis.at(0.0F, -outerY, NORMAL_OFFSET),
            basis.at(0.0F, -innerY, NORMAL_OFFSET), color);
        line(vertices, pose, basis.at(0.0F, innerY, NORMAL_OFFSET),
            basis.at(0.0F, outerY, NORMAL_OFFSET), color);
    }

    private static void line(VertexConsumer vertices, PoseStack.Pose pose,
                             Vec3 from, Vec3 to, int color) {
        Matrix4f matrix = pose.pose();
        Vec3 direction = to.subtract(from).normalize();
        vertices.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z)
            .setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
        vertices.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z)
            .setColor(color).setNormal(pose, (float) direction.x, (float) direction.y, (float) direction.z);
    }

    private PortalPairingMarkerRenderer() { }
}
