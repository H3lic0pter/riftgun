package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SwirlFallbackResourceTest {
    private static final Path TEXTURE =
        Path.of("src/main/resources/assets/riftgun/textures/entity/portal_surface.png");

    @Test
    void portalTextureHasATransparentClampBorder() throws IOException {
        BufferedImage image = ImageIO.read(TEXTURE.toFile());
        assertNotNull(image);

        for (int x = 0; x < image.getWidth(); x++) {
            assertEquals(0, alpha(image, x, 0));
            assertEquals(0, alpha(image, x, image.getHeight() - 1));
        }
        for (int y = 0; y < image.getHeight(); y++) {
            assertEquals(0, alpha(image, 0, y));
            assertEquals(0, alpha(image, image.getWidth() - 1, y));
        }
    }

    @Test
    void fallbackSubmissionUsesQuadsWithoutPerVertexObjectsOrTrig() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of(
                "versions", node, "src/main/java/dev/riftgun/client/render/SwirlPortalVisualRenderer.java"));
            assertTrue(source.contains("drawFallbackQuad"));
            assertFalse(source.contains("FALLBACK_SURFACE_SEGMENTS"));
            assertFalse(source.contains("drawFallbackDisc"));
            assertFalse(source.contains("SwirlFallbackGeometry.RimPoint"));
            assertFalse(source.contains("SwirlFallbackAnimation.Uv"));

            String vertexMethod = source.substring(source.indexOf("private static void fallbackVertex"),
                source.indexOf("private static void drawEdge"));
            assertFalse(vertexMethod.contains("Math.sin"));
            assertFalse(vertexMethod.contains("Math.cos"));
            assertFalse(vertexMethod.contains("new "));
        }
    }

    @Test
    void modernFallbackUsesExplicitClampSamplers() throws IOException {
        String source = Files.readString(Path.of(
            "versions/26.1.2/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java"));
        int fallback = source.indexOf("private static final RenderType SWIRL_FALLBACK =");
        int endframe = source.indexOf("private static final RenderType ENDFRAME_FALLBACK =");
        String fallbackTypes = source.substring(fallback, endframe);

        assertEquals(1, count(fallbackTypes, "getClampToEdge(FilterMode.LINEAR)"));

        int glow = source.indexOf("private static final RenderType SWIRL_FALLBACK_GLOW =");
        String glowType = source.substring(glow, fallback);
        assertEquals(1, count(glowType, "getClampToEdge(FilterMode.LINEAR)"));
    }

    @Test
    void modernCustomVisualsUseCrispSamplersAndAFixedSwirlAperture() throws IOException {
        String renderTypes = Files.readString(Path.of(
            "versions/26.1.2/src/main/java/dev/riftgun/client/render/PortalRenderTypes.java"));
        int swirl = renderTypes.indexOf("private static final RenderType SWIRL =");
        int fallbackGlow = renderTypes.indexOf(
            "private static final RenderType SWIRL_FALLBACK_GLOW =", swirl);
        String customSwirl = renderTypes.substring(swirl, fallbackGlow);
        assertEquals(2, count(customSwirl, "getClampToEdge(FilterMode.NEAREST)"));

        int swirlPipeline = renderTypes.indexOf("public static final RenderPipeline SWIRL =");
        int glowPipeline = renderTypes.indexOf(
            "public static final RenderPipeline SWIRL_GLOW =", swirlPipeline);
        String swirlPipelineSource = renderTypes.substring(swirlPipeline, glowPipeline);
        assertTrue(swirlPipelineSource.contains("ColorTargetState(BlendFunction.TRANSLUCENT)"));
        assertTrue(swirlPipelineSource.contains("CompareOp.LESS_THAN_OR_EQUAL, true"));

        int frame = renderTypes.indexOf("private static final RenderType ENDFRAME_FRAME =");
        int accessors = renderTypes.indexOf("public static RenderType portal()", frame);
        assertEquals(1, count(renderTypes.substring(frame, accessors),
            "getClampToEdge(FilterMode.NEAREST)"));

        int framePipeline = renderTypes.indexOf("public static final RenderPipeline ENDFRAME_FRAME =");
        int pipelineConstructor = renderTypes.indexOf("private Pipelines()", framePipeline);
        assertTrue(renderTypes.substring(framePipeline, pipelineConstructor)
            .contains("withColorTargetState(ColorTargetState.DEFAULT)"));

        String fragment = Files.readString(Path.of("versions/26.1.2/src/main/resources/"
            + "assets/riftgun/shaders/core/rendertype_rift_portal_swirl.fsh"));
        assertTrue(fragment.contains("smoothstep(0.484375, 0.5, radius)"));
        assertTrue(fragment.contains("vec4(tex.rgb * tintColor, apertureAlpha)"));
    }

    @Test
    void legacyFallbackTextureExplicitlyClampsToEdge() throws IOException {
        Path metadata = Path.of(TEXTURE + ".mcmeta");
        assertTrue(Files.isRegularFile(metadata));
        assertTrue(Pattern.compile("\\\"clamp\\\"\\s*:\\s*true")
            .matcher(Files.readString(metadata)).find());
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
