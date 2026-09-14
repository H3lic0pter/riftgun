package dev.riftgun.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import dev.riftgun.config.SkinRecommendationConfig.Category;
import dev.riftgun.core.config.ClientVisualConfig;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.config.RiftConfigs;
import net.neoforged.fml.config.IConfigSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkinRecommendationConfigTest {
    private final ClientVisualConfig previous = RiftConfigs.client();

    @AfterEach
    void restore() {
        ClientConfig.SPEC.acceptConfig(null);
        RiftConfigs.publishClient(previous);
    }

    private static void load(CommentedConfig config) throws ReflectiveOperationException {
        ClientConfig.SPEC.correct(config);
        var constructor = Class.forName("net.neoforged.fml.config.LoadedConfig")
            .getDeclaredConstructor(CommentedConfig.class, java.nio.file.Path.class,
                net.neoforged.fml.config.ModConfig.class);
        constructor.setAccessible(true);
        ClientConfig.SPEC.acceptConfig((IConfigSpec.ILoadedConfig) constructor.newInstance(config, null, null));
        ClientConfig.publishSnapshot();
    }

    @Test
    void defaultsEnableAllCategoriesWithoutOverwritingExistingCustomSettings() throws Exception {
        load(new TomlParser().parse("""
            portalVisualType = "riftgun:classic"
            [visuals.gun]
            animation = "LOWER"
            """));
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        for (Category category : Category.values()) assertTrue(recommendations.enabled(category).get());
        assertEquals("riftgun:classic", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());
    }

    @Test
    void applyingSkinCopiesEnabledCategoriesAndDisabledPreservesGunChoices() throws Exception {
        load(CommentedConfig.inMemory());
        var current = dev.riftgun.appearance.GunPresentation.DEFAULT
            .withVisual("riftgun:classic").withAnimation(GunShotAnimation.LOWER);
        var reference = new net.minecraft.nbt.CompoundTag();
        reference.putString("Instance", java.util.UUID.randomUUID().toString());
        var snapshot = new net.minecraft.nbt.CompoundTag();
        snapshot.putString("Kind", "Appearance");
        snapshot.put("GunReference", reference);
        snapshot.put("Presentation", current.save());
        dev.riftgun.client.appearance.SkinRecommendations.receive(snapshot);
        var request = new net.minecraft.nbt.CompoundTag();
        request.putString("Action", "SET_APPEARANCE");
        request.putString("RequestId", "apply-staff");
        request.putString("Skin", "riftgun:arcane_rift_staff");
        request.put("GunReference", reference);

        var background = new net.minecraft.nbt.CompoundTag();
        background.putString("Kind", "Snapshot");
        var otherReference = new net.minecraft.nbt.CompoundTag();
        otherReference.putString("Instance", java.util.UUID.randomUUID().toString());
        background.put("GunReference", otherReference);
        background.put("Presentation", dev.riftgun.appearance.GunPresentation.DEFAULT.save());
        dev.riftgun.client.appearance.SkinRecommendations.receive(background);

        dev.riftgun.client.appearance.SkinRecommendations.writeRequest(request);

        assertTrue(request.contains("Presentation"), "Background search must not detach the appearance session");
        var copied = dev.riftgun.appearance.GunPresentation.load(
            dev.riftgun.core.nbt.Nbt.getCompound(request, "Presentation"));
        var arcane = new dev.riftgun.sound.PortalSoundSettings(
            dev.riftgun.sound.PortalSoundRegistry.ARCANE_ID,
            dev.riftgun.sound.PortalSoundRegistry.ARCANE_ID,
            dev.riftgun.sound.PortalSoundRegistry.ARCANE_ID, false);
        assertEquals(current.withAnimation(GunShotAnimation.SWING).withSounds(arcane)
            .withVisual("riftgun:magic_circle"), copied);
        for (Category category : Category.values())
            ClientConfig.VALUES.skinRecommendations.enabled(category).set(false);
        dev.riftgun.client.appearance.SkinRecommendations.writeRequest(request);
        assertEquals(current, dev.riftgun.appearance.GunPresentation.load(
            dev.riftgun.core.nbt.Nbt.getCompound(request, "Presentation")));
        assertEquals(current, dev.riftgun.client.appearance.SkinRecommendations.current());
    }

    @Test
    void legacyOverlaysRemainReadableWithoutChangingRuntimeCustomSettings() throws Exception {
        load(CommentedConfig.inMemory());
        ClientConfig.VALUES.gunAnimation.set(GunShotAnimation.LOWER);
        ClientConfig.VALUES.portalVisualType.set("riftgun:classic");
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        for (var value : recommendations.appliedSkins.values()) value.set("riftgun:aperture_ish");
        ClientConfig.publishSnapshot();
        assertEquals("riftgun:classic", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());
        assertEquals("riftgun:endframe", recommendations.visual("riftgun:classic"));
        assertEquals(GunShotAnimation.RECOIL, recommendations.animation(GunShotAnimation.LOWER));
        recommendations.appliedSkins.get(Category.SHOT_ANIMATION).set("riftgun:arcane_rift_staff");
        ClientConfig.publishSnapshot();
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());
        assertEquals(GunShotAnimation.SWING, recommendations.animation(GunShotAnimation.LOWER));
    }
    @Test
    void manualChoicesDoNotEnableDisabledRecommendationSwitches() throws Exception {
        load(CommentedConfig.inMemory());
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        for (Category category : Category.values()) {
            recommendations.enabled(category).set(false);
            recommendations.appliedSkins.get(category).set("riftgun:aperture_ish");
            recommendations.useCustom(category);
            assertFalse(recommendations.enabled(category).get());
            assertNull(recommendations.activePreset(category));
        }
    }

    @Test
    void staffRecommendsArcaneSoundsAndMagicCircleVisual() throws Exception {
        load(CommentedConfig.inMemory());
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        recommendations.appliedSkins.get(Category.PORTAL_VISUAL).set("riftgun:arcane_rift_staff");
        assertEquals("riftgun:magic_circle", recommendations.visual("riftgun:classic"));
        var staff = recommendations.presets.get("riftgun:arcane_rift_staff");
        assertEquals("riftgun:arcane", staff.shotSound.get());
        assertEquals("riftgun:arcane", staff.portalSound.get());
        assertEquals("riftgun:arcane", staff.transitSound.get());
        assertEquals("riftgun:aperture_ish", recommendations.presets.get("riftgun:aperture_ish").shotSound.get());
        assertEquals("riftgun:rift", recommendations.presets.get("riftgun:default").transitSound.get());
    }

    @Test
    void existingLocalStaffRecommendationsAreNotReplacedByNewDefaults() throws Exception {
        load(new TomlParser().parse("""
            [appearance.presets.arcane_rift_staff]
            shotSound = "CUSTOM"
            portalSound = "riftgun:none"
            transitSound = "riftgun:ender"
            portalVisual = "CUSTOM"
            """));
        var staff = ClientConfig.VALUES.skinRecommendations.presets.get("riftgun:arcane_rift_staff");
        assertEquals("CUSTOM", staff.shotSound.get());
        assertEquals("riftgun:none", staff.portalSound.get());
        assertEquals("riftgun:ender", staff.transitSound.get());
        assertEquals("CUSTOM", staff.portalVisual.get());
    }

    @Test
    void editedPresetsAndSavedSelectionSurviveReload() throws Exception {
        var config = new TomlParser().parse("""
            portalVisualType = "riftgun:classic"
            [visuals.gun]
            animation = "OFF"
            [appearance.recommendations]
            portalVisualSkin = "riftgun:aperture_ish"
            shotAnimationSkin = "riftgun:aperture_ish"
            [appearance.presets.aperture_ish]
            portalVisual = "CUSTOM"
            shotAnimation = "LOWER"
            """);
        load(config);
        assertEquals("riftgun:classic", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.OFF, RiftConfigs.client().gunAnimation());
        assertEquals(GunShotAnimation.LOWER, ClientConfig.VALUES.skinRecommendations.animation(GunShotAnimation.OFF));
        config.set("appearance.presets.aperture_ish.shotAnimation", "SWING");
        ClientConfig.SPEC.afterReload();
        ClientConfig.publishSnapshot();
        assertEquals(GunShotAnimation.OFF, RiftConfigs.client().gunAnimation());
        assertEquals(GunShotAnimation.OFF, ClientConfig.VALUES.gunAnimation.get());
        assertEquals(GunShotAnimation.SWING, ClientConfig.VALUES.skinRecommendations.animation(GunShotAnimation.OFF));
    }

    @Test
    void invalidPresetsAreCorrectedAndUnknownSkinsUseCustomSettings() throws Exception {
        load(new TomlParser().parse("""
            [appearance.presets.default]
            shotAnimation = "BROKEN"
            portalVisual = "not an id"
            """));
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        assertEquals("RECOIL", recommendations.presets.get("riftgun:default").shotAnimation.get());
        assertEquals("riftgun:swirl", recommendations.presets.get("riftgun:default").portalVisual.get());
        for (var value : recommendations.appliedSkins.values()) value.set("pack:unknown");
        assertEquals(GunShotAnimation.OFF, recommendations.animation(GunShotAnimation.OFF));
        assertEquals("riftgun:classic", recommendations.visual("riftgun:classic"));
    }
}
