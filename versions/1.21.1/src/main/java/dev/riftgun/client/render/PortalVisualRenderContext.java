package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.portal.PortalVisualSource;
import dev.riftgun.internal.shader.ShaderPackProfile;
import net.minecraft.client.renderer.MultiBufferSource;

public record PortalVisualRenderContext(
    PortalVisualSource portal,
    float partialTick,
    PoseStack poseStack,
    MultiBufferSource buffers,
    int packedLight,
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
}
