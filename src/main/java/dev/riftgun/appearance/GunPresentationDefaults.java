package dev.riftgun.appearance;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.fuel.PortalGunComponents;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Connection-local, client-authored defaults. They never overwrite an initialized gun. */
public final class GunPresentationDefaults {
    private static final Map<ServerPlayer, CompoundTag> DEFAULTS = new WeakHashMap<>();

    public static void copyConnection(ServerPlayer original, ServerPlayer replacement) {
        CompoundTag values = DEFAULTS.get(original);
        if (values != null) DEFAULTS.put(replacement, values);
    }

    public static void receive(ServerPlayer player, CompoundTag request) {
        CompoundTag values = new CompoundTag();
        for (String key : new String[] {"Legacy", "Custom", "riftgun:default",
                "riftgun:aperture_ish", "riftgun:arcane_rift_staff"}) {
            values.put(key, GunPresentation.load(Nbt.getCompound(request, key)).save());
        }
        DEFAULTS.put(player, values);
    }

    public static boolean initialize(ServerPlayer player, ItemStack stack) {
        if (!GunPresentation.needsInitialization(stack)) return true;
        CompoundTag defaults = DEFAULTS.get(player);
        if (defaults == null) return false; // Do not permanently stamp fallback values before client hello.
        boolean legacy = !stack.has(PortalGunComponents.PRESENTATION);
        String skin = PortalGunSkin.current(stack);
        String key = legacy ? "Legacy" : defaults.contains(skin) ? skin : "Custom";
        GunPresentation selected = GunPresentation.load(Nbt.getCompound(defaults, key));
        if (legacy) selected = selected.withSounds(PortalDataStore.load(player).settings().portalSounds());
        stack.set(PortalGunComponents.PRESENTATION,
            new GunPresentation(selected.visual(), selected.animation(), selected.sounds(), true));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    private GunPresentationDefaults() {}
}
