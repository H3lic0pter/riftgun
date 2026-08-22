package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ImmersivePortalRenderPolicyTest {
    @Test
    void relocationVisualUsesImmersiveCoverWhenCompatibilityIsAvailable() {
        assertEquals(ImmersivePortalRenderPolicy.Mode.LOADING_COVER,
            ImmersivePortalRenderPolicy.choose(true, false));
    }

    @Test
    void swirlIsReservedForUnavailableCompatibility() {
        assertEquals(ImmersivePortalRenderPolicy.Mode.SWIRL,
            ImmersivePortalRenderPolicy.choose(false, false));
        assertEquals(ImmersivePortalRenderPolicy.Mode.PORTAL_PROXY,
            ImmersivePortalRenderPolicy.choose(true, true));
    }
}
