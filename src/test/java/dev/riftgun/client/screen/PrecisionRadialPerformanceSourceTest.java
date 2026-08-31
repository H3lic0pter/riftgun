package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PrecisionRadialPerformanceSourceTest {
    @Test
    void precisionRingUsesHorizontalSpansInsteadOfThousandsOfGuiFills() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "ModeRadialScreen.java"));
            int start = source.indexOf("private void drawRing(");
            int end = source.indexOf("private void drawOptions(", start);
            String drawRing = source.substring(start, end);

            assertTrue(drawRing.contains("RadialRingSpans.forEach"));
            assertFalse(drawRing.contains("graphics.fill(centerX + x, centerY + y"));
        }
    }

    @Test
    void radialFramesReuseModeListsAndWireframeGeometry() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "ModeRadialScreen.java"));
            int optionsStart = source.indexOf("private List<?> options()");
            int optionsEnd = source.indexOf("private void drawRing(", optionsStart);
            String options = source.substring(optionsStart, optionsEnd);
            int wireframeStart = source.indexOf("private void drawFaceWireframe(");
            int wireframeEnd = source.indexOf("private void line(", wireframeStart);
            String wireframe = source.substring(wireframeStart, wireframeEnd);

            assertFalse(options.contains("new ArrayList"), version);
            assertFalse(options.contains("Arrays.asList"), version);
            assertFalse(wireframe.contains("new int[]"), version);
            assertFalse(wireframe.contains("int[][] points ="), version);
        }
    }
}
