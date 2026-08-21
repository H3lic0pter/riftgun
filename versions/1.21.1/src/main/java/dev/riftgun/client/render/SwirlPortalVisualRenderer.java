package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SwirlPortalVisualRenderer implements PortalVisualRenderer {
    private static final int EDGE_SEGMENTS = 48;
    private static final float TICKS_PER_SECOND = 20.0F;
    private static final float GLOW_BRIGHTNESS_MULTIPLIER = 0.45F;
    private static final float TAU = (float) (Math.PI * 2.0);

    @Override
    public void render(PortalVisualRenderContext context) {
        float progress = context.visibleProgress();
        if (progress <= 0.0F) return;

        PortalVisualSource portal = context.portal();
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
        // Side portals (1x1 compact and expanded) share the top/bottom texture mapping
        // so the swirl fills the face at the same coverage as horizontal portals.
        boolean mapped = horizontal || placement.geometry() == PortalGeometry.SURFACE_COMPACT
            || placement.geometry().expanded();
        // Top/bottom faces lie flat on the same plane; sides keep a thin gap.
        float depth = horizontal ? 0.0F : SwirlVisualGeometry.DEPTH;
        float normalOffset = 0.0F;
        if (placement.anchored()) {
            Vec3 wallFace = Vec3.atCenterOf(placement.anchor())
                .add(placement.normal().scale(0.5));
            double centerDistance = placement.center().subtract(wallFace).dot(placement.normal());
            normalOffset = SwirlVisualGeometry.anchoredCenterOffset(centerDistance);
        }
        float phase = phase(portal);
        PortalSurfaceRenderPath path = context.surfaceRenderPath();
        if (path == PortalSurfaceRenderPath.CUSTOM) {
            drawFaces(matrix, basis, context.buffers().getBuffer(PortalRenderTypes.swirl()), width, height,
                depth, normalOffset, context.style().surfaceColor(), shimmer, phase, mapped);
        } else if (path == PortalSurfaceRenderPath.VANILLA_FALLBACK) {
            drawFallbackFace(matrix, basis,
                context.buffers().getBuffer(PortalRenderTypes.swirlFallback()), width, height,
                depth, normalOffset, context.style().surfaceColor(), shimmer, phase, mapped,
                context.age(), animated ? (float) SwirlVisualOptions.outerPeriod() : 0.0F, animated);
            drawFallbackFace(matrix, basis,
                context.buffers().getBuffer(PortalRenderTypes.swirlFallbackGlow()), width, height,
                depth, normalOffset, context.style().surfaceColor(), shimmer * GLOW_BRIGHTNESS_MULTIPLIER,
                phase, mapped, context.age(),
                animated ? (float) SwirlVisualOptions.outerPeriod() : 0.0F, animated);
        }
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

    private static void drawFallbackFace(Matrix4f matrix, PortalRenderBasis basis,
                                         VertexConsumer vertices, float width, float height,
                                         float depth, float normalOffset, int color, float shimmer,
                                         float phase, boolean mapped, float ageTicks,
                                         float periodSeconds, boolean animated) {
        float red = red(color) * shimmer;
        float green = green(color) * shimmer;
        float blue = blue(color) * shimmer;
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        SwirlFallbackGeometry.FaceOffsets faces =
            SwirlFallbackGeometry.faceOffsets(normalOffset, depth);

        drawFallbackQuad(vertices, matrix, basis, hw, hh, faces.front(), basis.normal(),
            red, green, blue, ageTicks, periodSeconds, phase, animated, mapped, false);

        if (!faces.hasDistinctBack()) return;
        drawFallbackQuad(vertices, matrix, basis, hw, hh, faces.back(), basis.normal().scale(-1.0),
            red, green, blue, ageTicks, periodSeconds, phase, animated, mapped, true);
    }

    private static void drawFallbackQuad(VertexConsumer vertices, Matrix4f matrix,
                                         PortalRenderBasis basis, float halfWidth, float halfHeight,
                                         float faceOffset, Vec3 normal,
                                         float red, float green, float blue,
                                         float ageTicks, float periodSeconds, float phase,
                                         boolean animated, boolean mapped, boolean backFace) {
        double cosine = 1.0;
        double sine = 0.0;
        if (animated) {
            float direction = SwirlFallbackGeometry.rotationDirection(backFace);
            float turns = ageTicks / (Math.max(periodSeconds, 0.1F) * TICKS_PER_SECOND) * direction
                + phase;
            double angle = (turns - Math.floor(turns)) * TAU;
            cosine = Math.cos(angle);
            sine = Math.sin(angle);
        }

        if (backFace) {
            fallbackVertex(vertices, matrix, basis.at(halfWidth, -halfHeight, faceOffset), normal,
                red, green, blue, 0, 0, cosine, sine, mapped);
            fallbackVertex(vertices, matrix, basis.at(-halfWidth, -halfHeight, faceOffset), normal,
                red, green, blue, 1, 0, cosine, sine, mapped);
            fallbackVertex(vertices, matrix, basis.at(-halfWidth, halfHeight, faceOffset), normal,
                red, green, blue, 1, 1, cosine, sine, mapped);
            fallbackVertex(vertices, matrix, basis.at(halfWidth, halfHeight, faceOffset), normal,
                red, green, blue, 0, 1, cosine, sine, mapped);
        } else {
            fallbackVertex(vertices, matrix, basis.at(-halfWidth, -halfHeight, faceOffset), normal,
                red, green, blue, 0, 0, cosine, sine, mapped);
            fallbackVertex(vertices, matrix, basis.at(halfWidth, -halfHeight, faceOffset), normal,
                red, green, blue, 1, 0, cosine, sine, mapped);
            fallbackVertex(vertices, matrix, basis.at(halfWidth, halfHeight, faceOffset), normal,
                red, green, blue, 1, 1, cosine, sine, mapped);
            fallbackVertex(vertices, matrix, basis.at(-halfWidth, halfHeight, faceOffset), normal,
                red, green, blue, 0, 1, cosine, sine, mapped);
        }
    }

    private static void fallbackVertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                                       Vec3 normal, float red, float green, float blue,
                                       float u, float v, double cosine, double sine,
                                       boolean mapped) {
        float centeredU = u - 0.5F;
        float centeredV = v - 0.5F;
        float rotatedU = (float) (centeredU * cosine - centeredV * sine) + 0.5F;
        float rotatedV = (float) (centeredU * sine + centeredV * cosine) + 0.5F;
        if (mapped) {
            rotatedU = 63.5F / 128.0F + (rotatedU - 0.5F) * (110.0F / 128.0F);
            rotatedV = 62.5F / 128.0F + (rotatedV - 0.5F) * (104.0F / 128.0F);
        }
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 1.0F)
            .setUv(rotatedU, rotatedV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
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
