package dev.riftgun.sound;

import dev.riftgun.core.RiftConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/** Deep playback module: callers provide an event location and a saved selection snapshot. */
public final class PortalSounds {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, RiftConstants.MOD_ID);

    private static final DeferredHolder<SoundEvent, SoundEvent> RIFT_SHOT =
        fixed("rift_shot", 24.0F);
    private static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PORTAL_OPEN =
        fixed("rift_portal_open", 32.0F);
    private static final DeferredHolder<SoundEvent, SoundEvent> RIFT_PORTAL_CLOSE =
        fixed("rift_portal_close", 32.0F);
    private static final DeferredHolder<SoundEvent, SoundEvent> RIFT_TRANSIT =
        fixed("rift_transit", 24.0F);
    private static final DeferredHolder<SoundEvent, SoundEvent> ENDER_TRANSIT =
        fixed("ender_transit", 24.0F);
    private static boolean suppressClosingSounds;

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    static SoundEvent riftShot() {
        return RIFT_SHOT.get();
    }

    static SoundEvent riftPortalOpen() {
        return RIFT_PORTAL_OPEN.get();
    }

    static SoundEvent riftPortalClose() {
        return RIFT_PORTAL_CLOSE.get();
    }

    static SoundEvent riftTransit() {
        return RIFT_TRANSIT.get();
    }

    static SoundEvent enderTransit() {
        return ENDER_TRANSIT.get();
    }

    public static void playShot(ServerPlayer player, PortalSoundSnapshot sounds) {
        play(player.serverLevel(), player.position(),
            PortalSoundRegistry.primaryCue(PortalSoundChannel.SHOT, sounds.shot()));
    }

    public static void playOpening(ServerLevel level, Vec3 position, PortalSoundSnapshot sounds) {
        play(level, position,
            PortalSoundRegistry.primaryCue(PortalSoundChannel.PORTAL, sounds.portal()));
        if (sounds.splashEnabled()) {
            level.playSound(null, position.x, position.y, position.z,
                SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.28F, 1.15F);
        }
    }

    public static void playClosing(ServerLevel level, Vec3 position, PortalSoundSnapshot sounds) {
        if (suppressClosingSounds) return;
        play(level, position, PortalSoundRegistry.closingCue(sounds.portal()));
    }

    public static void beginServerShutdown() {
        suppressClosingSounds = true;
    }

    public static void endServerShutdown() {
        suppressClosingSounds = false;
    }

    public static void playTransit(ServerLevel level, Vec3 position, PortalSoundSnapshot sounds) {
        play(level, position,
            PortalSoundRegistry.primaryCue(PortalSoundChannel.TRANSIT, sounds.transit()));
    }

    private static void play(ServerLevel level, Vec3 position, @Nullable PortalSoundCue cue) {
        if (cue == null) return;
        level.playSound(null, position.x, position.y, position.z,
            cue.sound().get(), cue.source(), cue.volume(), cue.pitch());
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixed(String path, float range) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, path);
        return SOUND_EVENTS.register(path, () -> SoundEvent.createFixedRangeEvent(id, range));
    }

    private PortalSounds() {}
}
