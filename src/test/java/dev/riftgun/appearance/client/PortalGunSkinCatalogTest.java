package dev.riftgun.appearance.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.riftgun.appearance.PortalGunSkin;
import java.io.StringReader;
import java.util.Map;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class PortalGunSkinCatalogTest {
    @AfterEach
    void reset() {
        PortalGunSkinCatalog.install(Map.of(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT));
    }

    @Test
    void unknownIdentityFallsBackWithoutBeingAddedAndRecoversAfterReload() {
        reset();
        assertEquals(PortalGunSkinDefinition.DEFAULT, PortalGunSkinCatalog.resolve("addon:flat"));
        assertEquals(PortalGunSkinDefinition.DEFAULT, PortalGunSkinCatalog.resolve(null));
        assertFalse(PortalGunSkinCatalog.contains(null));
        assertFalse(PortalGunSkinCatalog.contains("addon:flat"));
        var skin = new PortalGunSkinDefinition("addon:flat", "name", "addon:item/flat", false, true, false, false);
        PortalGunSkinCatalog.install(Map.of(PortalGunSkin.DEFAULT, PortalGunSkinDefinition.DEFAULT, skin.id(), skin));
        assertSame(skin, PortalGunSkinCatalog.resolve("addon:flat"));
        reset();
        assertFalse(PortalGunSkinCatalog.contains("addon:flat"));
    }

    @Test
    void reloadPublishesStableSnapshotEvenWhenOnlyAnExistingDefinitionChanges() {
        reset();
        var before = PortalGunSkinCatalog.snapshot();
        assertSame(before, PortalGunSkinCatalog.snapshot());
        var replacement = new PortalGunSkinDefinition(PortalGunSkin.DEFAULT, "replacement",
            "addon:item/flat", false, true, false, false);
        var input = new java.util.HashMap<>(Map.of(PortalGunSkin.DEFAULT, replacement));
        PortalGunSkinCatalog.install(input);
        var after = PortalGunSkinCatalog.snapshot();
        input.clear();

        assertNotSame(before, after);
        assertEquals(before.size(), after.size());
        assertSame(replacement, after.get(PortalGunSkin.DEFAULT));
        assertSame(after, PortalGunSkinCatalog.snapshot());
        assertSame(PortalGunSkinDefinition.DEFAULT, before.get(PortalGunSkin.DEFAULT));
        assertThrows(UnsupportedOperationException.class, after::clear);
    }

    @Test
    void loadsAdditionalSkinFromWinningResourceAndValidatesParentChain() throws Exception {
        ResourceManager resources = mock(ResourceManager.class);
        when(resources.listResources(eq("portal_gun_skins"), any())).thenAnswer(ignored -> Map.of(
            id("addon:portal_gun_skins/flat.json"), resource(description("addon:item/flat"))));
        when(resources.listResources(eq("models"), any())).thenAnswer(ignored -> Map.of(
            id("addon:models/item/flat.json"), resource("{\"parent\":\"minecraft:item/generated\"}"),
            id("minecraft:models/item/generated.json"), resource("{\"parent\":\"builtin/generated\"}")));
        var loaded = PortalGunSkinCatalog.load(resources);
        assertTrue(loaded.containsKey("addon:flat"));
        assertTrue(loaded.get("addon:flat").flat());
        assertTrue(loaded.containsKey(PortalGunSkin.DEFAULT));
    }

    @Test
    void oneBrokenSkinDoesNotHideOtherSkinsOrTheDefault() throws Exception {
        ResourceManager resources = mock(ResourceManager.class);
        when(resources.listResources(eq("portal_gun_skins"), any())).thenAnswer(ignored -> Map.of(
            id("addon:portal_gun_skins/good.json"), resource(description("addon:item/good")),
            id("addon:portal_gun_skins/missing.json"), resource(description("addon:item/missing")),
            id("addon:portal_gun_skins/parent.json"), resource(description("addon:item/parent")),
            id("addon:portal_gun_skins/cycle.json"), resource(description("addon:item/cycle")),
            id("addon:portal_gun_skins/broken.json"), resource("{invalid")));
        when(resources.listResources(eq("models"), any())).thenAnswer(ignored -> Map.of(
            id("addon:models/item/good.json"), resource("{}"),
            id("addon:models/item/parent.json"), resource("{\"parent\":\"addon:missing_parent\"}"),
            id("addon:models/item/cycle.json"), resource("{\"parent\":\"addon:item/cycle\"}")));
        var loaded = PortalGunSkinCatalog.load(resources);
        assertEquals(java.util.Set.of(PortalGunSkin.DEFAULT, "addon:good"), loaded.keySet());
    }

    @Test
    void validResourcePackCanOverrideDefaultIdentity() throws Exception {
        ResourceManager resources = mock(ResourceManager.class);
        when(resources.listResources(eq("portal_gun_skins"), any())).thenAnswer(ignored -> Map.of(
            id("riftgun:portal_gun_skins/default.json"), resource(description("addon:item/flat"))));
        when(resources.listResources(eq("models"), any())).thenAnswer(ignored -> Map.of(
            id("addon:models/item/flat.json"), resource("{}")));
        assertFalse(PortalGunSkinCatalog.load(resources).get(PortalGunSkin.DEFAULT).layered());
    }

    private static String description(String model) {
        return "{\"name\":\"skin.name\",\"model\":\"" + model
            + "\",\"renderer\":\"standard\",\"display\":\"flat\"}";
    }

    private static Resource resource(String json) throws Exception {
        Resource resource = mock(Resource.class);
        when(resource.openAsReader()).thenAnswer(ignored -> new java.io.BufferedReader(new StringReader(json)));
        return resource;
    }

//? if >=1.21.11 {
    /*private static Identifier id(String value) { return Identifier.parse(value); }
*///?} else {
    private static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
//?}
}
