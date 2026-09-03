package dev.riftgun.client.model;

import dev.riftgun.core.visual.PortalGunVisualSnapshot;

/** Tint-index contract used to filter the canonical Portal Gun model into baked variants. */
public final class PortalGunModelLayers {
    public static final int VARIANT_COUNT = PortalGunVisualSnapshot.VARIANT_COUNT;

    enum RenderLayer {
        BODY,
        CORE,
        FLUID,
        GLASS
    }

    enum RenderPass {
        OPAQUE,
        TRANSLUCENT
    }

    public static boolean includesTint(int geometryKey, int tintIndex) {
        return PortalGunVisualSnapshot.includesTint(geometryKey, tintIndex);
    }

    static RenderLayer renderLayer(int tintIndex) {
        if (tintIndex == 1) return RenderLayer.GLASS;
        if (tintIndex >= 2 && tintIndex <= 8) return RenderLayer.FLUID;
        if (tintIndex == 9 || tintIndex == 10) return RenderLayer.CORE;
        return RenderLayer.BODY;
    }

    static RenderPass renderPass(int tintIndex) {
        return renderLayer(tintIndex) == RenderLayer.BODY
            ? RenderPass.OPAQUE
            : RenderPass.TRANSLUCENT;
    }

    private PortalGunModelLayers() {}
}
