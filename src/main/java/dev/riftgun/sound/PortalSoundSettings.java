package dev.riftgun.sound;
import dev.riftgun.core.nbt.Nbt;

import net.minecraft.nbt.CompoundTag;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Player-owned selection of the three sound roles plus the independent splash layer. */
public record PortalSoundSettings(
//? if >=1.21.11 {
    /*Identifier shot,
*///?} else {
    ResourceLocation shot,
//?}
//? if >=1.21.11 {
    /*Identifier portal,
*///?} else {
    ResourceLocation portal,
//?}
//? if >=1.21.11 {
    /*Identifier transit,
*///?} else {
    ResourceLocation transit,
//?}
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

//? if >=1.21.11 {
    /*public Identifier selected(PortalSoundChannel channel) {
*///?} else {
    public ResourceLocation selected(PortalSoundChannel channel) {
//?}
        return switch (channel) {
            case SHOT -> shot;
            case PORTAL -> portal;
            case TRANSIT -> transit;
        };
    }

//? if >=1.21.11 {
    /*public PortalSoundSettings withSelection(PortalSoundChannel channel, Identifier id) {
*///?} else {
    public PortalSoundSettings withSelection(PortalSoundChannel channel, ResourceLocation id) {
//?}
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
            parse(Nbt.getString(tag, "Shot"), defaults.shot),
            parse(Nbt.getString(tag, "Portal"), defaults.portal),
            parse(Nbt.getString(tag, "Transit"), defaults.transit),
            tag.contains("Splash") && Nbt.getBoolean(tag, "Splash"));
    }

//? if >=1.21.11 {
    /*private static Identifier parse(String value, Identifier fallback) {
*///?} else {
    private static ResourceLocation parse(String value, ResourceLocation fallback) {
//?}
//? if >=1.21.11 {
        /*Identifier parsed = Identifier.tryParse(value);
*///?} else {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
//?}
        return parsed == null ? fallback : parsed;
    }
}
