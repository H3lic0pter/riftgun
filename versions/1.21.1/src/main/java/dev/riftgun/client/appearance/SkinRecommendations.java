package dev.riftgun.client.appearance;

import dev.riftgun.appearance.client.SkinSoundMemory;
import dev.riftgun.appearance.client.PendingSkinSounds;

import dev.riftgun.RiftGun;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.render.PortalVisualPreferences;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.config.SkinRecommendationConfig;
import dev.riftgun.config.SkinRecommendationConfig.Category;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.sound.PortalSoundChannel;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSettings;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Applies client-authored preferences through the existing settings protocol after a matching skin ACK. */
@EventBusSubscriber(modid = RiftGun.MOD_ID, value = Dist.CLIENT)
public final class SkinRecommendations {
    private static final Map<String, String> PENDING = new LinkedHashMap<>();
    private static final PendingSkinSounds SOUND_EDITS = new PendingSkinSounds();
    private static String soundScope;

    public static void onRequest(CompoundTag request) {
        if (!"SET_APPEARANCE".equals(Nbt.getString(request, "Action"))) return;
        String requestId = Nbt.getString(request, "RequestId");
        if (requestId.isEmpty()) return;
        if (PENDING.size() >= 16) PENDING.remove(PENDING.keySet().iterator().next());
        PENDING.put(requestId, Nbt.getString(request, "Skin"));
    }

    public static void receive(CompoundTag response) {
        String expected = PENDING.remove(Nbt.getString(response, "RequestId"));
        if (expected == null || !Nbt.getBoolean(response, "Applied")
            || !Nbt.getString(response, "Error").isEmpty()
            || !expected.equals(Nbt.getString(response, "Skin"))) return;
        apply(expected, Nbt.getCompound(response, "GunReference"));
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        PENDING.clear();
        SOUND_EDITS.clear();
        soundScope = null;
    }

    public static void receiveSounds(CompoundTag response) {
        if (soundScope == null || !soundScope.equals(scope())) return;
        String error = Nbt.getString(response, "Error");
        var server = PortalSoundSettings.load(Nbt.getCompound(response, "PortalSounds"));
        SOUND_EDITS.acknowledge(Nbt.getString(response, "RequestId"), error.isEmpty(), choices(server))
            .ifPresent(confirmation -> {
                if (confirmation.accepted()) {
                    config().soundMemory.set(confirmation.memory());
                    ClientConfig.SPEC.save();
                }
                setVisibleSounds(soundSettings(confirmation.visible()));
                if (!error.isEmpty() && Minecraft.getInstance().player != null) {
                    dev.riftgun.core.msg.Msg.displayClientMessage(Minecraft.getInstance().player,
                        net.minecraft.network.chat.Component.translatable(error), true);
                }
            });
    }

    /** Full snapshots may predate a pending edit; keep the newest optimistic sound selection. */
    public static void preservePendingSounds() {
        if (!SOUND_EDITS.isPending() || soundScope == null || !soundScope.equals(scope())) return;
        setVisibleSounds(soundSettings(SOUND_EDITS.visible(choices(PortalClientState.data().settings().portalSounds()))));
    }

    private static void setVisibleSounds(PortalSoundSettings sounds) {
        var current = PortalClientState.data().settings();
        PortalClientState.data().settings(current.withPortalSounds(sounds));
    }

    public static void apply(String skin, CompoundTag reference) {
        var config = config();
        for (Category category : Category.values()) {
            if (config.enabled(category).get()) config.appliedSkins.get(category).set(skin);
        }
        if (config.sounds.get()) applySounds(reference);
        publish();
    }

    public static void toggle(Category category, String currentSkin, CompoundTag reference) {
        var config = config();
        boolean enabled = !config.enabled(category).get();
        config.enabled(category).set(enabled);
        if (enabled) config.appliedSkins.get(category).set(currentSkin);
        if (category == Category.SOUNDS) applySounds(reference);
        publish();
    }

    /** Explicit edits replace the current overlay; the recommendation switch keeps its value. */
    public static void selectAnimation(GunShotAnimation animation) {
        config().useCustom(Category.SHOT_ANIMATION);
        ClientConfig.VALUES.gunAnimation.set(animation);
        publish();
    }

    public static void selectCustomSounds(PortalSoundSettings sounds) {
        var config = config();
        config.useCustom(Category.SOUNDS);
        String scope = scope();
        if (scope != null) {
            bindSoundScope(scope);
            var memory = SkinSoundMemory.rememberCustom(SOUND_EDITS.memory(config.soundMemory.get()), scope, choices(sounds));
            CompoundTag context = new CompoundTag();
            PortalClientState.writeGunReference(context);
            sendSounds(new SkinSoundMemory.Change(memory, choices(sounds)), Nbt.getCompound(context, "GunReference"));
        }
        ClientConfig.SPEC.save();
    }

    public static PortalSoundSettings customSounds() {
        var current = PortalClientState.data().settings().portalSounds();
        String scope = scope();
        if (scope == null) return current;
        bindSoundScope(scope);
        return soundSettings(SkinSoundMemory.custom(
            SOUND_EDITS.memory(config().soundMemory.get()), scope, SOUND_EDITS.visible(choices(current))));
    }

    private static void applySounds(CompoundTag reference) {
        String scope = scope();
        if (scope == null || reference.isEmpty()) return;
        bindSoundScope(scope);
        var config = config();
        var current = PortalClientState.data().settings();
        var preset = config.sounds.get() ? config.activePreset(Category.SOUNDS) : null;
        var change = SkinSoundMemory.apply(SOUND_EDITS.memory(config.soundMemory.get()), scope,
            SOUND_EDITS.visible(choices(current.portalSounds())), custom -> {
            if (preset == null) return custom;
            return new SkinSoundMemory.Sounds(
                resolveSound(PortalSoundChannel.SHOT, preset.shotSound.get(), custom.shot()),
                resolveSound(PortalSoundChannel.PORTAL, preset.portalSound.get(), custom.portal()),
                resolveSound(PortalSoundChannel.TRANSIT, preset.transitSound.get(), custom.transit()), custom.splash());
        });
        sendSounds(change, reference);
    }

    private static void sendSounds(SkinSoundMemory.Change change, CompoundTag reference) {
        String requestId = UUID.randomUUID().toString();
        SOUND_EDITS.submit(requestId, change);
        var selected = soundSettings(change.sounds());
        setVisibleSounds(selected);
        PortalNetworking.sendRequest(PortalAction.SET_PORTAL_SOUNDS, tag -> {
            tag.putString("RequestId", requestId);
            tag.put("PortalSounds", selected.save());
            tag.put("GunReference", reference.copy());
        });
    }

    private static void bindSoundScope(String scope) {
        if (!scope.equals(soundScope)) {
            SOUND_EDITS.clear();
            soundScope = scope;
        }
        var config = config();
        var captured = SkinSoundMemory.captureCustom(config.soundMemory.get(), scope,
            choices(PortalClientState.data().settings().portalSounds()));
        if (!captured.equals(config.soundMemory.get())) {
            config.soundMemory.set(captured);
            ClientConfig.SPEC.save();
        }
    }

    private static String resolveSound(PortalSoundChannel channel, String recommended, String custom) {
        String id = SkinRecommendationConfig.resolve(recommended, custom);
        // Missing or channel-incompatible preset choices retain custom sounds.
        return PortalSoundRegistry.values(channel).stream().anyMatch(choice -> choice.id().toString().equals(id))
            ? id : custom;
    }

    private static SkinSoundMemory.Sounds choices(PortalSoundSettings settings) {
        return new SkinSoundMemory.Sounds(settings.shot().toString(), settings.portal().toString(),
            settings.transit().toString(), settings.splashEnabled());
    }

    private static PortalSoundSettings soundSettings(SkinSoundMemory.Sounds sounds) {
        return new PortalSoundSettings(ResourceLocation.parse(sounds.shot()), ResourceLocation.parse(sounds.portal()),
            ResourceLocation.parse(sounds.transit()), sounds.splash());

    }

    private static String scope() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return null;
        var server = minecraft.getSingleplayerServer();
        String location;
        if (server != null) location = "save:" + server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        else if (minecraft.getCurrentServer() != null) {
            location = "server:" + minecraft.getCurrentServer().ip.toLowerCase(Locale.ROOT);
        } else return null;
        return location + "|" + minecraft.player.getUUID();
    }

    private static void publish() {
        ClientConfig.publishSnapshot();
        ClientConfig.SPEC.save();
        PortalVisualPreferences.notifySelectionChanged();
    }

    private static SkinRecommendationConfig config() { return ClientConfig.VALUES.skinRecommendations; }
    private SkinRecommendations() {}
}
