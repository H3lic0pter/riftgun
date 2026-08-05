package dev.riftgun;

import dev.riftgun.data.PortalDataStore;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.service.PortalServices;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = RiftGun.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
            PortalDataStore.copy(original, replacement);
            PortalServices.MOTION_HISTORY.remove(original.getUUID());
            PortalServices.MOTION_HISTORY.setPredictionEnabled(replacement,
                PortalDataStore.load(replacement).settings().motionPredictionEnabled());
        }
    }

    @SubscribeEvent
    public static void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortalServices.MOTION_HISTORY.setPredictionEnabled(player,
                PortalDataStore.load(player).settings().motionPredictionEnabled());
            PortalNetworking.sendSnapshot(player, false);
        }
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortalServices.MOTION_HISTORY.remove(player.getUUID());
            closeOwned(player);
        }
    }

    @SubscribeEvent
    public static void playerDeath(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortalServices.MOTION_HISTORY.reset(player);
            closeOwned(player);
        }
    }

    @SubscribeEvent
    public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PortalServices.MOTION_HISTORY.reset(player);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        PortalServices.MOTION_HISTORY.tick(event.getServer());
    }

    private static void closeOwned(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server != null) PortalEntity.closeOwnedPortals(server, player.getUUID());
    }

    private CommonEvents() {}
}
