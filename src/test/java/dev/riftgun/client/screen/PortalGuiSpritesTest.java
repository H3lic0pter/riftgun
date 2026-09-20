package dev.riftgun.client.screen;

import dev.riftgun.config.SkinRecommendationConfig.Category;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PortalGuiSpritesTest {
    @Test
    void catalogCoversEveryGuiPngWithItsAuthoredCanvasSize() throws Exception {
        Path root = Path.of("src/main/resources/assets/riftgun/textures/gui/sprites");
        Set<Path> referenced = new HashSet<>();
        for (var field : PortalGuiSprites.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != GuiSprite.class) continue;
            field.setAccessible(true);
            GuiSprite sprite = (GuiSprite) field.get(null);
            assertEquals("riftgun", sprite.id().getNamespace());
            Path png = root.resolve(sprite.id().getPath() + ".png");
            assertTrue(referenced.add(png), "duplicate sprite: " + png);
            assertTrue(Files.isRegularFile(png), "missing sprite: " + png);
            var image = ImageIO.read(png.toFile());
            assertNotNull(image, "invalid PNG: " + png);
            assertEquals(sprite.size(), image.getWidth(), png.toString());
            assertEquals(sprite.size(), image.getHeight(), png.toString());
        }
        try (var files = Files.walk(root)) {
            assertEquals(new HashSet<>(files.filter(p -> p.toString().endsWith(".png")).toList()), referenced);
        }
    }

    @Test
    void recommendationStatesReuseTheirNativeResourceIds() {
        for (Category category : Category.values()) {
            for (boolean enabled : new boolean[] {false, true}) {
                GuiSprite sprite = PortalGuiSprites.recommendation(category, enabled);
                assertEquals("riftgun:icons/recommend_" + category.name().toLowerCase(Locale.ROOT)
                    + (enabled ? "_on" : "_off"), sprite.id().toString());
                assertSame(sprite, PortalGuiSprites.recommendation(category, enabled));
                assertSame(sprite.id(), PortalGuiSprites.recommendation(category, enabled).id());
            }
        }
    }
}
