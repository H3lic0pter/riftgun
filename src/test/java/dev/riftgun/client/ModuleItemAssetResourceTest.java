package dev.riftgun.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModuleItemAssetResourceTest {
    private static final Path SHARED = Path.of("src/main/resources/assets/riftgun");
    private static final Path MODERN = Path.of(
        "versions/26.1.2/src/main/resources/assets/riftgun");

    @Test
    void modernSpecializedModulesHaveCompleteItemModelGraphs() throws Exception {
        for (String module : List.of("remote_module", "portal_pairing_module",
            "precision_placement_module", "dimensional_traversal_module")) {
            Path definitionPath = MODERN.resolve("items/" + module + ".json");
            assertTrue(Files.isRegularFile(definitionPath),
                module + " is missing its 26.1.2 Client Items definition");
            JsonObject definition = json(definitionPath).getAsJsonObject("model");
            assertEquals("minecraft:model", definition.get("type").getAsString(), module);
            assertEquals("riftgun:item/" + module, definition.get("model").getAsString(), module);

            Path modelPath = SHARED.resolve("models/item/" + module + ".json");
            assertTrue(Files.isRegularFile(modelPath), module + " is missing its shared item model");
            String texture = json(modelPath).getAsJsonObject("textures").get("layer0").getAsString();
            assertEquals("riftgun:item/modules/" + module, texture, module);
            assertTrue(Files.isRegularFile(SHARED.resolve(
                "textures/item/modules/" + module + ".png")), module + " is missing its PNG");
        }
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
