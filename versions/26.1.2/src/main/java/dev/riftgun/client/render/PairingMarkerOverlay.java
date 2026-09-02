package dev.riftgun.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.pairing.PortalPairingPreviewGeometry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Shader-safe screen projection of world-oriented pairing marker lines. */
final class PairingMarkerOverlay implements GuiElementRenderState {
    private static final float PHYSICAL_LINE_WIDTH = 2.5F;
    private static final float MINIMUM_CLIP_W = 1.0E-4F;

    private final List<ScreenSegment> segments;
    private final float halfWidth;
    private final ScreenRectangle viewport;
    private final ScreenRectangle bounds;

    private PairingMarkerOverlay(List<ScreenSegment> segments, float halfWidth,
                                 ScreenRectangle viewport, ScreenRectangle bounds) {
        this.segments = List.copyOf(segments);
        this.halfWidth = halfWidth;
        this.viewport = viewport;
        this.bounds = bounds;
    }

    static @Nullable PairingMarkerOverlay project(
        List<PortalPairingPreviewGeometry.ColoredSegment> pending,
        List<PortalPairingPreviewGeometry.ColoredSegment> entityTarget,
        Vec3 camera, Matrix4fc projection, Matrix4fc viewRotation,
        int guiWidth, int guiHeight, double guiScale
    ) {
        if (guiWidth <= 0 || guiHeight <= 0 || guiScale <= 0.0) return null;
        Matrix4f viewProjection = new Matrix4f(projection).mul(viewRotation);
        List<ScreenSegment> projected = new ArrayList<>(pending.size() + entityTarget.size());
        project(projected, pending, camera, viewProjection, guiWidth, guiHeight);
        project(projected, entityTarget, camera, viewProjection, guiWidth, guiHeight);
        if (projected.isEmpty()) return null;

        float halfWidth = halfLineWidth(guiScale);
        ScreenRectangle viewport = new ScreenRectangle(0, 0, guiWidth, guiHeight);
        ScreenRectangle bounds = bounds(projected, halfWidth, viewport);
        return bounds == null ? null
            : new PairingMarkerOverlay(projected, halfWidth, viewport, bounds);
    }

    private static void project(
        List<ScreenSegment> output,
        List<PortalPairingPreviewGeometry.ColoredSegment> source,
        Vec3 camera, Matrix4fc viewProjection, int width, int height
    ) {
        for (PortalPairingPreviewGeometry.ColoredSegment colored : source) {
            ProjectedPoint from = projectPoint(colored.geometry().from(), camera,
                viewProjection, width, height);
            ProjectedPoint to = projectPoint(colored.geometry().to(), camera,
                viewProjection, width, height);
            if (from == null || to == null || outsideSameEdge(from, to, width, height)) continue;
            output.add(new ScreenSegment(from.x(), from.y(), to.x(), to.y(),
                colored.color() | 0xFF000000));
        }
    }

    static @Nullable ProjectedPoint projectPoint(Vec3 point, Vec3 camera,
                                                 Matrix4fc viewProjection,
                                                 int width, int height) {
        Vector4f clip = new Vector4f((float) (point.x - camera.x),
            (float) (point.y - camera.y), (float) (point.z - camera.z), 1.0F);
        viewProjection.transform(clip);
        if (!Float.isFinite(clip.w) || clip.w <= MINIMUM_CLIP_W) return null;
        float inverseW = 1.0F / clip.w;
        float x = (clip.x * inverseW * 0.5F + 0.5F) * width;
        float y = (0.5F - clip.y * inverseW * 0.5F) * height;
        return Float.isFinite(x) && Float.isFinite(y) ? new ProjectedPoint(x, y) : null;
    }

    static float halfLineWidth(double guiScale) {
        return (float) (PHYSICAL_LINE_WIDTH / guiScale * 0.5);
    }

    private static boolean outsideSameEdge(ProjectedPoint from, ProjectedPoint to,
                                           int width, int height) {
        return from.x() < 0.0F && to.x() < 0.0F
            || from.x() > width && to.x() > width
            || from.y() < 0.0F && to.y() < 0.0F
            || from.y() > height && to.y() > height;
    }

    private static @Nullable ScreenRectangle bounds(List<ScreenSegment> segments,
                                                     float halfWidth,
                                                     ScreenRectangle viewport) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (ScreenSegment segment : segments) {
            minX = Math.min(minX, Math.min(segment.x0(), segment.x1()));
            minY = Math.min(minY, Math.min(segment.y0(), segment.y1()));
            maxX = Math.max(maxX, Math.max(segment.x0(), segment.x1()));
            maxY = Math.max(maxY, Math.max(segment.y0(), segment.y1()));
        }
        int left = (int) Math.floor(minX - halfWidth);
        int top = (int) Math.floor(minY - halfWidth);
        int right = (int) Math.ceil(maxX + halfWidth);
        int bottom = (int) Math.ceil(maxY + halfWidth);
        return viewport.intersection(new ScreenRectangle(left, top,
            Math.max(0, right - left), Math.max(0, bottom - top)));
    }

    @Override
    public void buildVertices(VertexConsumer vertices) {
        for (ScreenSegment segment : segments) {
            float dx = segment.x1() - segment.x0();
            float dy = segment.y1() - segment.y0();
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length <= 1.0E-4F) continue;
            float normalX = -dy / length * halfWidth;
            float normalY = dx / length * halfWidth;
            vertices.addVertex(segment.x0() - normalX, segment.y0() - normalY, 0.0F)
                .setColor(segment.color());
            vertices.addVertex(segment.x0() + normalX, segment.y0() + normalY, 0.0F)
                .setColor(segment.color());
            vertices.addVertex(segment.x1() + normalX, segment.y1() + normalY, 0.0F)
                .setColor(segment.color());
            vertices.addVertex(segment.x1() - normalX, segment.y1() - normalY, 0.0F)
                .setColor(segment.color());
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public ScreenRectangle scissorArea() {
        return viewport;
    }

    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    record ProjectedPoint(float x, float y) {}

    private record ScreenSegment(float x0, float y0, float x1, float y1, int color) {}
}
