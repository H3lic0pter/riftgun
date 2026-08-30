package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class PrecisionRadialSpriteResourceTest {
    private static final Path SPRITES = Path.of("src", "main", "resources", "assets", "riftgun",
        "textures", "gui", "sprites", "precision_radial");
    private static final Path REFERENCES = Path.of("reference", "precision_radial_center_icons");
    private static final String[] NAMES = {
        "front_in_front", "front_above_head", "front_below_feet",
        "remote_sideways", "remote_top_down", "remote_bottom_up"
    };

    @Test
    void editableCenterArtworkIsPackagedAndReferenceExportsRemainAvailable() throws Exception {
        for (String name : NAMES) {
            Path sprite = SPRITES.resolve(name + ".png");
            Path reference = REFERENCES.resolve(name + ".png");
            assertTrue(Files.isRegularFile(sprite), "missing packaged sprite: " + sprite);
            assertTrue(Files.isRegularFile(reference), "missing reference export: " + reference);

            BufferedImage image = ImageIO.read(sprite.toFile());
            assertNotNull(image, "unreadable sprite: " + sprite);
            assertEquals(64, image.getWidth(), "sprite width: " + sprite);
            assertEquals(64, image.getHeight(), "sprite height: " + sprite);
        }
    }

    @Test
    void bothClientsRenderThePackagedSpritesInsteadOfProceduralOutlines() throws Exception {
        for (String version : new String[] {"1.21.1", "26.1.2"}) {
            Path screenRoot = Path.of("versions", version, "src", "main", "java", "dev",
                "riftgun", "client", "screen");
            String radial = Files.readString(screenRoot.resolve("ModeRadialScreen.java"));
            String sprites = Files.readString(screenRoot.resolve("PrecisionRadialSprites.java"));

            assertTrue(radial.contains("PrecisionRadialSprites.draw"));
            assertTrue(!radial.contains("int[][] outline = selectedOrientation"));
            for (String name : NAMES) {
                assertTrue(sprites.contains(name), "missing sprite mapping: " + name);
            }
        }
    }
}
