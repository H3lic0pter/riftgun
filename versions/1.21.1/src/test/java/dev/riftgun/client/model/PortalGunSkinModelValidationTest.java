package dev.riftgun.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.appearance.client.PortalGunSkinCatalog;
import dev.riftgun.appearance.client.PortalGunSkinDefinition;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class PortalGunSkinModelValidationTest {
    @Test
    void bundledArcaneRiftStaffModelParsesAndUsesOnlyLayeredTintSlots() throws Exception {
        String path = "/assets/riftgun/models/item/portal_gun/arcane_rift_staff.json";
        try (var stream = PortalGunSkinModelValidationTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            JsonObject model = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            BlockModel.fromStream(new StringReader(model.toString()));

            Set<Integer> tints = new HashSet<>();
            model.getAsJsonArray("elements").forEach(element ->
                element.getAsJsonObject().getAsJsonObject("faces").entrySet().forEach(face -> {
                    JsonObject definition = face.getValue().getAsJsonObject();
                    if (definition.has("tintindex")) tints.add(definition.get("tintindex").getAsInt());
                }));
            assertEquals(Set.of(2, 3, 4, 5, 6, 7, 8, 9, 10, 11), tints);
            assertEquals(Set.of(11), groupTints(model, "Permanent rift"));
            assertEquals(Set.of(11), groupTints(model, "Sigil"));
            assertEquals(Set.of(9, 10), groupTints(model, "Suspended crystals"));
            assertTrue(groupTints(model, "Heartwood").isEmpty());
            assertTrue(groupTints(model, "Binding").isEmpty());
            assertEquals(Set.of(11), groupTints(model, "Engraved ferrules"));
            assertTrue(groupTints(model, "Crown root").isEmpty());
            assertTrue(groupTints(model, "Swept crown").isEmpty());
            assertEquals(Set.of(11), groupTints(model, "Crown inlay"));
            assertEquals(Set.of(11), groupTints(model, "Crystal heel"));
            assertTrue(groupTints(model, "Runic gauge").isEmpty());
            assertEquals(Set.of(2, 3, 4, 5, 6, 7, 8), groupTints(model, "Fuel tiers 2-8"));
            assertNotNull(getClass().getResourceAsStream(
                "/assets/riftgun/textures/item/arcane_rift_staff/tintable_body.png"));
            assertEquals(Set.of(
                "neck_amethyst", "left_crown_inlay", "right_crown_inlay",
                "heel_crystal_0", "heel_crystal_1", "heel_crystal_2",
                "heel_crystal_3", "heel_crystal_4", "heel_crystal_5",
                "permanent_void_orb_0", "permanent_void_orb_1", "permanent_void_orb_2",
                "permanent_void_orb_3", "permanent_void_orb_4", "permanent_void_orb_5",
                "permanent_void_orb_6", "rift_sigil"), tintedElements(model, 11));
        }
    }

    @Test
    void bundledApertureIshModelParsesAndMarksOnlyGladosAsZeroPointGeometry() throws Exception {
        String path = "/assets/riftgun/models/item/portal_gun/aperture_ish.json";
        try (var stream = PortalGunSkinModelValidationTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            JsonObject model = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            BlockModel.fromStream(new StringReader(model.toString()));

            JsonObject energyStrip = model.getAsJsonArray("elements").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(element -> element.has("name")
                    && element.get("name").getAsString().equals("energy_strip"))
                .findFirst().orElseThrow();
            energyStrip.getAsJsonObject("faces").entrySet().forEach(face ->
                assertFalse(face.getValue().getAsJsonObject().has("tintindex")));

            JsonObject glados = model.getAsJsonArray("groups").asList().stream()
                .map(group -> group.getAsJsonObject())
                .filter(group -> group.get("name").getAsString().equals("glados"))
                .findFirst().orElseThrow();
            Set<Integer> gladosElements = new HashSet<>();
            collectElements(glados.getAsJsonArray("children"), gladosElements);
            assertFalse(gladosElements.isEmpty());
            gladosElements.forEach(index -> model.getAsJsonArray("elements").get(index)
                .getAsJsonObject().getAsJsonObject("faces").entrySet().forEach(face ->
                    assertEquals(12,
                        face.getValue().getAsJsonObject().get("tintindex").getAsInt())));
        }
    }

    @Test
    void bundledApertureIshStatusLampUsesModeTintSlot() throws Exception {
        String path = "/assets/riftgun/models/item/portal_gun/aperture_ish.json";
        try (var stream = PortalGunSkinModelValidationTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            JsonObject model = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject lamp = model.getAsJsonArray("elements").asList().stream()
                .map(element -> element.getAsJsonObject())
                .filter(element -> element.has("name")
                    && element.get("name").getAsString().equals("status_lamp"))
                .findFirst().orElseThrow();
            lamp.getAsJsonObject("faces").entrySet().forEach(face ->
                assertEquals(40, face.getValue().getAsJsonObject().get("tintindex").getAsInt()));
        }
    }

    @Test
    void excludesSyntacticallyValidJsonWithInvalidModelElements() {
        ResourceManager resources = resources("addon:item/broken", Map.of(
            "addon:models/item/broken.json", "{\"elements\":\"bad\"}"));

        Map<String, PortalGunSkinDefinition> loaded = load(resources);

        assertEquals(Set.of(PortalGunSkin.DEFAULT), loaded.keySet());
        assertFalse(loaded.containsKey("addon:skin"));
    }

    @Test
    void validatesAncestorsEvenWhenTheSkinModelItselfIsValid() {
        ResourceManager resources = resources("addon:item/child", Map.of(
            "addon:models/item/child.json", "{\"parent\":\"addon:item/middle\"}",
            "addon:models/item/middle.json", "{\"parent\":\"addon:item/broken\"}",
            "addon:models/item/broken.json", "{\"elements\":\"bad\"}"));

        Map<String, PortalGunSkinDefinition> loaded = load(resources);

        assertEquals(Set.of(PortalGunSkin.DEFAULT), loaded.keySet());
    }

    @Test
    void acceptsAStandardGeneratedItemModelAndItsBuiltinParent() {
        ResourceManager resources = resources("addon:item/icon", Map.of(
            "addon:models/item/icon.json", """
                {"parent":"minecraft:item/generated","textures":{"layer0":"addon:item/icon"}}
                """,
            "minecraft:models/item/generated.json", "{\"parent\":\"builtin/generated\"}"));

        Map<String, PortalGunSkinDefinition> loaded = load(resources);

        assertEquals(Set.of(PortalGunSkin.DEFAULT, "addon:skin"), loaded.keySet());
        PortalGunSkinDefinition skin = loaded.get("addon:skin");
        assertEquals("addon:item/icon", skin.model());
        assertTrue(skin.flat());
        assertFalse(skin.layered());
    }

    private static Map<String, PortalGunSkinDefinition> load(ResourceManager resources) {
        return PortalGunSkinCatalog.load(resources,
            json -> BlockModel.fromStream(new StringReader(json.toString())));
    }

    private static void collectElements(com.google.gson.JsonArray children, Set<Integer> elements) {
        children.forEach(child -> {
            if (child.isJsonPrimitive()) {
                elements.add(child.getAsInt());
            } else {
                collectElements(child.getAsJsonObject().getAsJsonArray("children"), elements);
            }
        });
    }

    private static Set<Integer> groupTints(JsonObject model, String name) {
        JsonObject group = model.getAsJsonArray("groups").asList().stream()
            .map(value -> value.getAsJsonObject())
            .filter(value -> value.get("name").getAsString().equals(name))
            .findFirst().orElseThrow();
        Set<Integer> elements = new HashSet<>();
        collectElements(group.getAsJsonArray("children"), elements);
        Set<Integer> tints = new HashSet<>();
        elements.forEach(index -> model.getAsJsonArray("elements").get(index)
            .getAsJsonObject().getAsJsonObject("faces").entrySet().forEach(face -> {
                JsonObject definition = face.getValue().getAsJsonObject();
                if (definition.has("tintindex")) tints.add(definition.get("tintindex").getAsInt());
            }));
        return tints;
    }

    private static Set<String> tintedElements(JsonObject model, int tint) {
        Set<String> names = new HashSet<>();
        model.getAsJsonArray("elements").forEach(value -> {
            JsonObject element = value.getAsJsonObject();
            boolean present = element.getAsJsonObject("faces").entrySet().stream()
                .map(face -> face.getValue().getAsJsonObject())
                .anyMatch(face -> face.has("tintindex")
                    && face.get("tintindex").getAsInt() == tint);
            if (present) names.add(element.get("name").getAsString());
        });
        return names;
    }

    private static ResourceManager resources(String model, Map<String, String> models) {
        ResourceManager resources = mock(ResourceManager.class);
        when(resources.listResources(eq("portal_gun_skins"), any())).thenAnswer(ignored -> Map.of(
            ResourceLocation.parse("addon:portal_gun_skins/skin.json"), resource(
                "{\"name\":\"skin.addon.skin\",\"model\":\"" + model
                    + "\",\"renderer\":\"standard\",\"display\":\"flat\"}")));
        when(resources.getResource(any())).thenAnswer(call -> {
            String json = models.get(call.getArgument(0).toString());
            return json == null ? java.util.Optional.empty() : java.util.Optional.of(resource(json));
        });
        return resources;
    }

    private static Resource resource(String json) throws Exception {
        Resource resource = mock(Resource.class);
        when(resource.openAsReader()).thenAnswer(ignored -> new BufferedReader(new StringReader(json)));
        return resource;
    }
}
