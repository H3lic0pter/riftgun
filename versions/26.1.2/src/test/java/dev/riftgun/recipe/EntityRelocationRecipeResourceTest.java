package dev.riftgun.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EntityRelocationRecipeResourceTest {
    private static final String RECIPE_PATH =
        "data/riftgun/recipe/entity_relocation_module.json";

    @Test
    void remainsAValidThreeByThreeComponentRecipe() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(RECIPE_PATH);
        assertNotNull(stream, RECIPE_PATH);

        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var recipe = JsonParser.parseReader(reader).getAsJsonObject();
            var pattern = recipe.getAsJsonArray("pattern").asList().stream()
                .map(element -> element.getAsString())
                .toList();
            var bindingBook = recipe.getAsJsonObject("key").get("B").getAsJsonObject();
            var storedEnchantments = bindingBook.getAsJsonObject("components")
                .getAsJsonObject("minecraft:stored_enchantments");

            assertEquals(List.of("EBE", "DMD", "EOE"), pattern);
            assertEquals("neoforge:components",
                bindingBook.get("neoforge:ingredient_type").getAsString());
            assertEquals("minecraft:enchanted_book",
                bindingBook.get("items").getAsString());
            assertEquals(1, storedEnchantments.get("minecraft:binding_curse").getAsInt());
            assertEquals(false, bindingBook.get("strict").getAsBoolean());
        }
    }
}
