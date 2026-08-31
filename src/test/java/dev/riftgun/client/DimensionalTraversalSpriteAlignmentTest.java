package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class DimensionalTraversalSpriteAlignmentTest {
    @Test
    void headerArtworkUsesNonEmptySixteenPixelCanvas() throws Exception {
        BufferedImage image = resource(
            "assets/riftgun/textures/gui/sprites/icons/dimensional_traversal.png");
        assertValidArtwork(image);
    }

    @Test
    void moduleArtworkUsesNonEmptySixteenPixelCanvas() throws Exception {
        BufferedImage image = resource(
            "assets/riftgun/textures/item/modules/dimensional_traversal_module.png");
        assertValidArtwork(image);
    }

    private static void assertValidArtwork(BufferedImage image) {
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        assertTrue(hasVisiblePixel(image), "artwork must not be fully transparent");
    }

    private static BufferedImage resource(String path) throws Exception {
        try (InputStream stream = DimensionalTraversalSpriteAlignmentTest.class
            .getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing " + path);
            return ImageIO.read(stream);
        }
    }

    private static boolean hasVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }
}
