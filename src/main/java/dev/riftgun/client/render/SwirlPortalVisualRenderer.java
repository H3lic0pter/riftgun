package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SwirlPortalVisualRenderer implements PortalVisualRenderer {
    private static final int EDGE_SEGMENTS = 48;

    @Override
    public void render(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalEntity portal = context.portal();
        PortalPlacement placement = portal.placement();
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        Matrix4f matrix = context.poseStack().last().pose();
        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = portal.portalWidth() * eased;
        float height = portal.portalHeight() * eased;
        boolean horizontal = portal.orientation() != PortalOrientation.VERTICAL;
        width *= SwirlVisualGeometry.visibleWidthScale(placement);
        height *= SwirlVisualGeometry.visibleHeightScale(placement);
        boolean animated = SwirlVisualOptions.animationEnabled();
        float shimmer = animated ? 0.96F + Mth.sin(context.age() * 0.18F) * 0.04F : 1.0F;
        // 1x1 side portals share the top/bottom texture mapping so the swirl fills the face.
        boolean mapped = horizontal || placement.geometry() == PortalGeometry.SURFACE_COMPACT;
        // Top/bottom faces lie flat on the same plane; sides keep a thin gap.
        float depth = horizontal ? 0.0F : SwirlVisualGeometry.DEPTH;
        float normalOffset = 0.0F;
        if (placement.anchored()) {
            Vec3 wallFace = Vec3.atCenterOf(placement.anchor())
                .add(placement.normal().scale(0.5));
            double centerDistance = portal.position().subtract(wallFace).dot(placement.normal());
            normalOffset = SwirlVisualGeometry.anchoredCenterOffset(centerDistance);
        }
        float phase = phase(portal);
        drawFaces(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.swirl()), width, height,
            depth, normalOffset, context.style().surfaceColor(), shimmer, phase, mapped);
        drawEdge(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.swirlEdge()), width, height,
            depth, normalOffset, context.style().surfaceColor(), shimmer);
    }

    private static void drawFaces(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                  float width, float height, float depth, float normalOffset,
                                  int color, float shimmer, float phase, boolean horizontal) {
        float red = red(color) * shimmer;
        float green = green(color) * shimmer;
        float blue = blue(color) * shimmer;
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float hd = depth * 0.5F;
        float front = normalOffset + hd;
        float back = normalOffset - hd;

        // Opposite winding plus viewer-relative UVs prevents the two opaque faces from overlapping.
        vertex(vertices, matrix, basis.at(-hw, -hh, front), red, green, blue, phase, horizontal, 0, 0);
        vertex(vertices, matrix, basis.at(hw, -hh, front), red, green, blue, phase, horizontal, 1, 0);
        vertex(vertices, matrix, basis.at(hw, hh, front), red, green, blue, phase, horizontal, 1, 1);
        vertex(vertices, matrix, basis.at(-hw, hh, front), red, green, blue, phase, horizontal, 0, 1);

        vertex(vertices, matrix, basis.at(hw, -hh, back), red, green, blue, phase, horizontal, 0, 0);
        vertex(vertices, matrix, basis.at(-hw, -hh, back), red, green, blue, phase, horizontal, 1, 0);
        vertex(vertices, matrix, basis.at(-hw, hh, back), red, green, blue, phase, horizontal, 1, 1);
        vertex(vertices, matrix, basis.at(hw, hh, back), red, green, blue, phase, horizontal, 0, 1);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               float red, float green, float blue, float phase,
                               boolean horizontal, float u, float v) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, phase).setUv(u, v).setUv2(horizontal ? 1 : 0, 0);
    }

    private static void drawEdge(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                 float width, float height, float depth, float normalOffset,
                                 int color, float shimmer) {
        float red = red(color) * shimmer;
        float green = green(color) * shimmer;
        float blue = blue(color) * shimmer;
        float hw = width * 0.5F * SwirlVisualGeometry.EDGE_RADIUS_SCALE;
        float hh = height * 0.5F * SwirlVisualGeometry.EDGE_RADIUS_SCALE;
        float front = normalOffset + depth * 0.5F;
        float back = normalOffset - depth * 0.5F;
        for (int segment = 0; segment < EDGE_SEGMENTS; segment++) {
            double firstAngle = (Math.PI * 2.0 * segment) / EDGE_SEGMENTS;
            double secondAngle = (Math.PI * 2.0 * (segment + 1)) / EDGE_SEGMENTS;
            float x1 = (float) Math.cos(firstAngle) * hw;
            float y1 = (float) Math.sin(firstAngle) * hh;
            float x2 = (float) Math.cos(secondAngle) * hw;
            float y2 = (float) Math.sin(secondAngle) * hh;
            edgeVertex(vertices, matrix, basis.at(x1, y1, back), red, green, blue);
            edgeVertex(vertices, matrix, basis.at(x2, y2, back), red, green, blue);
            edgeVertex(vertices, matrix, basis.at(x2, y2, front), red, green, blue);
            edgeVertex(vertices, matrix, basis.at(x1, y1, front), red, green, blue);
        }
    }

    private static void edgeVertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                                   float red, float green, float blue) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 1.0F);
    }

    private static float phase(PortalEntity portal) {
        return ((portal.getUUID().hashCode() & 255) + 0.5F) / 256.0F;
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }
}
