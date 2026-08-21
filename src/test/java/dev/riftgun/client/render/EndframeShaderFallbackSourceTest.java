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
    void bothNodesUseContextPathAndAnimatedFallbackLayers() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = read("versions/" + node
                + "/src/main/java/dev/riftgun/client/render/EndframePortalVisualRenderer.java");

            assertTrue(source.contains("context.surfaceRenderPath()"));
            assertFalse(source.contains("PortalShaderCompatibility.currentPath()"));
            assertTrue(source.contains("PortalRenderTypes.endframeStar(path)"));
            assertTrue(source.contains("PortalRenderTypes.endframeFrameGlow()"));
            assertEquals(1, count(source, "Math.cos(rotation)"));
            assertEquals(1, count(source, "Math.sin(rotation)"));
        }
    }

    @Test
    void shaderFallbackUsesNativeEndPortalAndClampSamplers() throws IOException {
        String legacy = read("versions/1.21.1/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java");
        assertTrue(legacy.contains("RenderType.endPortal()"));

        String modern = read("versions/26.1.2/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java");
        assertTrue(modern.contains("RenderTypes.endPortal()"));
        int start = modern.indexOf("private static final RenderType ENDFRAME_FALLBACK");
        int end = modern.indexOf("public static RenderType portal()", start);
        assertEquals(2, count(modern.substring(start, end),
            "getClampToEdge(FilterMode.LINEAR)"));
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
