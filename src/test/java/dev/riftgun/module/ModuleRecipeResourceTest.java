package dev.riftgun.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ModuleRecipeResourceTest {
    private static final List<Path> RESOURCE_ROOTS = List.of(
        Path.of("src/main/resources"),
        Path.of("versions/26.1.2/src/main/resources")
    );

    @Test
    void specializedModuleRecipesMatchTheirDesignedLayouts() throws Exception {
        assertRecipe("remote_module", List.of("RER", "PMP", "ICI"), Map.of(
            "R", "minecraft:diamond", "E", "minecraft:ender_eye",
            "P", "minecraft:repeater", "M", "riftgun:basic_module",
            "I", "minecraft:copper_ingot", "C", "minecraft:compass"));
        assertRecipe("portal_pairing_module", List.of("KEO", "TMC", "OEK"), Map.of(
            "K", "minecraft:poisonous_potato", "E", "minecraft:ender_pearl",
            "O", "minecraft:chain", "T", "minecraft:oxidized_copper",
            "M", "riftgun:basic_module", "C", "minecraft:copper_block"));
        assertRecipe("precision_placement_module", List.of("CSC", "OMO", "QLQ"), Map.of(
            "C", "minecraft:copper_ingot", "S", "minecraft:target",
            "O", "minecraft:observer", "M", "riftgun:basic_module",
            "Q", "minecraft:quartz", "L", "minecraft:lever"));
        assertRecipe("dimensional_traversal_module", List.of("ECE", "OAO", "ELE"), Map.of(
            "E", "minecraft:ender_eye", "C", "minecraft:end_crystal",
            "O", "minecraft:crying_obsidian", "A", "riftgun:advanced_basic_module",
            "L", "minecraft:lodestone"));
    }

    private static void assertRecipe(String name, List<String> pattern,
                                     Map<String, String> ingredients) throws Exception {
        for (Path root : RESOURCE_ROOTS) {
            Path path = root.resolve("data/riftgun/recipe/" + name + ".json");
            JsonObject recipe = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            List<String> actualPattern = new ArrayList<>();
            for (JsonElement row : recipe.getAsJsonArray("pattern")) {
                actualPattern.add(row.getAsString());
            }
            assertEquals(pattern, actualPattern, path.toString());
            JsonObject key = recipe.getAsJsonObject("key");
            assertEquals(ingredients.keySet(), key.keySet(), path.toString());
            for (var entry : ingredients.entrySet()) {
                JsonElement value = key.get(entry.getKey());
                String item = value.isJsonPrimitive()
                    ? value.getAsString() : value.getAsJsonObject().get("item").getAsString();
                String expected = root.startsWith(Path.of("versions/26.1.2"))
                    && name.equals("portal_pairing_module") && entry.getKey().equals("O")
                    ? "minecraft:iron_chain" : entry.getValue();
                assertEquals(expected, item, path + " key " + entry.getKey());
            }
            assertEquals("riftgun:" + name,
                recipe.getAsJsonObject("result").get("id").getAsString(), path.toString());
        }
    }
}
