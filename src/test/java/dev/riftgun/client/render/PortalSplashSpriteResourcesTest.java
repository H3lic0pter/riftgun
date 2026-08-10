package dev.riftgun.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalSplashSpriteResourcesTest {
    private static final String PARTICLE_DEFINITION = "assets/riftgun/particles/portal_splash.json";

    @Test
    void particleDefinitionProvidesEqualOriginalAndGrayscalePools() {
        JsonArray textures = loadParticleDefinition().getAsJsonArray("textures");
        Set<String> textureIds = new HashSet<>();
        int originalCount = 0;
        int grayscaleCount = 0;

        for (var element : textures) {
            String textureId = element.getAsString();
            textureIds.add(textureId);
            if (textureId.startsWith("riftgun:portal_splash/original_")) {
                originalCount++;
            } else if (textureId.startsWith("riftgun:portal_splash/grayscale_")) {
                grayscaleCount++;
            }
            assertTextureExists(textureId);
        }

        assertEquals(8, textures.size());
        assertEquals(8, textureIds.size(), "Each sprite entry must be unique");
        assertEquals(4, originalCount);
        assertEquals(4, grayscaleCount);
    }

    private static JsonObject loadParticleDefinition() {
        InputStream stream = resource(PARTICLE_DEFINITION);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception exception) {
            throw new AssertionError("Could not read portal splash particle definition", exception);
        }
    }

    private static void assertTextureExists(String textureId) {
        int separator = textureId.indexOf(':');
        String path = "assets/" + textureId.substring(0, separator) + "/textures/particle/"
                + textureId.substring(separator + 1) + ".png";
        try (InputStream stream = resource(path)) {
            assertTrue(stream.read() >= 0, () -> "Texture is empty: " + path);
        } catch (Exception exception) {
            throw new AssertionError("Could not read texture: " + path, exception);
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = PortalSplashSpriteResourcesTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, () -> "Missing resource: " + path);
        return stream;
    }
}
