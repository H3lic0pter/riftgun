package dev.riftgun.client.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riftgun.appearance.client.PortalGunSkinDefinition;
import dev.riftgun.fuel.PortalGunVisualState;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.render.DynamicAtlasAllocator;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

/** Exercises the same model identity and allocator used by GuiItemAtlas, without a GPU. */
final class PortalGunGuiCacheTest {
    @Test
    void modelOutputAlreadyChangesBeforeGuiCaching() throws Exception {
        var original = model("default");
        var coordinate = render(original, new PortalGunVisualState(0, false, 0xFFFFFF, false));
        var pairing = render(original, new PortalGunVisualState(0, false, 0xFFFFFF, true));
        assertEquals(0xFFF85B5B, submitted(coordinate).tints()[41]);
        assertEquals(0xFFFFD45A, submitted(pairing).tints()[41]);

        var potato = model("aperture_ish");
        var enabled = submitted(render(potato, new PortalGunVisualState(0, true, 0xFFFFFF)));
        var disabled = submitted(render(potato, new PortalGunVisualState(0, false, 0xFFFFFF)));
        assertTrue(enabled.faces().stream().anyMatch(face -> face.materialInfo().tintIndex() == 12));
        assertFalse(disabled.faces().stream().anyMatch(face -> face.materialInfo().tintIndex() == 12));
    }

    @Test
    void pairingColorChangeRequiresANewIcon() throws Exception {
        assertRedraw("default", new PortalGunVisualState(0, false, 0xFFFFFF, false),
            new PortalGunVisualState(0, false, 0xFFFFFF, true));
    }

    @Test
    void removingPotatoRequiresANewIcon() throws Exception {
        assertRedraw("aperture_ish", new PortalGunVisualState(0, true, 0xFFFFFF),
            new PortalGunVisualState(0, false, 0xFFFFFF));
    }

    @Test
    void liquidLevelAndFuelColorRequireNewIcons() throws Exception {
        assertRedraw("default", new PortalGunVisualState(2, false, 0x4FCB72),
            new PortalGunVisualState(8, false, 0x4FCB72));
        assertRedraw("aperture_ish", new PortalGunVisualState(2, false, 0x4FCB72),
            new PortalGunVisualState(2, false, 0x4287F5));
    }

    private static void assertRedraw(String skinId, PortalGunVisualState before,
                                     PortalGunVisualState after) throws Exception {
        var model = model(skinId);
        var allocator = new DynamicAtlasAllocator<Object>(4, 4);
        var first = render(model, before);
        assertEquals(DynamicAtlasAllocator.SlotState.EMPTY,
            allocator.getOrAllocate(first.getModelIdentity(), first.isAnimated()).state());
        allocator.endFrame();
        var changed = render(model, after);
        assertNotEquals(DynamicAtlasAllocator.SlotState.READY,
            allocator.getOrAllocate(changed.getModelIdentity(), changed.isAnimated()).state(),
            "Changed gun appearance reuses the old GUI icon instead of redrawing");
        allocator.endFrame();
        var repeated = render(model, after);
        assertEquals(DynamicAtlasAllocator.SlotState.READY,
            allocator.getOrAllocate(repeated.getModelIdentity(), repeated.isAnimated()).state(),
            "Unchanged appearance should still reuse its cached icon");
    }

    private static TrackingItemStackRenderState render(PortalGunLayeredModel model,
                                                       PortalGunVisualState visual) {
        var item = mock(ItemStack.class);
        var output = new TrackingItemStackRenderState();
        try (var states = mockStatic(PortalGunVisualState.class)) {
            states.when(() -> PortalGunVisualState.current(item)).thenReturn(visual);
            model.update(output, item, null, ItemDisplayContext.GUI, null, null, 0);
        }
        return output;
    }

    private record Submitted(int[] tints, List<BakedQuad> faces) {}

    private static Submitted submitted(TrackingItemStackRenderState output) {
        var collector = mock(SubmitNodeCollector.class);
        var captured = new ArrayList<Submitted>();
        doAnswer(call -> {
            captured.add(new Submitted(call.getArgument(5), call.getArgument(6)));
            return null;
        }).when(collector).submitItem(any(), any(), anyInt(), anyInt(), anyInt(), any(), any(), any());
        output.submit(new PoseStack(), collector, 0, 0, 0);
        assertEquals(1, captured.size());
        return captured.getFirst();
    }

    private static PortalGunLayeredModel model(String skinId) throws Exception {
        PortalGunSkinDefinition skin;
        try (var stream = PortalGunGuiCacheTest.class.getResourceAsStream(
                "/assets/riftgun/portal_gun_skins/" + skinId + ".json")) {
            skin = PortalGunSkinDefinition.parse("riftgun:" + skinId,
                JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject());
        }
        var faces = new ArrayList<BakedQuad>();
        for (int tint : new int[] {-1, 2, 8, 9, 10, 11, 12, 40, 41}) {
            var face = mock(BakedQuad.class, RETURNS_DEEP_STUBS);
            when(face.materialInfo().tintIndex()).thenReturn(tint);
            faces.add(face);
        }
        var variants = new ArrayList<QuadCollection>();
        var extents = new ArrayList<Supplier<Vector3fc[]>>();
        for (int key = 0; key < PortalGunModelLayers.VARIANT_COUNT; key++) {
            int geometryKey = key;
            var quads = mock(QuadCollection.class);
            var visible = faces.stream().filter(face ->
                PortalGunModelLayers.includesTint(geometryKey, face.materialInfo().tintIndex())).toList();
            when(quads.getAll()).thenReturn(visible);
            variants.add(quads);
            extents.add(() -> new Vector3fc[0]);
        }
        return new PortalGunLayeredModel(List.copyOf(variants), List.copyOf(extents),
            mock(ModelRenderProperties.class), new Matrix4f(), skin);
    }
}
