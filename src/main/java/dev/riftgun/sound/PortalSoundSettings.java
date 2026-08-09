package dev.riftgun.sound;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Player-owned selection of the three sound roles plus the independent splash layer. */
public record PortalSoundSettings(
    ResourceLocation shot,
    ResourceLocation portal,
    ResourceLocation transit,
    boolean splashEnabled
) {
    public PortalSoundSettings {
        shot = PortalSoundRegistry.normalize(PortalSoundChannel.SHOT, shot);
        portal = PortalSoundRegistry.normalize(PortalSoundChannel.PORTAL, portal);
        transit = PortalSoundRegistry.normalize(PortalSoundChannel.TRANSIT, transit);
    }

    public static PortalSoundSettings defaults() {
        return new PortalSoundSettings(PortalSoundRegistry.RIFT_ID, PortalSoundRegistry.RIFT_ID,
            PortalSoundRegistry.RIFT_ID, false);
    }

    public ResourceLocation selected(PortalSoundChannel channel) {
        return switch (channel) {
            case SHOT -> shot;
            case PORTAL -> portal;
            case TRANSIT -> transit;
        };
    }

    public PortalSoundSettings withSelection(PortalSoundChannel channel, ResourceLocation id) {
        return switch (channel) {
            case SHOT -> new PortalSoundSettings(id, portal, transit, splashEnabled);
            case PORTAL -> new PortalSoundSettings(shot, id, transit, splashEnabled);
            case TRANSIT -> new PortalSoundSettings(shot, portal, id, splashEnabled);
        };
    }

    public PortalSoundSettings withSplashEnabled(boolean enabled) {
        return new PortalSoundSettings(shot, portal, transit, enabled);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Shot", shot.toString());
        tag.putString("Portal", portal.toString());
        tag.putString("Transit", transit.toString());
        tag.putBoolean("Splash", splashEnabled);
        return tag;
    }

    public static PortalSoundSettings load(CompoundTag tag) {
        PortalSoundSettings defaults = defaults();
        return new PortalSoundSettings(
            parse(tag.getString("Shot"), defaults.shot),
            parse(tag.getString("Portal"), defaults.portal),
            parse(tag.getString("Transit"), defaults.transit),
            tag.contains("Splash") && tag.getBoolean("Splash"));
    }

    private static ResourceLocation parse(String value, ResourceLocation fallback) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        return parsed == null ? fallback : parsed;
    }
}
