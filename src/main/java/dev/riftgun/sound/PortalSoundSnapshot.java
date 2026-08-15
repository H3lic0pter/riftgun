package dev.riftgun.sound;
import dev.riftgun.core.nbt.Nbt;

import net.minecraft.nbt.CompoundTag;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Immutable lifecycle/transit sound selection captured when one portal pair opens. */
public record PortalSoundSnapshot(
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
//? if >=1.21.11 {
        /*Identifier shot = Identifier.tryParse(Nbt.getString(tag, "Shot"));
*///?} else {
        ResourceLocation shot = ResourceLocation.tryParse(Nbt.getString(tag, "Shot"));
//?}
//? if >=1.21.11 {
        /*Identifier portal = Identifier.tryParse(Nbt.getString(tag, "Portal"));
*///?} else {
        ResourceLocation portal = ResourceLocation.tryParse(Nbt.getString(tag, "Portal"));
//?}
//? if >=1.21.11 {
        /*Identifier transit = Identifier.tryParse(Nbt.getString(tag, "Transit"));
*///?} else {
        ResourceLocation transit = ResourceLocation.tryParse(Nbt.getString(tag, "Transit"));
//?}
        return new PortalSoundSnapshot(
            shot == null ? defaults.shot : shot,
            portal == null ? defaults.portal : portal,
            transit == null ? defaults.transit : transit,
            tag.contains("Splash") && Nbt.getBoolean(tag, "Splash"));
    }
}
