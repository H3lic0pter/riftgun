package dev.riftgun.sound;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Immutable lifecycle/transit sound selection captured when one portal pair opens. */
public record PortalSoundSnapshot(
    ResourceLocation shot,
    ResourceLocation portal,
    ResourceLocation transit,
    boolean splashEnabled
) {
    public PortalSoundSnapshot {
        shot = PortalSoundRegistry.normalize(PortalSoundChannel.SHOT, shot);
        portal = PortalSoundRegistry.normalize(PortalSoundChannel.PORTAL, portal);
        transit = PortalSoundRegistry.normalize(PortalSoundChannel.TRANSIT, transit);
    }

    public static PortalSoundSnapshot from(PortalSoundSettings settings) {
        return new PortalSoundSnapshot(
            settings.shot(), settings.portal(), settings.transit(), settings.splashEnabled());
    }

    public static PortalSoundSnapshot defaults() {
        return from(PortalSoundSettings.defaults());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Shot", shot.toString());
        tag.putString("Portal", portal.toString());
        tag.putString("Transit", transit.toString());
        tag.putBoolean("Splash", splashEnabled);
        return tag;
    }

    public static PortalSoundSnapshot load(CompoundTag tag) {
        PortalSoundSnapshot defaults = defaults();
        ResourceLocation shot = ResourceLocation.tryParse(tag.getString("Shot"));
        ResourceLocation portal = ResourceLocation.tryParse(tag.getString("Portal"));
        ResourceLocation transit = ResourceLocation.tryParse(tag.getString("Transit"));
        return new PortalSoundSnapshot(
            shot == null ? defaults.shot : shot,
            portal == null ? defaults.portal : portal,
            transit == null ? defaults.transit : transit,
            tag.contains("Splash") && tag.getBoolean("Splash"));
    }
}
