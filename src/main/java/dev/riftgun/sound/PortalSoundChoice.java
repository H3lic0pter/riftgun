package dev.riftgun.sound;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Stable saved ID and display name for one selectable sound adapter. */
public record PortalSoundChoice(ResourceLocation id, String nameKey) {
    public PortalSoundChoice {
        Objects.requireNonNull(id);
        Objects.requireNonNull(nameKey);
    }
}
