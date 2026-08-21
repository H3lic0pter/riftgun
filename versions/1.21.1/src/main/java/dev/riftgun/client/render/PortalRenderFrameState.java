package dev.riftgun.client.render;

/** Immutable shader-compatibility state sampled exactly once before each render frame. */
public record PortalRenderFrameState(PortalSurfaceRenderPath surfaceRenderPath) {
    private static volatile PortalRenderFrameState current =
        new PortalRenderFrameState(PortalSurfaceRenderPath.CUSTOM);

    public static PortalRenderFrameState current() {
        return current;
    }

    public static void refresh() {
        refresh(EnvironmentHolder.ENVIRONMENT);
    }

    static void refresh(PortalShaderEnvironment environment) {
        PortalShaderEnvironment.State snapshot = environment.snapshot();
        current = new PortalRenderFrameState(PortalShaderCompatibility.selectPath(
            snapshot.shaderPackActive(), snapshot.shadowPass()));
    }

    private static final class EnvironmentHolder {
        private static final PortalShaderEnvironment ENVIRONMENT = IrisPortalShaderEnvironment.detect();
    }
}
