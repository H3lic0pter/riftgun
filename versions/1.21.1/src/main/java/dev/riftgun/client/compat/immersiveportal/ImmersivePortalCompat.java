package dev.riftgun.client.compat.immersiveportal;

import dev.riftgun.client.render.PortalVisualRenderContext;
import dev.riftgun.client.render.PortalVisualPreferences;
import dev.riftgun.client.render.PortalVisualRegistry;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.network.ImmersivePortalHelloPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import java.util.UUID;
import net.neoforged.fml.ModList;

/** Optional client integration. No Immersive Portals classes are referenced. */
public final class ImmersivePortalCompat {
    private static final String MOD_ID = "immersive_portals_core";
    private static boolean serverSupported;
    private static ClientPacketListener connection;

    public static boolean isLoaded() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    public static boolean render(PortalVisualRenderContext context) {
        return isAvailable() && ImmersivePortalBridge.render(context);
    }

    public static void tick(Minecraft minecraft) {
        ClientPacketListener current = minecraft.getConnection();
        if (current != connection) {
            serverSupported = false;
            if (isLoaded()) ImmersivePortalBridge.clear();
            connection = current;
            if (current != null) sendSelection();
        }
        if (isLoaded()) ImmersivePortalBridge.tick(minecraft);
    }

    public static void entityJoined(Entity entity) {
        if (isLoaded()) ImmersivePortalBridge.track(entity);
    }

    public static void entityLeft(Entity entity) {
        if (isLoaded()) ImmersivePortalBridge.untrack(entity);
    }

    public static boolean isAvailable() {
        return isLoaded() && serverSupported;
    }

    public static void sendSelection() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) return;
        RiftNetwork.sendToServer(new ImmersivePortalHelloPayload(
            isLoaded() && PortalVisualPreferences.configuredId()
                .equals(PortalVisualRegistry.IMMERSIVE_PORTAL_ID)));
    }

    public static void handleCapability(boolean supported) {
        serverSupported = supported;
    }

    public static void disconnected() {
        serverSupported = false;
        connection = null;
        if (isLoaded()) ImmersivePortalBridge.clear();
    }

    public static float readiness(UUID portalId) {
        return isLoaded() ? ImmersivePortalBridge.readiness(portalId) : 0.0F;
    }

    private ImmersivePortalCompat() {}
}
