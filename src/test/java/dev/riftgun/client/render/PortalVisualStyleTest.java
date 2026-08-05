package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PortalVisualStyleTest {
    @Test
    void defaultSplashIsPaleGreenRgb() {
        assertEquals(0xA8F0B6, PortalVisualStyle.PALE_GREEN.splashRgb());
    }

    @Test
    void splashColorCannotChangeOpacity() {
        assertThrows(IllegalArgumentException.class,
            () -> new PortalVisualStyle(0x80A8F0B6, 0xFF78D998, 0xFFD1FFDA));
    }
}
