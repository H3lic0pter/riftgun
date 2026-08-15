package dev.riftgun.client.render;

final class PortalShaderCompatibility {
    static PortalSurfaceRenderPath currentPath() {
        PortalShaderEnvironment.State state = EnvironmentHolder.ENVIRONMENT.snapshot();
        return selectPath(state.shaderPackActive(), state.shadowPass());
    }

    static PortalSurfaceRenderPath selectPath(boolean shaderPackActive, boolean shadowPass) {
        if (shaderPackActive && shadowPass) return PortalSurfaceRenderPath.SKIP_SURFACE;
        if (shaderPackActive) return PortalSurfaceRenderPath.VANILLA_FALLBACK;
        return PortalSurfaceRenderPath.CUSTOM;
    }

    private static final class EnvironmentHolder {
        private static final PortalShaderEnvironment ENVIRONMENT = IrisPortalShaderEnvironment.detect();
    }

    private PortalShaderCompatibility() {}
}
