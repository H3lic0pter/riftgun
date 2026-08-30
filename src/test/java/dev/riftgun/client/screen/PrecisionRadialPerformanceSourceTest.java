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
}
