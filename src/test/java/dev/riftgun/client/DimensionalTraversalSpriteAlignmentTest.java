package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class DimensionalTraversalSpriteAlignmentTest {
    @Test
    void headerArtworkUsesCenteredSixteenPixelCanvas() throws Exception {
        BufferedImage image = resource(
            "assets/riftgun/textures/gui/sprites/icons/dimensional_traversal.png");
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        Bounds bounds = bounds(image);
        assertEquals(bounds.left(), 15 - bounds.right());
        assertEquals(bounds.top(), 15 - bounds.bottom());
    }

    @Test
    void moduleArtworkUsesSymmetricTransparentPadding() throws Exception {
        BufferedImage image = resource(
            "assets/riftgun/textures/item/modules/dimensional_traversal_module.png");
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        Bounds bounds = bounds(image);
        assertEquals(bounds.left(), 15 - bounds.right());
        assertEquals(bounds.top(), 15 - bounds.bottom());
    }

    private static BufferedImage resource(String path) throws Exception {
        try (InputStream stream = DimensionalTraversalSpriteAlignmentTest.class
            .getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing " + path);
            return ImageIO.read(stream);
        }
    }

    private static Bounds bounds(BufferedImage image) {
        int left = image.getWidth();
        int right = -1;
        int top = image.getHeight();
        int bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                left = Math.min(left, x);
                right = Math.max(right, x);
                top = Math.min(top, y);
                bottom = Math.max(bottom, y);
            }
        }
        return new Bounds(left, right, top, bottom);
    }

    private record Bounds(int left, int right, int top, int bottom) {}
}
