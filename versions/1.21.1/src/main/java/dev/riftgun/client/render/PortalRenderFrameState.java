package dev.riftgun.client.render;

import dev.riftgun.internal.shader.ShaderPackProfile;
import dev.riftgun.internal.shader.ShaderPackProfileRegistry;

/** Shader-pack activation sampled once per frame, with pass-local shadow state resolved on use. */
public final class PortalRenderFrameState {
    private static volatile PortalRenderFrameState current =
        new PortalRenderFrameState(false, ShaderPackProfile.EMPTY,
            () -> PortalShaderEnvironment.State.INACTIVE);

    private final boolean shaderPackActive;
    private final ShaderPackProfile shaderPackProfile;
    private final PortalShaderEnvironment environment;

    private PortalRenderFrameState(boolean shaderPackActive, ShaderPackProfile shaderPackProfile,
                                   PortalShaderEnvironment environment) {
        this.shaderPackActive = shaderPackActive;
        this.shaderPackProfile = shaderPackProfile;
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
        ShaderPackProfile profile = snapshot.shaderPackActive()
            ? ShaderPackProfileRegistry.resolve(snapshot.shaderPackName())
            : ShaderPackProfile.EMPTY;
        current = new PortalRenderFrameState(snapshot.shaderPackActive(), profile, environment);
    }

    public PortalSurfaceRenderPath surfaceRenderPath() {
        return PortalShaderCompatibility.selectPath(
            shaderPackActive, shaderPackActive && environment.shadowPass());
    }

    /** Cached by {@link #refresh()} once per render frame. */
    public boolean shaderPackActive() {
        return shaderPackActive;
    }

    public ShaderPackProfile shaderPackProfile() {
        return shaderPackProfile;
    }

    private static final class EnvironmentHolder {
        private static final PortalShaderEnvironment ENVIRONMENT = IrisPortalShaderEnvironment.detect();
    }
}
