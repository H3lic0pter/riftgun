package dev.riftgun.compat.immersiveportal;

import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.network.ImmersivePortalCapabilityPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/** Server-safe optional gate; direct IP references live behind the bridge. */
public final class ImmersivePortalServerCompat {
    private static final String MOD_ID = "immersive_portals_core";

    public static void tick(MinecraftServer server) {
        if (isLoaded()) ImmersivePortalServerBridge.tick(server);
    }

    public static void clear() {
        if (isLoaded()) ImmersivePortalServerBridge.clear();
    }

    public static void entityJoined(Entity entity) {
        if (!entity.level().isClientSide() && isLoaded()) {
            ImmersivePortalServerBridge.track(entity);
        }
    }

    public static void entityLeft(Entity entity) {
        if (!entity.level().isClientSide() && isLoaded()) {
            ImmersivePortalServerBridge.untrack(entity);
        }
    }

    public static void handleSelection(ServerPlayer player, boolean selected) {
        boolean supported = isLoaded();
        RiftNetwork.sendToPlayer(player, new ImmersivePortalCapabilityPayload(supported));
        if (supported) ImmersivePortalServerBridge.setSelected(player, selected);
    }

    public static void playerLeft(ServerPlayer player) {
        if (isLoaded()) ImmersivePortalServerBridge.removePlayer(player.getUUID());
    }

    private static boolean isLoaded() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    private ImmersivePortalServerCompat() {}
}
