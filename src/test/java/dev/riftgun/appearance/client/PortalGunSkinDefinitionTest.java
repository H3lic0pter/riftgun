package dev.riftgun.appearance.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalGunSkinDefinitionTest {
    @Test
    void modePaletteRejectsIndicesAboveTheHardLimit() {
        for (String index : new String[] {"101", "1000000", "2147483647", "2147483648"}) {
            JsonObject json = description("layered", "3d");
            json.add("dynamic", JsonParser.parseString("{\"mode_colors\": {\"" + index
                + "\": {\"coordinate\": \"#123456\", \"pairing\": \"#ABCDEF\"}}}"));
            assertThrows(IllegalArgumentException.class,
                () -> PortalGunSkinDefinition.parse("example:icon", json), index);
        }
    }

    @Test
    void resourcePaletteControlsIndependentSlotsWithoutDependingOnFuelOrCore() {
        JsonObject json = description("layered", "3d");
        json.add("dynamic", JsonParser.parseString("""
            {"fluid": true, "zero_point": true, "mode_colors": {
              "40": {"coordinate": "#123456", "pairing": "#098765"},
              "41": {"coordinate": "#abcdef", "pairing": "#FEDCBA"},
              "100": {"coordinate": "#654321", "pairing": "#112233"}
            }}
            """));
        var skin = PortalGunSkinDefinition.parse("example:icon", json);
        for (int liquid : new int[] {0, 2, 3, 4, 5, 6, 7, 8}) {
            for (boolean core : new boolean[] {false, true}) {
                for (int fuel : new int[] {0, 0xFFFFFF, 0x4FCB72}) {
                    assertEquals(0xFF123456, skin.color(liquid, core, fuel, false, 40));
                    assertEquals(0xFFABCDEF, skin.color(liquid, core, fuel, false, 41));
                    assertEquals(0xFF098765, skin.color(liquid, core, fuel, true, 40));
                    assertEquals(0xFFFEDCBA, skin.color(liquid, core, fuel, true, 41));
                    assertEquals(0xFF654321, skin.color(liquid, core, fuel, false, 100));
                    assertEquals(0xFF112233, skin.color(liquid, core, fuel, true, 100));
                    assertEquals(-1, skin.color(liquid, core, fuel, true, 42));
                    for (int tint = -1; tint <= 12; tint++) {
                        assertEquals(PortalGunVisualSnapshot.color(liquid, core, fuel, tint),
                            skin.color(liquid, core, fuel, true, tint));
                    }
                }
            }
        }
    }

    @Test
    void oneRuleCanUseAnyUnreservedSlotAndEmptyRulesKeepOriginalTexture() {
        JsonObject json = description("layered", "3d");
        json.add("dynamic", JsonParser.parseString("""
            {"mode_colors": {"45": {"coordinate": "#123456", "pairing": "#ABCDEF"}}}
            """));
        var skin = PortalGunSkinDefinition.parse("example:icon", json);
        assertEquals(1, skin.modeColors().size());
        assertEquals(0xFF123456, skin.color(0, false, 0, false, 45));
        assertEquals(0xFFABCDEF, skin.color(0, false, 0, true, 45));
        assertEquals(-1, skin.color(0, false, 0, true, 40));
        json.getAsJsonObject("dynamic").add("mode_colors", new JsonObject());
        var empty = PortalGunSkinDefinition.parse("example:icon", json);
        assertEquals(-1, empty.color(0, false, 0, true, 45));
        assertThrows(UnsupportedOperationException.class, () -> skin.modeColors().clear());
    }

    @Test
    void bundledPaletteRetainsTheAgreedColors() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/riftgun/portal_gun_skins/default.json")) {
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var skin = PortalGunSkinDefinition.parse(PortalGunSkin.DEFAULT, json);
            assertEquals(0xFF680A1C, skin.color(0, false, 0, false, 40));
            assertEquals(0xFFF85B5B, skin.color(0, false, 0, false, 41));
            assertEquals(0xFF806515, skin.color(0, false, 0, true, 40));
            assertEquals(0xFFFFD45A, skin.color(0, false, 0, true, 41));
        }
    }

    @Test
    void omittedPaletteLeavesTexturesUntintedInBothModes() {
        var skin = PortalGunSkinDefinition.parse("example:icon", description("layered", "3d"));
        for (boolean pairing : new boolean[] {false, true}) {
            assertEquals(-1, skin.color(4, true, 0x123456, pairing, 40));
            assertEquals(-1, skin.color(4, true, 0x123456, pairing, 41));
        }
    }

    @Test
    void incompleteInvalidAndUnsupportedPalettesAreRejected() {
        for (String palette : new String[] {
            "{\"40\": {\"coordinate\": \"#123456\"}}",
            "{\"40\": {\"coordinate\": \"#ZZZZZZ\", \"pairing\": \"#FFFFFF\"}}",
            "{\"40\": {\"coordinate\": 123456, \"pairing\": \"#FFFFFF\"}}",
            "{\"39\": {\"coordinate\": \"#123456\", \"pairing\": \"#FFFFFF\"}}",
            "{\"-1\": {\"coordinate\": \"#123456\", \"pairing\": \"#FFFFFF\"}}",
            "{\"040\": {\"coordinate\": \"#123456\", \"pairing\": \"#FFFFFF\"}}"
        }) {
            JsonObject json = description("layered", "3d");
            JsonObject dynamic = new JsonObject();
            dynamic.add("mode_colors", JsonParser.parseString(palette));
            json.add("dynamic", dynamic);
            assertThrows(IllegalArgumentException.class, () -> PortalGunSkinDefinition.parse("example:icon", json));
        }
        JsonObject json = description("standard", "flat");
        json.add("dynamic", JsonParser.parseString("""
            {"mode_colors": {
              "40": {"coordinate": "#123456", "pairing": "#abcdef"}
            }}
            """));
        assertThrows(IllegalArgumentException.class, () -> PortalGunSkinDefinition.parse("example:icon", json));
    }

    @Test
    void defaultRetainsTheExistingModelAndBothDynamicDisplays() {
        PortalGunSkinDefinition skin = PortalGunSkinDefinition.DEFAULT;
        assertEquals("riftgun:default", skin.id());
        assertEquals("riftgun:item/portal_gun", skin.model());
        assertTrue(skin.layered());
        assertFalse(skin.flat());
        assertTrue(skin.fluid());
        assertTrue(skin.zeroPoint());
    }

    @Test
    void staticFlatModelDoesNotRequireDynamicMetadata() {
        PortalGunSkinDefinition skin = PortalGunSkinDefinition.parse(
            "example:icon", description("standard", "flat"));
        assertEquals("example:icon", skin.id());
        assertEquals("skin.example.icon", skin.name());
        assertFalse(skin.layered());
        assertTrue(skin.flat());
        assertFalse(skin.fluid());
        assertFalse(skin.zeroPoint());
    }

    @Test
    void layeredRendererAllowsFlatAndThreeDimensionalPresentations() {
        for (String display : new String[] {"flat", "3d"}) {
            PortalGunSkinDefinition skin = PortalGunSkinDefinition.parse(
                "example:icon", description("layered", display));
            assertTrue(skin.layered());
            assertEquals(display.equals("flat"), skin.flat());
        }
    }

    @Test
    void descriptionRejectsUnknownRendererDisplayAndInvalidIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> PortalGunSkinDefinition.parse(
            "example:icon", description("custom", "3d")));
        assertThrows(IllegalArgumentException.class, () -> PortalGunSkinDefinition.parse(
            "example:icon", description("standard", "2d")));
        assertThrows(IllegalArgumentException.class, () -> PortalGunSkinDefinition.parse(
            "not_namespaced", description("standard", "flat")));
        JsonObject invalidModel = description("standard", "flat");
        invalidModel.addProperty("model", "example:Invalid");
        assertThrows(IllegalArgumentException.class,
            () -> PortalGunSkinDefinition.parse("example:icon", invalidModel));
        JsonObject blankName = description("standard", "flat");
        blankName.addProperty("name", " ");
        assertThrows(IllegalArgumentException.class,
            () -> PortalGunSkinDefinition.parse("example:icon", blankName));
    }

    @Test
    void standardRendererRejectsEitherDynamicDisplay() {
        for (int mask = 1; mask < 4; mask++) {
            JsonObject json = description("standard", "flat");
            json.add("dynamic", dynamic((mask & 1) != 0, (mask & 2) != 0));
            assertThrows(IllegalArgumentException.class,
                () -> PortalGunSkinDefinition.parse("example:icon", json));
        }
    }

    @Test
    void allCapabilityCombinationsFilterLiquidAndCoreIndependently() {
        // Include empty fuel plus all seven liquid columns, with the core both off and on.
        int[] liquidTints = {0, 2, 3, 4, 5, 6, 7, 8};
        for (int mask = 0; mask < 4; mask++) {
            boolean fluid = (mask & 1) != 0;
            boolean zeroPoint = (mask & 2) != 0;
            JsonObject json = description("layered", "3d");
            json.add("dynamic", dynamic(fluid, zeroPoint));
            PortalGunSkinDefinition skin = PortalGunSkinDefinition.parse("example:icon", json);
            assertEquals(fluid, skin.fluid());
            assertEquals(zeroPoint, skin.zeroPoint());
            for (int liquid : liquidTints) {
                for (boolean core : new boolean[] {false, true}) {
                    int key = skin.geometryKey(liquid, core);
                    for (int tint = 2; tint <= 8; tint++) {
                        assertEquals(fluid && liquid == tint,
                            PortalGunVisualSnapshot.includesTint(key, tint),
                            "mask=" + mask + ", liquid=" + liquid + ", tint=" + tint);
                    }
                    assertEquals(zeroPoint && core, PortalGunVisualSnapshot.includesTint(key, 9));
                    assertEquals(zeroPoint && core, PortalGunVisualSnapshot.includesTint(key, 10));
                    assertTrue(PortalGunVisualSnapshot.includesTint(key, -1));
                    assertTrue(PortalGunVisualSnapshot.includesTint(key, 1));
                }
            }
        }
    }

    @Test
    void skinIdentifiersRequireNamespaceAndRespectTheNetworkLengthLimit() {
        assertTrue(PortalGunSkin.validId(PortalGunSkin.DEFAULT));
        assertTrue(PortalGunSkin.validId("pack-name.v2:skins/path_01"));
        assertTrue(PortalGunSkin.validId("a:" + "b".repeat(254)));
        assertFalse(PortalGunSkin.validId("a:" + "b".repeat(255)));
        assertFalse(PortalGunSkin.validId(null));
        for (String invalid : new String[] {"", "default", ":default", "pack:",
                "pack:skin:extra", "Pack:skin", "pack:Skin", "pack:with space"}) {
            assertFalse(PortalGunSkin.validId(invalid), invalid);
        }
    }

    private static JsonObject description(String renderer, String display) {
        JsonObject json = new JsonObject();
        json.addProperty("name", "skin.example.icon");
        json.addProperty("model", "example:item/icon");
        json.addProperty("renderer", renderer);
        json.addProperty("display", display);
        return json;
    }

    private static JsonObject dynamic(boolean fluid, boolean zeroPoint) {
        JsonObject json = new JsonObject();
        json.addProperty("fluid", fluid);
        json.addProperty("zero_point", zeroPoint);
        return json;
    }
}
