package dev.riftgun.client.render;

final class PortalShaderCompatibility {
    static PortalSurfaceRenderPath currentPath() {
        return PortalRenderFrameState.current().surfaceRenderPath();
    }

    static PortalSurfaceRenderPath selectPath(boolean shaderPackActive, boolean shadowPass) {
        if (shaderPackActive && shadowPass) return PortalSurfaceRenderPath.SKIP_SURFACE;
        if (shaderPackActive) return PortalSurfaceRenderPath.VANILLA_FALLBACK;
        return PortalSurfaceRenderPath.CUSTOM;
    }

    private PortalShaderCompatibility() {}
}
