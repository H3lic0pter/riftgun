package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PrecisionRadialLabelLayoutSourceTest {
    @Test
    void floatingOrientationSideLabelsUseMeasuredConstrainedLayout() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            String screen = Files.readString(Path.of("versions", version, "src", "main", "java",
                "dev", "riftgun", "client", "screen", "ModeRadialScreen.java"));
            int start = screen.indexOf("private void drawOptions(");
            int end = screen.indexOf("private void drawCenter(", start);
            String drawOptions = screen.substring(start, end);

            assertTrue(drawOptions.contains("RadialOptionLabelLayout.resolve("), version);
            assertTrue(drawOptions.contains("font.width(label)"), version);
            assertTrue(drawOptions.contains("layout.maximumWidth()"), version);
        }
    }
}
