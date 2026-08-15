package dev.riftgun.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class EndgameModuleRecipeResourceTest {
    @Test
    void advancedBasicUsesTheNonConsumingRecipeSerializer() throws Exception {
        assertEquals("riftgun:advanced_basic_module",
            recipe("advanced_basic_module").get("type").getAsString());
    }

    @Test
    void zeroPointFuelUsesItsConfigAwareRecipeSerializer() throws Exception {
        assertEquals("riftgun:zero_point_fuel_module",
            recipe("zero_point_fuel_module").get("type").getAsString());
    }

    @Test
    void eternalDurationNowRequiresTheAdvancedBase() throws Exception {
        var recipe = recipe("duration_eternal_module");
        assertEquals("riftgun:advanced_basic_module",
            recipe.getAsJsonObject("key").get("M").getAsString());
        assertTrue(recipe.getAsJsonArray("pattern").asList().stream()
            .anyMatch(element -> element.getAsString().contains("M")));
    }

    private static com.google.gson.JsonObject recipe(String name) throws Exception {
        String path = "data/riftgun/recipe/" + name + ".json";
        var stream = EndgameModuleRecipeResourceTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("missing " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
