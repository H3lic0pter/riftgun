package dev.riftgun.sound;

import dev.riftgun.core.RiftConstants;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * Registry seam for independently replaceable shot, portal-lifecycle, and transit sound adapters.
 * Saved settings only retain stable choice IDs; playback details remain local to this module.
 */
public final class PortalSoundRegistry {
    public static final ResourceLocation NONE_ID = id("none");
    public static final ResourceLocation RIFT_ID = id("rift");
    public static final ResourceLocation ENDER_ID = id("ender");

    private static final Map<PortalSoundChannel, LinkedHashMap<ResourceLocation, Definition>> DEFINITIONS =
        new EnumMap<>(PortalSoundChannel.class);

    static {
        for (PortalSoundChannel channel : PortalSoundChannel.values()) {
            DEFINITIONS.put(channel, new LinkedHashMap<>());
        }
        PortalSoundChoice none = new PortalSoundChoice(NONE_ID, "screen.riftgun.sound.none");
        PortalSoundChoice rift = new PortalSoundChoice(RIFT_ID, "screen.riftgun.sound.rift");
        registerShot(rift, new PortalSoundCue(
            PortalSounds::riftShot, SoundSource.PLAYERS, 0.8F, 1.0F));
        registerPortal(rift,
            new PortalSoundCue(PortalSounds::riftPortalOpen,
                SoundSource.BLOCKS, 0.85F, 1.0F),
            new PortalSoundCue(PortalSounds::riftPortalClose,
                SoundSource.BLOCKS, 0.8F, 1.0F));
        registerTransit(rift, new PortalSoundCue(
            PortalSounds::riftTransit, SoundSource.PLAYERS, 0.75F, 1.0F));

        registerTransit(new PortalSoundChoice(ENDER_ID, "screen.riftgun.sound.ender"),
            new PortalSoundCue(PortalSounds::enderTransit,
                SoundSource.PLAYERS, 0.6F, 1.4F));
        registerShot(none, null);
        registerPortal(none, null, null);
        registerTransit(none, null);
    }

    public static PortalSoundChoice registerShot(PortalSoundChoice choice,
                                                  @Nullable PortalSoundCue cue) {
        return register(PortalSoundChannel.SHOT, new Definition(choice, cue, null));
    }

    public static PortalSoundChoice registerPortal(PortalSoundChoice choice,
                                                    @Nullable PortalSoundCue opening,
                                                    @Nullable PortalSoundCue closing) {
        return register(PortalSoundChannel.PORTAL, new Definition(choice, opening, closing));
    }

    public static PortalSoundChoice registerTransit(PortalSoundChoice choice,
                                                     @Nullable PortalSoundCue cue) {
        return register(PortalSoundChannel.TRANSIT, new Definition(choice, cue, null));
    }

    public static List<PortalSoundChoice> values(PortalSoundChannel channel) {
        return DEFINITIONS.get(channel).values().stream().map(Definition::choice).toList();
    }

    public static PortalSoundChoice resolve(PortalSoundChannel channel, ResourceLocation requested) {
        return definition(channel, requested).choice();
    }

    public static ResourceLocation normalize(PortalSoundChannel channel, ResourceLocation requested) {
        return resolve(channel, requested).id();
    }

    public static ResourceLocation cycle(PortalSoundChannel channel, ResourceLocation selected, int direction) {
        List<PortalSoundChoice> choices = values(channel);
        if (choices.isEmpty()) return defaultId(channel);
        ResourceLocation normalized = normalize(channel, selected);
        int index = 0;
        for (int candidate = 0; candidate < choices.size(); candidate++) {
            if (choices.get(candidate).id().equals(normalized)) {
                index = candidate;
                break;
            }
        }
        return choices.get(Math.floorMod(index + direction, choices.size())).id();
    }

    static @Nullable PortalSoundCue primaryCue(PortalSoundChannel channel, ResourceLocation id) {
        return definition(channel, id).primary();
    }

    static @Nullable PortalSoundCue closingCue(ResourceLocation id) {
        return definition(PortalSoundChannel.PORTAL, id).secondary();
    }

    private static PortalSoundChoice register(PortalSoundChannel channel, Definition definition) {
        Objects.requireNonNull(channel);
        Objects.requireNonNull(definition);
        Definition previous = DEFINITIONS.get(channel).putIfAbsent(definition.choice().id(), definition);
        if (previous != null) {
            throw new IllegalArgumentException(
                "Duplicate portal sound choice for " + channel + ": " + definition.choice().id());
        }
        return definition.choice();
    }

    private static Definition definition(PortalSoundChannel channel, ResourceLocation requested) {
        Map<ResourceLocation, Definition> definitions = DEFINITIONS.get(channel);
        Definition resolved = definitions.get(requested);
        if (resolved != null) return resolved;
        resolved = definitions.get(defaultId(channel));
        if (resolved != null) return resolved;
        return definitions.values().iterator().next();
    }

    private static ResourceLocation defaultId(PortalSoundChannel channel) {
        return RIFT_ID;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, path);
    }

    private record Definition(
        PortalSoundChoice choice,
        @Nullable PortalSoundCue primary,
        @Nullable PortalSoundCue secondary
    ) {
        private Definition {
            Objects.requireNonNull(choice);
        }
    }

    private PortalSoundRegistry() {}
}
