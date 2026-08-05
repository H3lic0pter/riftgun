package dev.riftgun.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;
import net.minecraft.client.renderer.MultiBufferSource;

public record PortalVisualRenderContext(
    PortalEntity portal,
    float partialTick,
    PoseStack poseStack,
    MultiBufferSource buffers,
    int packedLight,
    PortalVisualStyle style
) {
    public float visibleProgress() {
        return PortalLifecycle.visibleProgress(portal.phase(), portal.phaseTicks(), partialTick);
    }

    public float age() {
        return portal.tickCount + partialTick;
    }
}
