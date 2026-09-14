package dev.riftgun.client.appearance;

import dev.riftgun.appearance.GunPresentation;
import dev.riftgun.appearance.client.SkinSoundMemory;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.config.SkinRecommendationConfig;
import dev.riftgun.config.SkinRecommendationConfig.Category;
import dev.riftgun.core.config.GunShotAnimation;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.sound.PortalSoundSettings;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** Copies recommendations on demand; edits and acknowledgements belong to one exact gun. */
public final class SkinRecommendations {
    private static Object connection;
    private static boolean defaultsDirty = true;
    private static long configRevision = -1;
    private static Object owner;
    private static PortalSoundSettings legacySounds;
    private static CompoundTag reference = new CompoundTag();
    private static GunPresentation current = GunPresentation.DEFAULT;
    private static String pendingRequest = "";

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        var latestSounds = PortalClientState.data().settings().portalSounds();
        if (owner != minecraft.player || configRevision != ClientConfig.revision()
                || !latestSounds.equals(legacySounds)) {
            owner = minecraft.player;
            configRevision = ClientConfig.revision();
            legacySounds = latestSounds;
            defaultsDirty = true;
        }
        if (connection != minecraft.getConnection()) {
            connection = minecraft.getConnection();
            reference = new CompoundTag();
            current = GunPresentation.DEFAULT;
            pendingRequest = "";
            defaultsDirty = true;
        }
        if (connection != null && minecraft.player != null && defaultsDirty) {
            defaultsDirty = false;
            PortalNetworking.sendRequest(PortalAction.SYNC_PRESENTATION_DEFAULTS, tag -> {
                var custom = localCustom();
                var config = ClientConfig.VALUES.skinRecommendations;
                tag.put("Custom", custom.save());
                tag.put("Legacy", custom.withVisual(config.visual(custom.visual()))
                    .withAnimation(config.animation(custom.animation())).save());
                config.presets.forEach((skin, preset) -> tag.put(skin, recommended(skin, custom, false).save()));
            });
        }
    }

    private static GunPresentation localCustom() {
        var settings = PortalClientState.data().settings().portalSounds();
        String scope = scope();
        if (scope != null) {
            var saved = SkinSoundMemory.custom(ClientConfig.VALUES.skinRecommendations.soundMemory.get(), scope,
                new SkinSoundMemory.Sounds(settings.shot().toString(), settings.portal().toString(),
                    settings.transit().toString(), settings.splashEnabled()));
            settings = new PortalSoundSettings(Identifier.parse(saved.shot()),
                Identifier.parse(saved.portal()), Identifier.parse(saved.transit()), saved.splash());
        }
        return new GunPresentation(ClientConfig.VALUES.portalVisualType.get(),
            ClientConfig.VALUES.gunAnimation.get(), settings, true);
    }

    public static GunPresentation current() { return current; }

    public static GunShotAnimation animation(ItemStack stack) {
        var saved = stack.get(dev.riftgun.fuel.PortalGunComponents.PRESENTATION);
        if (saved != null && saved.initialized()) return saved.animation();
        var config = ClientConfig.VALUES.skinRecommendations;
        if (saved != null) {
            var preset = config.presets.get(dev.riftgun.appearance.PortalGunSkin.current(stack));
            if (preset != null) return GunShotAnimation.valueOf(SkinRecommendationConfig.resolve(
                preset.shotAnimation.get(), ClientConfig.VALUES.gunAnimation.get().name()));
            return ClientConfig.VALUES.gunAnimation.get();
        }
        return config.animation(ClientConfig.VALUES.gunAnimation.get());
    }

    /** Returns whether the response belongs to the current gun context, even if an edit is pending. */
    public static boolean receive(CompoundTag response) {
        if (!response.contains("GunReference")) return false;
        CompoundTag incoming = Nbt.getCompound(response, "GunReference");
        String requestId = Nbt.getString(response, "RequestId");
        String kind = Nbt.getString(response, "Kind");
        boolean opensGun = "Snapshot".equals(kind)
                && (Nbt.getBoolean(response, "OpenScreen") || Nbt.getBoolean(response, "OpenRadial"))
            || "Appearance".equals(kind) && !Nbt.getBoolean(response, "Applied");
        // Search completions and slider replies may refer to a gun whose editor is already closed.
        if (!opensGun && !incoming.equals(reference)) return false;
        boolean acknowledgement = "GunPresentation".equals(kind)
            || Nbt.getBoolean(response, "Applied");
        if (acknowledgement && (pendingRequest.isEmpty() || !pendingRequest.equals(requestId)
                || !incoming.equals(reference))) return false;
        if (!pendingRequest.isEmpty()) {
            // Other state for this gun can refresh without rolling back its optimistic presentation.
            if (!opensGun && !pendingRequest.equals(requestId)) return true;
            pendingRequest = "";
        }
        if (!response.contains("Presentation")) return false;
        reference = incoming.copy();
        current = GunPresentation.load(Nbt.getCompound(response, "Presentation"));
        return true;
    }

    public static void writeRequest(CompoundTag request) {
        if (!"SET_APPEARANCE".equals(Nbt.getString(request, "Action"))) return;
        if (!Nbt.getCompound(request, "GunReference").equals(reference)) return;
        request.put("Presentation", recommended(Nbt.getString(request, "Skin"), current, true).save());
        pendingRequest = Nbt.getString(request, "RequestId");
    }

    public static void toggle(Category category, String skin, CompoundTag gunReference) {
        var enabled = ClientConfig.VALUES.skinRecommendations.enabled(category);
        enabled.set(!enabled.get());
        ClientConfig.SPEC.save();
    }

    public static void selectAnimation(GunShotAnimation animation) {
        edit(Category.SHOT_ANIMATION, current.withAnimation(animation));
    }

    public static void selectVisual(String visual) {
        edit(Category.PORTAL_VISUAL, current.withVisual(visual));
    }

    public static PortalSoundSettings customSounds() { return current.sounds(); }

    public static void selectCustomSounds(PortalSoundSettings sounds) {
        edit(Category.SOUNDS, current.withSounds(sounds));
    }

    private static void edit(Category category, GunPresentation value) {
        if (reference.isEmpty()) return;
        current = value;
        pendingRequest = UUID.randomUUID().toString();
        PortalNetworking.sendRequest(PortalAction.SET_GUN_PRESENTATION, tag -> {
            tag.put("GunReference", reference.copy());
            tag.putString("RequestId", pendingRequest);
            tag.putString("Category", category.name());
            tag.put("Presentation", value.save());
        });
    }

    private static GunPresentation recommended(String skin, GunPresentation base, boolean honorSwitches) {
        var config = ClientConfig.VALUES.skinRecommendations;
        var preset = config.presets.get(skin);
        if (preset == null) return base;
        String visual = base.visual();
        GunShotAnimation animation = base.animation();
        PortalSoundSettings sounds = base.sounds();
        if (!honorSwitches || config.portalVisual.get()) {
            visual = SkinRecommendationConfig.resolve(preset.portalVisual.get(), visual);
        }
        if (!honorSwitches || config.shotAnimation.get()) {
            animation = GunShotAnimation.valueOf(SkinRecommendationConfig.resolve(
                preset.shotAnimation.get(), animation.name()));
        }
        if (!honorSwitches || config.sounds.get()) {
            sounds = new PortalSoundSettings(
                Identifier.parse(SkinRecommendationConfig.resolve(preset.shotSound.get(), sounds.shot().toString())),
                Identifier.parse(SkinRecommendationConfig.resolve(preset.portalSound.get(), sounds.portal().toString())),
                Identifier.parse(SkinRecommendationConfig.resolve(preset.transitSound.get(), sounds.transit().toString())),
                sounds.splashEnabled());
        }
        return new GunPresentation(visual, animation, sounds, true);
    }

    private static String scope() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return null;
        String place;
        if (minecraft.getSingleplayerServer() != null) {
            place = "save:" + minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        } else if (minecraft.getCurrentServer() != null) {
            place = "server:" + minecraft.getCurrentServer().ip.toLowerCase(java.util.Locale.ROOT);
        } else return null;
        return place + "|" + minecraft.player.getUUID();
    }

    private SkinRecommendations() {}
}
