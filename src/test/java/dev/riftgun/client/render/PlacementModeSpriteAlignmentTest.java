package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class PlacementModeSpriteAlignmentTest {
    @Test
    void remoteArtworkKeepsReviewedOpticalBounds() throws Exception {
        BufferedImage image = ImageIO.read(Path.of(
            "src/main/resources/assets/riftgun/textures/gui/sprites/icons/placement_remote.png")
            .toFile());
        Bounds bounds = alphaBounds(image);

        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        // The redrawn sprite is intentionally optically asymmetric; callers that need
        // correction own it locally instead of shifting every use of the shared artwork.
        assertEquals(new Bounds(0, 3, 13, 12), bounds,
            "REMOTE icon alpha bounds changed; re-check every GUI placement before accepting it");
    }

    private static Bounds alphaBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {}
}
