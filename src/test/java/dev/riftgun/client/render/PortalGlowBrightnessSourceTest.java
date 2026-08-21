package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PortalGlowBrightnessSourceTest {
    @Test
    void swirlGlowUsesPathSpecificRestrainedMultipliers() throws IOException {
        String legacy = Files.readString(Path.of("versions/1.21.1/src/main/java/dev/riftgun/"
            + "client/render/SwirlPortalVisualRenderer.java"));
        assertTrue(legacy.contains("GLOW_BRIGHTNESS_MULTIPLIER = 0.45F"));

        String modern = Files.readString(Path.of("versions/26.1.2/src/main/java/dev/riftgun/"
            + "client/render/SwirlPortalVisualRenderer.java"));
        assertTrue(modern.contains("CUSTOM_GLOW_BRIGHTNESS_MULTIPLIER = 0.35F"));
        assertTrue(modern.contains("FALLBACK_GLOW_BRIGHTNESS_MULTIPLIER = 0.45F"));
        assertFalse(modern.contains("FALLBACK_BRIGHTNESS_BOOST"));
    }
}
