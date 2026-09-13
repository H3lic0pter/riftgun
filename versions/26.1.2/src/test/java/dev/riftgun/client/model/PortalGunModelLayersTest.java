package dev.riftgun.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import org.junit.jupiter.api.Test;

final class PortalGunModelLayersTest {
    @Test
    void bakedModelRejectsOversizedTintBeforeAnyRenderAllocation() {
        for (int tint : new int[] {101, 1000000, Integer.MAX_VALUE}) {
            var quad = mock(BakedQuad.class, RETURNS_DEEP_STUBS);
            when(quad.materialInfo().tintIndex()).thenReturn(tint);
            var quads = mock(QuadCollection.class);
            when(quads.getAll()).thenReturn(List.of(quad));
            assertThrows(IllegalArgumentException.class,
                () -> new PortalGunLayeredModel(List.of(quads), List.of(), null, null));
        }
    }

    @Test
    void tintSlotsCoverTheActualModelIncludingAdditionalModeIndicators() {
        var quad = mock(BakedQuad.class, RETURNS_DEEP_STUBS);
        when(quad.materialInfo().tintIndex()).thenReturn(100);
        var quads = mock(QuadCollection.class);
        when(quads.getAll()).thenReturn(List.of(quad));
        var model = new PortalGunLayeredModel(List.of(quads), List.of(), null, null);
        assertEquals(100, model.maxTintIndex());
        for (int key = 0; key < PortalGunModelLayers.VARIANT_COUNT; key++) {
            assertTrue(PortalGunModelLayers.includesTint(key, 100));
        }
    }

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
        assertTrue(PortalGunModelLayers.includesTint(0, 11));
        assertTrue(PortalGunModelLayers.includesTint(15, 11));
        assertFalse(PortalGunModelLayers.includesTint(0, 12));
        assertTrue(PortalGunModelLayers.includesTint(8, 12));
        assertTrue(PortalGunModelLayers.includesTint(15, 12));
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

        assertEquals(212, counts[0], "empty gun should contain only fixed model faces");
        assertEquals(218, counts[1], "one liquid level adds one six-face cuboid");
        assertEquals(224, counts[8], "the two core cuboids add twelve faces");
        assertEquals(230, counts[15], "liquid and core must not restore hidden levels");
    }
}
