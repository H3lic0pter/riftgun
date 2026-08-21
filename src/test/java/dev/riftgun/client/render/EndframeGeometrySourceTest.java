package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EndframeGeometrySourceTest {
    @Test
    void bothNodesSubmitLegalPrecomputedStarQuads() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", node, "src/main/java/dev/riftgun/client/render/EndframePortalVisualRenderer.java"));
            int start = source.indexOf("private static void starFan");
            int end = source.indexOf("private static void drawSlab", start);
            String star = source.substring(start, end);

            assertTrue(star.contains("EndframeVisualGeometry.STAR_SEGMENTS"));
            assertEquals(4, count(star, "starVertex(vertices"));
            assertFalse(star.contains("Math.sin"));
            assertFalse(star.contains("Math.cos"));
            assertFalse(star.contains("basis.at"));
        }
    }

    private static int count(String text, String token) {
        return (text.length() - text.replace(token, "").length()) / token.length();
    }
}
