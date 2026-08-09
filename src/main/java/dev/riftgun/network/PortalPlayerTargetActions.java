package dev.riftgun.network;

import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalOpenCoordinator;
import dev.riftgun.service.ServerPlayerRoster;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Player Target selection, roster and portal actions. */
final class PortalPlayerTargetActions {
    static boolean select(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        UUID playerId = PortalRequestFields.id(request, "Target");
        if (player.getServer() == null || !ServerPlayerRoster.isOnline(player.getServer(), playerId)) {
            throw PortalRequestFields.error("message.riftgun.player_target_offline");
        }
        data.selectedDestinationId(null);
        data.selectedPlayerId(playerId);
        data.lastViewedDestinationId(null);
        return true;
    }

    static void sendList(ServerPlayer player, ItemStack gun) {
        if (PortalGunModules.activeCount(gun, PortalModuleKind.PLAYER_TARGET,
            PortalModuleRules.current()) <= 0) return;
        PortalPlayerData data = PortalDataStore.load(player);
        data.prunePlayerTargets(ServerPlayerRoster.onlinePlayerIds(player.getServer()));
        PortalDataStore.save(player, data);
        PortalNetworking.sendPlayerList(player);
    }

    static void openFromRequest(ServerPlayer player, PortalPlayerData data,
                                CompoundTag request, PortalGunLocator.LocatedGun gun) {
        PortalOpenCoordinator.requestPlayerTarget(player, data,
            PortalRequestFields.id(request, "Target"), true,
            data.settings().placementMode(), gun);
    }

    static boolean togglePin(PortalPlayerData data, CompoundTag request) {
        UUID playerId = PortalRequestFields.id(request, "Target");
        if (data.isPlayerPinned(playerId)) data.pinnedPlayers().remove(playerId);
        else data.pinnedPlayers().add(playerId);
        return true;
    }

    static boolean openSelected(ServerPlayer player, PortalPlayerData data,
                                PortalPlacementMode mode, PortalGunLocator.LocatedGun gun,
                                boolean fromGui) {
        UUID targetId = data.selectedPlayerId();
        if (targetId == null) return false;
        if (player.getServer() == null || !ServerPlayerRoster.isOnline(player.getServer(), targetId)) {
            if (fromGui) throw PortalRequestFields.error("message.riftgun.player_target_offline");
            player.displayClientMessage(Component.translatable("message.riftgun.player_target_offline"), true);
            return true;
        }
        PortalOpenCoordinator.requestPlayerTarget(player, data, targetId, fromGui, mode, gun);
        return true;
    }

    private PortalPlayerTargetActions() {}
}
