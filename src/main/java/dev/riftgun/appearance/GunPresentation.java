package dev.riftgun.appearance;

import com.mojang.serialization.Codec;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.sound.PortalSoundSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Immutable per-gun choices. Detailed renderer/recoil parameters remain client preferences. */
public record GunPresentation(String visual, GunShotAnimation animation,
                              PortalSoundSettings sounds, boolean initialized) {
    public static final String DEFAULT_VISUAL = "riftgun:swirl";
    public static final GunPresentation DEFAULT = new GunPresentation(
        DEFAULT_VISUAL, GunShotAnimation.RECOIL, PortalSoundSettings.defaults(), true);
    public static final GunPresentation NEW = new GunPresentation(
        DEFAULT_VISUAL, GunShotAnimation.RECOIL, PortalSoundSettings.defaults(), false);
    public static final Codec<GunPresentation> CODEC = CompoundTag.CODEC.xmap(
        GunPresentation::load, GunPresentation::save);

    public GunPresentation {
        if (!PortalGunSkin.validId(visual)) visual = DEFAULT_VISUAL;
        if (animation == null) animation = GunShotAnimation.RECOIL;
        if (sounds == null) sounds = PortalSoundSettings.defaults();
    }

    public static GunPresentation current(ItemStack stack) {
        return stack.getOrDefault(PortalGunComponents.PRESENTATION, DEFAULT);
    }

    public static boolean needsInitialization(ItemStack stack) {
        GunPresentation value = stack.get(PortalGunComponents.PRESENTATION);
        return value == null || !value.initialized;
    }

    public GunPresentation withVisual(String value) {
        return new GunPresentation(value, animation, sounds, true);
    }

    public GunPresentation withAnimation(GunShotAnimation value) {
        return new GunPresentation(visual, value, sounds, true);
    }

    public GunPresentation withSounds(PortalSoundSettings value) {
        return new GunPresentation(visual, animation, value, true);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Visual", visual);
        tag.putString("Animation", animation.name());
        tag.put("Sounds", sounds.save());
        tag.putBoolean("Initialized", initialized);
        return tag;
    }

    public static GunPresentation load(CompoundTag tag) {
        GunShotAnimation animation;
        try { animation = GunShotAnimation.valueOf(Nbt.getString(tag, "Animation")); }
        catch (IllegalArgumentException error) { animation = GunShotAnimation.RECOIL; }
        return new GunPresentation(Nbt.getString(tag, "Visual"), animation,
            PortalSoundSettings.load(Nbt.getCompound(tag, "Sounds")), Nbt.getBoolean(tag, "Initialized"));
    }
}
