package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.riftgun.internal.shader.ShaderPackProfile;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
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
    void snapshotsActivationOnceAndResolvesPassStateOnUse() {
        AtomicInteger frameSnapshots = new AtomicInteger();
        AtomicInteger shadowPassQueries = new AtomicInteger();
        PortalShaderEnvironment environment = new PortalShaderEnvironment() {
            @Override
            public State snapshot() {
                frameSnapshots.incrementAndGet();
                return State.COMPATIBILITY_FALLBACK;
            }

            @Override
            public boolean shadowPass() {
                shadowPassQueries.incrementAndGet();
                return false;
            }
        };
        try {
            PortalRenderFrameState.refresh(environment);
            for (int portal = 0; portal < 30; portal++) {
                assertEquals(PortalSurfaceRenderPath.VANILLA_FALLBACK,
                    PortalShaderCompatibility.currentPath());
            }
            assertEquals(1, frameSnapshots.get());
            assertEquals(30, shadowPassQueries.get());

            PortalRenderFrameState.refresh(environment);
            assertEquals(2, frameSnapshots.get());
        } finally {
            PortalRenderFrameState.refresh(() -> PortalShaderEnvironment.State.INACTIVE);
        }
    }

    @Test
    void observesShadowPassChangesWithinTheFrame() {
        AtomicBoolean shadowPass = new AtomicBoolean();
        PortalShaderEnvironment environment = new PortalShaderEnvironment() {
            @Override
            public State snapshot() {
                return State.COMPATIBILITY_FALLBACK;
            }

            @Override
            public boolean shadowPass() {
                return shadowPass.get();
            }
        };
        try {
            PortalRenderFrameState.refresh(environment);
            assertEquals(PortalSurfaceRenderPath.VANILLA_FALLBACK,
                PortalShaderCompatibility.currentPath());

            shadowPass.set(true);
            assertEquals(PortalSurfaceRenderPath.SKIP_SURFACE,
                PortalShaderCompatibility.currentPath());
        } finally {
            PortalRenderFrameState.refresh(() -> PortalShaderEnvironment.State.INACTIVE);
        }
    }

    @Test
    void resolvesTheRegisteredShaderProfileOncePerFrame() {
        try {
            PortalRenderFrameState.refresh(() ->
                PortalShaderEnvironment.State.active("ComplementaryUnbound_r5.8.1.zip"));
            ShaderPackProfile.EndframeCenter center =
                PortalRenderFrameState.current().shaderPackProfile().endframeCenter();
            assertEquals(ShaderPackProfile.EndframeCenter.Mode.IRIS_BLOCK_ENTITY, center.mode());
            assertEquals(5025, center.materialId());

            PortalRenderFrameState.refresh(() ->
                PortalShaderEnvironment.State.active("ComplementaryUnbound_r6.0.zip"));
            assertSame(ShaderPackProfile.EMPTY,
                PortalRenderFrameState.current().shaderPackProfile());
        } finally {
            PortalRenderFrameState.refresh(() -> PortalShaderEnvironment.State.INACTIVE);
        }
    }
}
