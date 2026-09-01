package dev.riftgun.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CreateMixingRecipeResourceTest {
    @Test
    void recipesUseCreateMixingAndBothOptionalGates() throws Exception {
        for (String name : List.of("unstable_portal_fluid", "portal_fluid", "dimensional_portal_fluid")) {
            JsonObject recipe = recipe(name);
            assertEquals("create:mixing", recipe.get("type").getAsString());
            assertFalse(recipe.has("processing_time"));

            JsonArray conditions = recipe.getAsJsonArray("neoforge:conditions");
            assertEquals("neoforge:mod_loaded",
                conditions.get(0).getAsJsonObject().get("type").getAsString());
            assertEquals("create", conditions.get(0).getAsJsonObject().get("modid").getAsString());
            assertEquals("riftgun:create_mixing_recipes_enabled",
                conditions.get(1).getAsJsonObject().get("type").getAsString());

            JsonObject water = recipe.getAsJsonArray("ingredients").get(0).getAsJsonObject();
            assertEquals("neoforge:single", water.get("type").getAsString());
            assertEquals("minecraft:water", water.get("fluid").getAsString());
            assertEquals(1000, water.get("amount").getAsInt());
            assertEquals(1000,
                recipe.getAsJsonArray("results").get(0).getAsJsonObject().get("amount").getAsInt());
        }
    }

    @Test
    void ingredientSetsAndHeatMatchThePortalFluidRecipes() throws Exception {
        JsonObject unstable = recipe("unstable_portal_fluid");
        assertEquals(List.of("c:gems/lapis", "c:ingots/copper"), tags(unstable));
        assertEquals(List.of("minecraft:rotten_flesh"), items(unstable));
        assertFalse(unstable.has("heat_requirement"));
        JsonObject returnedCopper = unstable.getAsJsonArray("results").get(1).getAsJsonObject();
        assertEquals("minecraft:copper_ingot", returnedCopper.get("id").getAsString());
        assertFalse(returnedCopper.has("chance"));

        JsonObject stable = recipe("portal_fluid");
        assertEquals(List.of("c:crops/nether_wart", "c:gems/quartz", "c:gems/diamond"), tags(stable));
        assertEquals(List.of(), items(stable));
        assertFalse(stable.has("heat_requirement"));
        JsonObject returnedDiamond = stable.getAsJsonArray("results").get(1).getAsJsonObject();
        assertEquals("minecraft:diamond", returnedDiamond.get("id").getAsString());
        assertEquals(0.5F, returnedDiamond.get("chance").getAsFloat());

        JsonObject dimensional = recipe("dimensional_portal_fluid");
        assertEquals(List.of(), tags(dimensional));
        assertEquals(List.of("minecraft:chorus_fruit", "minecraft:ender_pearl"), items(dimensional));
        assertEquals("heated", dimensional.get("heat_requirement").getAsString());
        assertEquals(1, dimensional.getAsJsonArray("results").size());
    }

    private static List<String> tags(JsonObject recipe) {
        return recipe.getAsJsonArray("ingredients").asList().stream()
            .map(element -> element.getAsJsonObject())
            .filter(ingredient -> ingredient.has("tag"))
            .map(ingredient -> ingredient.get("tag").getAsString())
            .toList();
    }

    private static List<String> items(JsonObject recipe) {
        return recipe.getAsJsonArray("ingredients").asList().stream()
            .map(element -> element.getAsJsonObject())
            .filter(ingredient -> ingredient.has("item"))
            .map(ingredient -> ingredient.get("item").getAsString())
            .toList();
    }

    private static JsonObject recipe(String name) throws Exception {
        String path = "data/riftgun/recipe/create_mixing/" + name + ".json";
        var stream = CreateMixingRecipeResourceTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("missing " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
