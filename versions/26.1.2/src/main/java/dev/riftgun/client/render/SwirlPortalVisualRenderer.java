package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SwirlPortalVisualRenderer implements PortalVisualRenderer {
    private static final int EDGE_SEGMENTS = 48;
    private static final float FALLBACK_BRIGHTNESS_BOOST = 0.80F;
    private static final float TAU = (float) (Math.PI * 2.0);

    @Override
    public void submit(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalVisualSource portal = context.portal();
        PortalPlacement placement = portal.placement();
        PortalRenderBasis basis = PortalRenderBasis.from(portal);
        float eased = Mth.sin(progress * Mth.HALF_PI);
        float width = portal.portalWidth() * eased * SwirlVisualGeometry.visibleWidthScale(placement);
        float height = portal.portalHeight() * eased * SwirlVisualGeometry.visibleHeightScale(placement);
        boolean horizontal = portal.orientation() != PortalOrientation.VERTICAL;
        boolean animated = SwirlVisualOptions.animationEnabled();
        float shimmer = animated ? 0.96F + Mth.sin(context.age() * 0.18F) * 0.04F : 1.0F;
        // Side portals (1x1 compact and expanded) share the top/bottom texture mapping
        // so the swirl fills the face at the same coverage as horizontal portals.
        boolean mapped = horizontal || placement.geometry() == PortalGeometry.SURFACE_COMPACT
            || placement.geometry().expanded();
        // Top/bottom faces lie flat on the same plane; sides keep a thin gap.
        float depth = horizontal ? 0.0F : SwirlVisualGeometry.DEPTH;
        final float normalOffset;
        if (placement.anchored()) {
            Vec3 wallFace = Vec3.atCenterOf(placement.anchor())
                .add(placement.normal().scale(0.5));
            double centerDistance = placement.center().subtract(wallFace).dot(placement.normal());
            normalOffset = SwirlVisualGeometry.anchoredCenterOffset(centerDistance);
        } else {
            normalOffset = 0.0F;
        }
        float phase = phase(portal);
        // The rotation angle is baked into the lightmap attribute per frame (0..TAU), so the
        // GPU surface/glow shaders stay uniform-free and the animation settings apply live.
        float rotation = animated
            ? swirlRotation(context.age(), (float) SwirlVisualOptions.outerPeriod(), phase)
            : 0.0F;
        PortalSurfaceRenderPath path = PortalShaderCompatibility.currentPath();
        if (path != PortalSurfaceRenderPath.SKIP_SURFACE) {
            context.submit(PortalRenderTypes.swirl(), (pose, vertices) -> drawSurface(
                pose.pose(), basis, vertices, width, height, depth, normalOffset,
                context.style().surfaceColor(), shimmer, rotation, mapped));
            context.submit(PortalRenderTypes.swirlGlow(), (pose, vertices) -> drawSurface(
                pose.pose(), basis, vertices, width, height, depth, normalOffset,
                context.style().surfaceColor(), shimmer * FALLBACK_BRIGHTNESS_BOOST, rotation, mapped));
        }
        context.submit(PortalRenderTypes.swirlEdge(), (pose, vertices) -> drawEdge(pose.pose(), basis,
            vertices, width, height, depth, normalOffset, context.style().surfaceColor(), shimmer));
    }

    static float swirlRotation(float ageTicks, float periodSeconds, float phase) {
        float turns = ageTicks / (Math.max(periodSeconds, 0.1F) * 20.0F) + phase;
        float fraction = turns - (float) Math.floor(turns);
        return fraction * TAU;
    }

    private static void drawSurface(Matrix4f matrix, PortalRenderBasis basis, VertexConsumer vertices,
                                    float width, float height, float depth, float normalOffset,
                                    int color, float shimmer, float rotation, boolean mapped) {
        float red = red(color) * shimmer;
        float green = green(color) * shimmer;
        float blue = blue(color) * shimmer;
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float hd = depth * 0.5F;
        float front = normalOffset + hd;
        float back = normalOffset - hd;
        int encoded = (int) (rotation / TAU * 65535.0F);
        int mappedFlag = mapped ? 1 : 0;

        // Opposite winding plus viewer-relative UVs prevents the two opaque faces from overlapping.
        vertex(vertices, matrix, basis.at(-hw, -hh, front), red, green, blue, encoded, mappedFlag, 0, 0);
        vertex(vertices, matrix, basis.at(hw, -hh, front), red, green, blue, encoded, mappedFlag, 1, 0);
        vertex(vertices, matrix, basis.at(hw, hh, front), red, green, blue, encoded, mappedFlag, 1, 1);
        vertex(vertices, matrix, basis.at(-hw, hh, front), red, green, blue, encoded, mappedFlag, 0, 1);

        vertex(vertices, matrix, basis.at(hw, -hh, back), red, green, blue, encoded, mappedFlag, 0, 0);
        vertex(vertices, matrix, basis.at(-hw, -hh, back), red, green, blue, encoded, mappedFlag, 1, 0);
        vertex(vertices, matrix, basis.at(-hw, hh, back), red, green, blue, encoded, mappedFlag, 1, 1);
        vertex(vertices, matrix, basis.at(hw, hh, back), red, green, blue, encoded, mappedFlag, 0, 1);
    }

    private static void vertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                               float red, float green, float blue, int rotationEncoded,
                               int mapped, float u, float v) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 1.0F)
            .setUv(u, v)
            .setUv2(rotationEncoded, mapped);
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

    private static float phase(PortalVisualSource portal) {
        return ((portal.visualId().hashCode() & 255) + 0.5F) / 256.0F;
    }

    private static float red(int color) { return ((color >> 16) & 255) / 255.0F; }
    private static float green(int color) { return ((color >> 8) & 255) / 255.0F; }
    private static float blue(int color) { return (color & 255) / 255.0F; }
}
