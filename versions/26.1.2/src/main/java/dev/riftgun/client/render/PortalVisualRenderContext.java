package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.internal.shader.ShaderPackProfile;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Render submission context for portal visuals.
 *
 * <p>26.1.2 replaces the 1.21.x {@code MultiBufferSource} hand-off with node-collector
 * submissions: each visual submits one geometry callback per render type and the collector
 * replays them into the frame's buffer source.
 */
public record PortalVisualRenderContext(
    PortalVisualSource portal,
    float partialTick,
    PoseStack poseStack,
    SubmitNodeCollector collector,
    PortalSurfaceRenderPath surfaceRenderPath,
    ShaderPackProfile shaderPackProfile,
    PortalVisualStyle style
) {
    public float visibleProgress() {
        return portal.visualProgress(partialTick);
    }

    public float age() {
        return portal.visualAge(partialTick);
    }

    public void submit(RenderType renderType, Geometry geometry) {
        collector.submitCustomGeometry(poseStack, renderType, geometry::render);
    }

    @FunctionalInterface
    public interface Geometry {
        void render(PoseStack.Pose pose, VertexConsumer buffer);
    }
}
