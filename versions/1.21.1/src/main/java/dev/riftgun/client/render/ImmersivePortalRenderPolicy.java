package dev.riftgun.client.render;

final class ImmersivePortalRenderPolicy {
    enum Mode {
        SWIRL,
        LOADING_COVER,
        PORTAL_PROXY
    }

    static Mode choose(boolean compatibilityAvailable, boolean supportsPortalProxy) {
        if (!compatibilityAvailable) return Mode.SWIRL;
        return supportsPortalProxy ? Mode.PORTAL_PROXY : Mode.LOADING_COVER;
    }

    private ImmersivePortalRenderPolicy() {}
}
