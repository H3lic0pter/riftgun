package dev.riftgun.sound;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Playback parameters supplied by a sound adapter at the registry seam. */
public record PortalSoundCue(
    Supplier<? extends SoundEvent> sound,
    SoundSource source,
    float volume,
    float pitch
) {
    public PortalSoundCue {
        Objects.requireNonNull(sound);
        Objects.requireNonNull(source);
        if (volume < 0.0F) throw new IllegalArgumentException("volume must be non-negative");
        if (pitch <= 0.0F) throw new IllegalArgumentException("pitch must be positive");
    }
}
