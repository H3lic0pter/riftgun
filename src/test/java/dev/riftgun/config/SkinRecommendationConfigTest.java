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
    void switchingPresetsAndDisablingRestoresCustomValuesIndependently() throws Exception {
        load(CommentedConfig.inMemory());
        ClientConfig.VALUES.gunAnimation.set(GunShotAnimation.LOWER);
        ClientConfig.VALUES.portalVisualType.set("riftgun:classic");
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        for (var value : recommendations.appliedSkins.values()) value.set("riftgun:aperture_ish");
        ClientConfig.publishSnapshot();
        assertEquals("riftgun:endframe", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.RECOIL, RiftConfigs.client().gunAnimation());
        recommendations.appliedSkins.get(Category.SHOT_ANIMATION).set("riftgun:arcane_rift_staff");
        ClientConfig.publishSnapshot();
        assertEquals(GunShotAnimation.SWING, RiftConfigs.client().gunAnimation());
        assertEquals("riftgun:endframe", RiftConfigs.client().portalVisualType());
        recommendations.shotAnimation.set(false);
        ClientConfig.publishSnapshot();
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());
        assertEquals("riftgun:endframe", RiftConfigs.client().portalVisualType());
        recommendations.portalVisual.set(false);
        ClientConfig.publishSnapshot();
        assertEquals("riftgun:classic", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.LOWER, ClientConfig.VALUES.gunAnimation.get());
    }

    @Test
    void manualChoicesReplaceOverlaysButKeepSwitchesAcrossReloadAndTheNextApply() throws Exception {
        var config = CommentedConfig.inMemory();
        load(config);
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        for (var value : recommendations.appliedSkins.values()) value.set("riftgun:aperture_ish");
        ClientConfig.publishSnapshot();
        assertEquals("riftgun:endframe", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.RECOIL, RiftConfigs.client().gunAnimation());

        ClientConfig.VALUES.portalVisualType.set("riftgun:classic");
        ClientConfig.VALUES.gunAnimation.set(GunShotAnimation.LOWER);
        for (Category category : Category.values()) recommendations.useCustom(category);
        ClientConfig.SPEC.afterReload();
        ClientConfig.publishSnapshot();
        for (Category category : Category.values()) {
            assertTrue(recommendations.enabled(category).get());
            assertNull(recommendations.activePreset(category));
        }
        assertEquals("riftgun:classic", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());

        for (Category category : Category.values()) {
            if (recommendations.enabled(category).get()) {
                recommendations.appliedSkins.get(category).set("riftgun:aperture_ish");
            }
        }
        ClientConfig.publishSnapshot();
        assertEquals("riftgun:endframe", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.RECOIL, RiftConfigs.client().gunAnimation());
        recommendations.portalVisual.set(false);
        recommendations.shotAnimation.set(false);
        ClientConfig.publishSnapshot();
        assertEquals("riftgun:classic", RiftConfigs.client().portalVisualType());
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());
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
    void staffKeepsCustomVisualAndSoundFields() throws Exception {
        load(CommentedConfig.inMemory());
        var recommendations = ClientConfig.VALUES.skinRecommendations;
        recommendations.appliedSkins.get(Category.PORTAL_VISUAL).set("riftgun:arcane_rift_staff");
        assertEquals("riftgun:classic", recommendations.visual("riftgun:classic"));
        var staff = recommendations.presets.get("riftgun:arcane_rift_staff");
        assertEquals("CUSTOM", staff.shotSound.get());
        assertEquals("CUSTOM", staff.portalSound.get());
        assertEquals("CUSTOM", staff.transitSound.get());
        assertEquals("riftgun:aperture_ish", recommendations.presets.get("riftgun:aperture_ish").shotSound.get());
        assertEquals("riftgun:rift", recommendations.presets.get("riftgun:default").transitSound.get());
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
        assertEquals(GunShotAnimation.LOWER, RiftConfigs.client().gunAnimation());
        config.set("appearance.presets.aperture_ish.shotAnimation", "SWING");
        ClientConfig.SPEC.afterReload();
        ClientConfig.publishSnapshot();
        assertEquals(GunShotAnimation.SWING, RiftConfigs.client().gunAnimation());
        assertEquals(GunShotAnimation.OFF, ClientConfig.VALUES.gunAnimation.get());
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
