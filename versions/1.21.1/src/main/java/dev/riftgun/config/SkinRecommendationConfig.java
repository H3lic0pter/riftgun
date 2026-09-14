package dev.riftgun.config;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.config.GunShotAnimation;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Local copy templates. Legacy overlay fields remain readable solely for old-gun migration. */
public final class SkinRecommendationConfig {
    public static final String CUSTOM = "CUSTOM";
    public enum Category { SOUNDS, PORTAL_VISUAL, SHOT_ANIMATION }

    public final ModConfigSpec.BooleanValue sounds;
    public final ModConfigSpec.BooleanValue portalVisual;
    public final ModConfigSpec.BooleanValue shotAnimation;
    public final Map<Category, ModConfigSpec.ConfigValue<String>> appliedSkins;
    public final ModConfigSpec.ConfigValue<List<? extends String>> soundMemory;
    public final Map<String, Preset> presets;

    public SkinRecommendationConfig(ModConfigSpec.Builder builder) {
        builder.push("appearance").push("recommendations");
        sounds = builder.comment("Copy recommended sounds into this gun when applying a skin; disabling preserves its sounds")
            .define("sounds", true);
        portalVisual = builder.comment("Apply the recommended portal visual when applying a skin")
            .define("portalVisual", true);
        shotAnimation = builder.comment("Apply the recommended first-person shot animation when applying a skin")
            .define("shotAnimation", true);
        appliedSkins = Map.of(Category.SOUNDS, appliedSkin(builder, "soundSkin"),
            Category.PORTAL_VISUAL, appliedSkin(builder, "portalVisualSkin"),
            Category.SHOT_ANIMATION, appliedSkin(builder, "shotAnimationSkin"));
        soundMemory = builder.comment("Saved custom sounds per server or save and player; managed automatically")
            .defineListAllowEmpty("soundMemory", List.of(), () -> "", value -> value instanceof String);
        builder.pop();
        builder.push("presets");
        presets = Map.of(
            "riftgun:default", new Preset(builder, "default", "riftgun:rift", "riftgun:rift", "riftgun:rift", "riftgun:swirl", "RECOIL"),
            "riftgun:aperture_ish", new Preset(builder, "aperture_ish", "riftgun:aperture_ish", "riftgun:rift", "riftgun:rift", "riftgun:endframe", "RECOIL"),
            "riftgun:arcane_rift_staff", new Preset(builder, "arcane_rift_staff", "riftgun:arcane", "riftgun:arcane", "riftgun:arcane", "riftgun:endframe", "SWING"));
        builder.pop(2);
    }

    public ModConfigSpec.BooleanValue enabled(Category category) {
        return switch (category) {
            case SOUNDS -> sounds;
            case PORTAL_VISUAL -> portalVisual;
            case SHOT_ANIMATION -> shotAnimation;
        };
    }

    private static ModConfigSpec.ConfigValue<String> appliedSkin(ModConfigSpec.Builder builder, String key) {
        return builder.comment("Last applied skin for this recommendation; managed by the appearance screen")
            .define(key, "", value -> value instanceof String id
                && (id.isEmpty() || PortalGunSkin.validId(id)));
    }

    public Preset activePreset(Category category) { return presets.get(appliedSkins.get(category).get()); }

    /** A manual choice replaces the current overlay without changing the next skin-apply preference. */
    public void useCustom(Category category) { appliedSkins.get(category).set(""); }

    public String visual(String custom) {
        Preset preset = activePreset(Category.PORTAL_VISUAL);
        return portalVisual.get() && preset != null ? resolve(preset.portalVisual.get(), custom) : custom;
    }

    public GunShotAnimation animation(GunShotAnimation custom) {
        Preset preset = activePreset(Category.SHOT_ANIMATION);
        return shotAnimation.get() && preset != null
            ? GunShotAnimation.valueOf(resolve(preset.shotAnimation.get(), custom.name())) : custom;
    }

    public static String resolve(String recommended, String custom) {
        return CUSTOM.equals(recommended) ? custom : recommended;
    }

    public static final class Preset {
        public final ModConfigSpec.ConfigValue<String> shotSound, portalSound, transitSound, portalVisual, shotAnimation;

        private Preset(ModConfigSpec.Builder builder, String skin, String shot, String portal, String transit,
                       String visual, String animation) {
            builder.comment("Use CUSTOM to retain the saved custom choice for a field").push(skin);
            shotSound = id(builder, "shotSound", shot);
            portalSound = id(builder, "portalSound", portal);
            transitSound = id(builder, "transitSound", transit);
            portalVisual = id(builder, "portalVisual", visual);
            shotAnimation = builder.comment("CUSTOM, OFF, RECOIL, SWING, or LOWER")
                .define("shotAnimation", animation, value -> {
                    if (CUSTOM.equals(value)) return true;
                    if (!(value instanceof String text)) return false;
                    try { GunShotAnimation.valueOf(text); return true; }
                    catch (IllegalArgumentException ignored) { return false; }
                });
            builder.pop();
        }

        private static ModConfigSpec.ConfigValue<String> id(ModConfigSpec.Builder builder, String key, String value) {
            return builder.define(key, value, input -> input instanceof String text
                && (CUSTOM.equals(text) || PortalGunSkin.validId(text)));
        }
    }
}
