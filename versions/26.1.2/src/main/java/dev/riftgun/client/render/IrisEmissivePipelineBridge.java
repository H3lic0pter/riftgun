package dev.riftgun.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.fml.ModList;

/** Optional Iris shader mapping, resolved once; no reflection or registration in the frame loop. */
final class IrisEmissivePipelineBridge {
    private static final RenderPipeline MAGIC_CIRCLE = resolve();

    static RenderPipeline magicCircle() {
        return MAGIC_CIRCLE;
    }

    private static RenderPipeline resolve() {
        RenderPipeline original = RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
        if (!ModList.get().isLoaded("iris")) return original;
        RenderPipeline depthWriting = PortalRenderTypes.Pipelines.MAGIC_CIRCLE_SHADER;
        try {
            Class<?> pipelines = Class.forName("net.irisshaders.iris.pipeline.IrisPipelines", true,
                IrisEmissivePipelineBridge.class.getClassLoader());
            // Iris exposes this mapping copy for pipeline variants; retain both main and shadow mappings.
            pipelines.getMethod("copyPipeline", RenderPipeline.class, RenderPipeline.class)
                .invoke(null, original, depthWriting);
            return depthWriting;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            LogUtils.getLogger().warn("Iris emissive pipeline mapping unavailable; magic circles retain vanilla emissive rendering without the depth fix", error);
            return original;
        }
    }

    private IrisEmissivePipelineBridge() {}
}
