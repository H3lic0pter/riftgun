package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
