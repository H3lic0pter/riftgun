package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PortalShaderCompatibilityTest {
    @Test
    void keepsCustomRendererWithoutAnActiveShaderPack() {
        assertEquals(PortalSurfaceRenderPath.CUSTOM,
            PortalShaderCompatibility.selectPath(false, false));
    }

    @Test
    void usesVanillaFallbackWithAnActiveShaderPack() {
        assertEquals(PortalSurfaceRenderPath.VANILLA_FALLBACK,
            PortalShaderCompatibility.selectPath(true, false));
    }

    @Test
    void skipsPortalSurfaceDuringShaderShadowPass() {
        assertEquals(PortalSurfaceRenderPath.SKIP_SURFACE,
            PortalShaderCompatibility.selectPath(true, true));
    }

    @Test
    void samplesEnvironmentOnlyWhenFrameStateRefreshes() {
        AtomicInteger snapshots = new AtomicInteger();
        PortalShaderEnvironment environment = () -> {
            snapshots.incrementAndGet();
            return PortalShaderEnvironment.State.COMPATIBILITY_FALLBACK;
        };
        try {
            PortalRenderFrameState.refresh(environment);
            for (int portal = 0; portal < 30; portal++) {
                assertEquals(PortalSurfaceRenderPath.VANILLA_FALLBACK,
                    PortalShaderCompatibility.currentPath());
            }
            assertEquals(1, snapshots.get());

            PortalRenderFrameState.refresh(environment);
            assertEquals(2, snapshots.get());
        } finally {
            PortalRenderFrameState.refresh(() -> PortalShaderEnvironment.State.INACTIVE);
        }
    }
}
