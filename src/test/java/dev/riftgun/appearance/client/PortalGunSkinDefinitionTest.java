package dev.riftgun.appearance.client;

import com.google.gson.JsonObject;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.visual.PortalGunVisualSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalGunSkinDefinitionTest {
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
