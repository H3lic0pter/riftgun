package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.portal.PortalGeometry;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SwirlPortalVisualRenderer implements PortalVisualRenderer {
    private static final int EDGE_SEGMENTS = 48;
    private static final int FALLBACK_SURFACE_SEGMENTS = 48;
    private static final float FALLBACK_BRIGHTNESS_BOOST = 0.80F;

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
        // 26.1.2 has no per-draw scalar uniform API, so the custom swirl shader is gone and
        // both surface paths run the CPU animation; the shadow pass keeps skipping the surface.
        PortalSurfaceRenderPath path = PortalShaderCompatibility.currentPath();
        if (path != PortalSurfaceRenderPath.SKIP_SURFACE) {
            context.submit(PortalRenderTypes.swirlFallback(), (pose, vertices) -> drawFallbackFace(
                pose.pose(), basis, vertices, width, height, depth, normalOffset,
                context.style().surfaceColor(), shimmer, phase, mapped,
                context.age(), animated ? (float) SwirlVisualOptions.outerPeriod() : 0.0F, animated));
            context.submit(PortalRenderTypes.swirlFallbackGlow(), (pose, vertices) -> drawFallbackFace(
                pose.pose(), basis, vertices, width, height, depth, normalOffset,
                context.style().surfaceColor(), shimmer * FALLBACK_BRIGHTNESS_BOOST,
                phase, mapped, context.age(),
                animated ? (float) SwirlVisualOptions.outerPeriod() : 0.0F, animated));
        }
        context.submit(PortalRenderTypes.swirlEdge(), (pose, vertices) -> drawEdge(pose.pose(), basis,
            vertices, width, height, depth, normalOffset, context.style().surfaceColor(), shimmer));
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

        drawFallbackDisc(vertices, matrix, basis, hw, hh, faces.front(), basis.normal(),
            red, green, blue, ageTicks, periodSeconds, phase, animated, mapped, false);

        if (!faces.hasDistinctBack()) return;
        drawFallbackDisc(vertices, matrix, basis, hw, hh, faces.back(), basis.normal().scale(-1.0),
            red, green, blue, ageTicks, periodSeconds, phase, animated, mapped, true);
    }

    private static void drawFallbackDisc(VertexConsumer vertices, Matrix4f matrix,
                                         PortalRenderBasis basis, float halfWidth, float halfHeight,
                                         float faceOffset, Vec3 normal,
                                         float red, float green, float blue,
                                         float ageTicks, float periodSeconds, float phase,
                                         boolean animated, boolean mapped, boolean backFace) {
        float rotationDirection = backFace ? -1.0F : 1.0F;
        for (int segment = 0; segment < FALLBACK_SURFACE_SEGMENTS; segment++) {
            SwirlFallbackGeometry.RimPoint first =
                SwirlFallbackGeometry.rimPoint(segment, FALLBACK_SURFACE_SEGMENTS);
            SwirlFallbackGeometry.RimPoint second =
                SwirlFallbackGeometry.rimPoint(segment + 1, FALLBACK_SURFACE_SEGMENTS);

            fallbackVertex(vertices, matrix, basis.at(0, 0, faceOffset), normal, red, green, blue,
                rotatedUv(0.5F, 0.5F, ageTicks, periodSeconds, phase, animated, mapped,
                    rotationDirection));
            fallbackRimVertex(vertices, matrix, basis, halfWidth, halfHeight, faceOffset, normal,
                red, green, blue, first, backFace, ageTicks, periodSeconds, phase, animated, mapped,
                rotationDirection);
            fallbackRimVertex(vertices, matrix, basis, halfWidth, halfHeight, faceOffset, normal,
                red, green, blue, second, backFace, ageTicks, periodSeconds, phase, animated, mapped,
                rotationDirection);
            // The RenderType consumes quads; repeating the third vertex makes the second triangle degenerate.
            fallbackRimVertex(vertices, matrix, basis, halfWidth, halfHeight, faceOffset, normal,
                red, green, blue, second, backFace, ageTicks, periodSeconds, phase, animated, mapped,
                rotationDirection);
        }
    }

    private static void fallbackRimVertex(VertexConsumer vertices, Matrix4f matrix,
                                          PortalRenderBasis basis, float halfWidth, float halfHeight,
                                          float faceOffset, Vec3 normal,
                                          float red, float green, float blue,
                                          SwirlFallbackGeometry.RimPoint point, boolean backFace,
                                          float ageTicks, float periodSeconds, float phase,
                                          boolean animated, boolean mapped, float rotationDirection) {
        float x = point.x() * halfWidth * (backFace ? -1.0F : 1.0F);
        float y = point.y() * halfHeight;
        fallbackVertex(vertices, matrix, basis.at(x, y, faceOffset), normal, red, green, blue,
            rotatedUv(point.u(), point.v(), ageTicks, periodSeconds, phase, animated, mapped,
                rotationDirection));
    }

    private static SwirlFallbackAnimation.Uv rotatedUv(float u, float v, float ageTicks,
                                                       float periodSeconds, float phase,
                                                       boolean animated, boolean mapped,
                                                       float rotationDirection) {
        SwirlFallbackAnimation.Uv rotated = SwirlFallbackAnimation.rotate(
            u, v, ageTicks, periodSeconds, phase, animated, rotationDirection);
        if (!mapped) return rotated;

        float mappedU = 63.5F / 128.0F + (rotated.u() - 0.5F) * (110.0F / 128.0F);
        float mappedV = 62.5F / 128.0F + (rotated.v() - 0.5F) * (104.0F / 128.0F);
        return new SwirlFallbackAnimation.Uv(mappedU, mappedV);
    }

    private static void fallbackVertex(VertexConsumer vertices, Matrix4f matrix, Vec3 point,
                                       Vec3 normal, float red, float green, float blue,
                                       SwirlFallbackAnimation.Uv uv) {
        vertices.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z)
            .setColor(red, green, blue, 1.0F)
            .setUv(uv.u(), uv.v())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT)
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
