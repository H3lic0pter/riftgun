package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EndframeShaderFallbackSourceTest {
    @Test
    void bothNodesUseContextPathAndLeaveTheFallbackDiscEmpty() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = read("versions/" + node
                + "/src/main/java/dev/riftgun/client/render/EndframePortalVisualRenderer.java");

            assertTrue(source.contains("context.surfaceRenderPath()"));
            assertFalse(source.contains("PortalShaderCompatibility.currentPath()"));
            assertTrue(source.contains("PortalRenderTypes.endframeStar(path)"));
            assertFalse(source.contains("PortalRenderTypes.endframeFallbackSky()"));
            assertFalse(source.contains("PortalRenderTypes.endframeFallbackPortal()"));
            int custom = source.indexOf("if (path == PortalSurfaceRenderPath.CUSTOM)");
            int star = source.indexOf("PortalRenderTypes.endframeStar(path)", custom);
            int fallback = source.indexOf("} else {", star);
            assertTrue(custom >= 0 && star > custom && fallback > star);
            assertTrue(source.contains("PortalRenderTypes.endframeFrameGlow()"));
            assertEquals(1, count(source, "Math.cos(rotation)"));
            assertEquals(1, count(source, "Math.sin(rotation)"));
        }

        String modern = read(
            "versions/26.1.2/src/main/java/dev/riftgun/client/render/EndframePortalVisualRenderer.java");
        assertTrue(modern.contains("gpuRotating, !gpuRotating"));
    }

    @Test
    void shaderFallbackUsesCutoutFrameAndNoReplacementStarTextures() throws IOException {
        String legacy = read("versions/1.21.1/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java");
        assertTrue(legacy.contains("RenderType.entityCutout(ENDFRAME_FRAME_TEXTURE)"));
        assertFalse(legacy.contains("endframeFallbackSky"));
        assertFalse(legacy.contains("endframeFallbackPortal"));

        String modern = read("versions/26.1.2/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java");
        assertFalse(modern.contains("ENDFRAME_FALLBACK_SKY"));
        assertFalse(modern.contains("ENDFRAME_FALLBACK_PORTAL"));
        int start = modern.indexOf("private static final RenderType ENDFRAME_FALLBACK");
        int end = modern.indexOf("public static RenderType portal()", start);
        assertEquals(2, count(modern.substring(start, end),
            "getClampToEdge(FilterMode.LINEAR)"));
    }

    @Test
    void frameBodyUsesCutoutDepthWriteLikeSwirl() throws IOException {
        String legacy = read("versions/1.21.1/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java");
        int legacyStart = legacy.indexOf("private static final RenderType ENDFRAME_FRAME_ROTATING");
        int legacyEnd = legacy.indexOf("private PortalRenderTypes", legacyStart);
        String legacyFrame = legacy.substring(legacyStart, legacyEnd);
        assertTrue(legacyFrame.contains(".setTransparencyState(TRANSLUCENT_TRANSPARENCY)"));
        assertTrue(legacyFrame.contains(".setCullState(NO_CULL)"));

        String modern = read("versions/26.1.2/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java");
        int modernStart = modern.indexOf("public static final RenderPipeline ENDFRAME_FRAME");
        int modernEnd = modern.indexOf("private Pipelines()", modernStart);
        String modernFrame = modern.substring(modernStart, modernEnd);
        assertTrue(modernFrame.contains(".withColorTargetState(ColorTargetState.DEFAULT)"));
        assertTrue(modernFrame.contains(".withCull(false)"));
        assertTrue(modernFrame.contains(
            "new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true)"));

        String legacyFragment = read(
            "src/main/resources/assets/minecraft/shaders/core/rendertype_rift_endframe.fsh");
        assertFalse(legacyFragment.contains("if (tex.a < 0.01) discard;"));
        String modernFragment = read("versions/26.1.2/src/main/resources/assets/riftgun/shaders/"
            + "core/rendertype_rift_endframe.fsh");
        assertTrue(modernFragment.contains("if (tex.a < 0.1) discard;"));
        assertTrue(modernFragment.contains("vec4(tex.rgb * tintColor, 1.0)"));
    }

    @Test
    void frameTextureDeclaresLegacyClampMetadata() throws IOException {
        String metadata = read(
            "src/main/resources/assets/riftgun/textures/entity/portal_frame.png.mcmeta");
        assertTrue(metadata.matches("(?s).*\\\"clamp\\\"\\s*:\\s*true.*"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
