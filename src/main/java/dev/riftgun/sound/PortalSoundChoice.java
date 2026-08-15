package dev.riftgun.sound;

import java.util.Objects;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Stable saved ID and display name for one selectable sound adapter. */
//? if >=1.21.11 {
/*public record PortalSoundChoice(Identifier id, String nameKey) {
*///?} else {
public record PortalSoundChoice(ResourceLocation id, String nameKey) {
//?}
    public PortalSoundChoice {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nameKey);
    }
}
