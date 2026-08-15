package dev.riftgun.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.core.visual.PortalGunVisualSnapshot;
import dev.riftgun.fuel.PortalFuelProfiles;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class PortalGunCoreColorsTest {
    @Test
    void derivesGlossierOuterAndBrighterInnerColors() {
        int fuel = PortalFuelProfiles.DIMENSIONAL_RGB;
        int outer = PortalGunCoreColors.outer(fuel);
        int inner = PortalGunCoreColors.inner(fuel);

        assertEquals(PortalGunVisualSnapshot.OUTER_CORE_ALPHA, outer >>> 24);
        assertEquals(PortalGunVisualSnapshot.INNER_CORE_ALPHA, inner >>> 24);
        assertTrue(brightness(outer) > brightness(fuel));
        assertTrue(brightness(inner) > brightness(outer));
        assertTrue(saturation(inner) > saturation(outer));
    }

    @Test
    void coreMaskHasNoInvisibleDepthWritingTexels() throws IOException {
        BufferedImage texture = ImageIO.read(getClass().getResourceAsStream(
            "/assets/riftgun/textures/item/portal_gun/details.png"));
        for (int y = 24; y < 32; y++) {
            for (int x = 0; x < 8; x++) {
                assertEquals(0xFF, texture.getRGB(x, y) >>> 24);
            }
        }
    }

    private static int brightness(int color) {
        return Math.max(color >> 16 & 0xFF, Math.max(color >> 8 & 0xFF, color & 0xFF));
    }

    private static int saturation(int color) {
        int maximum = brightness(color);
        int minimum = Math.min(color >> 16 & 0xFF, Math.min(color >> 8 & 0xFF, color & 0xFF));
        return maximum - minimum;
    }
}
