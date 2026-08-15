package dev.riftgun.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

final class SpecialEntityTagResourcesTest {
    @Test
    void builtInAllowTagsCoverFallingBlocksTntAndExperienceOrbs() {
        Set<String> expected = Set.of(
            "minecraft:falling_block", "minecraft:tnt", "minecraft:experience_orb");

        assertEquals(expected, values("portal_transit_allowed"));
        assertEquals(expected, values("entity_relocation_allowed"));
    }

    @Test
    void builtInSweptTagExcludesExperienceOrbs() {
        assertEquals(Set.of("minecraft:falling_block", "minecraft:tnt"),
            values("portal_transit_swept"));
    }

    @Test
    void denyTagsArePublicEmptyExtensionPoints() {
        assertEquals(Set.of(), values("portal_transit_denied"));
        assertEquals(Set.of(), values("entity_relocation_denied"));
    }

    private static Set<String> values(String name) {
        String path = "/data/riftgun/tags/entity_type/" + name + ".json";
        try (var stream = SpecialEntityTagResourcesTest.class.getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("Missing resource " + path);
            JsonObject json = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return StreamSupport.stream(json.getAsJsonArray("values").spliterator(), false)
                .map(element -> element.getAsString())
                .collect(Collectors.toUnmodifiableSet());
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
