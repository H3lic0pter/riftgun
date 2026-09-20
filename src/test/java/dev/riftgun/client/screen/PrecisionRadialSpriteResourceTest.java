package dev.riftgun.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalOrientation;
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
    void bothClientsPreserveTheSixPrecisionOrientationMappings() {
        assertSelection(PortalPlacementMode.FRONT, PortalOrientation.VERTICAL, "front_in_front");
        assertSelection(PortalPlacementMode.FRONT, PortalOrientation.TOP, "front_below_feet");
        assertSelection(PortalPlacementMode.FRONT, PortalOrientation.BOTTOM, "front_above_head");
        assertSelection(PortalPlacementMode.REMOTE, PortalOrientation.VERTICAL, "remote_sideways");
        assertSelection(PortalPlacementMode.REMOTE, PortalOrientation.TOP, "remote_top_down");
        assertSelection(PortalPlacementMode.REMOTE, PortalOrientation.BOTTOM, "remote_bottom_up");
    }

    private static void assertSelection(PortalPlacementMode mode, PortalOrientation orientation, String name) {
        GuiSprite sprite = PortalGuiSprites.precision(mode, orientation);
        assertEquals("riftgun:precision_radial/" + name, sprite.id().toString());
        assertEquals(64, sprite.size());
        assertSame(sprite, PortalGuiSprites.precision(mode, orientation));
    }
}
