package dev.riftgun.client.render;

/** Shader-pack activation sampled once per frame, with pass-local shadow state resolved on use. */
public final class PortalRenderFrameState {
    private static volatile PortalRenderFrameState current =
        new PortalRenderFrameState(false, () -> PortalShaderEnvironment.State.INACTIVE);

    private final boolean shaderPackActive;
    private final PortalShaderEnvironment environment;

    private PortalRenderFrameState(boolean shaderPackActive, PortalShaderEnvironment environment) {
        this.shaderPackActive = shaderPackActive;
        this.environment = environment;
    }

    public static PortalRenderFrameState current() {
        return current;
    }

    public static void refresh() {
        refresh(EnvironmentHolder.ENVIRONMENT);
    }

    static void refresh(PortalShaderEnvironment environment) {
        PortalShaderEnvironment.State snapshot = environment.snapshot();
        current = new PortalRenderFrameState(snapshot.shaderPackActive(), environment);
    }

    public PortalSurfaceRenderPath surfaceRenderPath() {
        return PortalShaderCompatibility.selectPath(
            shaderPackActive, shaderPackActive && environment.shadowPass());
    }

    private static final class EnvironmentHolder {
        private static final PortalShaderEnvironment ENVIRONMENT = IrisPortalShaderEnvironment.detect();
    }
}
