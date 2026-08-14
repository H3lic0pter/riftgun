package dev.riftgun.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class PortalGunModelLayersTest {
    @Test
    void selectsOnlyTheActiveLiquidLevelAndOptionalCore() {
        assertTrue(PortalGunModelLayers.includesTint(0, -1));
        assertFalse(PortalGunModelLayers.includesTint(0, 2));
        assertFalse(PortalGunModelLayers.includesTint(0, 9));

        assertTrue(PortalGunModelLayers.includesTint(1, 2));
        assertFalse(PortalGunModelLayers.includesTint(1, 3));
        assertTrue(PortalGunModelLayers.includesTint(8, 9));
        assertTrue(PortalGunModelLayers.includesTint(8, 10));
        assertFalse(PortalGunModelLayers.includesTint(8, 2));
        assertTrue(PortalGunModelLayers.includesTint(15, 8));
    }

    @Test
    void canonicalModelProducesExpectedVariantFaceCounts() throws Exception {
        JsonObject model;
        try (var stream = getClass().getResourceAsStream(
                "/assets/riftgun/models/item/portal_gun.json")) {
            model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
        }

        int[] counts = new int[PortalGunModelLayers.VARIANT_COUNT];
        model.getAsJsonArray("elements").forEach(element -> element.getAsJsonObject()
            .getAsJsonObject("faces").entrySet().forEach(face -> {
                JsonObject definition = face.getValue().getAsJsonObject();
                int tint = definition.has("tintindex") ? definition.get("tintindex").getAsInt() : -1;
                for (int key = 0; key < counts.length; key++) {
                    if (PortalGunModelLayers.includesTint(key, tint)) counts[key]++;
                }
            }));

        assertEquals(230, counts[0], "empty gun should contain only fixed model faces");
        assertEquals(236, counts[1], "one liquid level adds one six-face cuboid");
        assertEquals(242, counts[8], "the two core cuboids add twelve faces");
        assertEquals(248, counts[15], "liquid and core must not restore hidden levels");
    }
}
