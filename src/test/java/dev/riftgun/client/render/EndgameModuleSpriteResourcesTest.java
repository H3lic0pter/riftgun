package dev.riftgun.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class EndgameModuleSpriteResourcesTest {
    @Test
    void endgameModuleSpritesAreSixteenPixelTransparentIcons() throws Exception {
        for (String name : new String[] {
            "advanced_basic_module", "zero_point_fuel_module", "creative_module"
        }) {
            String path = "assets/riftgun/textures/item/modules/" + name + ".png";
            try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                BufferedImage image = ImageIO.read(stream);
                assertEquals(16, image.getWidth(), path);
                assertEquals(16, image.getHeight(), path);
                assertEquals(0, image.getRGB(0, 0) >>> 24, path);
                assertEquals(0, image.getRGB(15, 15) >>> 24, path);
            }
        }
    }
}
