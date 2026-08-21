package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalGlowBrightnessSourceTest {
    @Test
    void swirlGlowUsesTheSharedRestrainedMultiplierOnBothNodes() throws IOException {
        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of("versions", node,
                "src/main/java/dev/riftgun/client/render/SwirlPortalVisualRenderer.java"));

            assertTrue(source.contains("GLOW_BRIGHTNESS_MULTIPLIER = 0.45F"));
            assertFalse(source.contains("GLOW_BRIGHTNESS_MULTIPLIER = 0.80F"));
            assertFalse(source.contains("FALLBACK_BRIGHTNESS_BOOST"));
        }
    }
}
