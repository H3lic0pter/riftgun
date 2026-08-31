package dev.riftgun.network;
import dev.riftgun.core.msg.Msg;

import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalOpenCoordinator;
import dev.riftgun.service.PortalOpenOrigin;
import dev.riftgun.service.ServerPlayerRoster;
import dev.riftgun.service.PrecisionPlacementIntent;
import dev.riftgun.service.SurfaceFaceSelection;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Player Target selection, roster and portal actions. */
final class PortalPlayerTargetActions {
    static boolean select(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        UUID playerId = PortalRequestFields.id(request, "Target");
//? if >=1.21.11 {
        /*if (player.level().getServer() == null || !ServerPlayerRoster.isOnline(player.level().getServer(), playerId)) {
*///?} else {
        if (player.getServer() == null || !ServerPlayerRoster.isOnline(player.getServer(), playerId)) {
//?}
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
//? if >=1.21.11 {
        /*data.prunePlayerTargets(ServerPlayerRoster.onlinePlayerIds(player.level().getServer()));
*///?} else {
        data.prunePlayerTargets(ServerPlayerRoster.onlinePlayerIds(player.getServer()));
//?}
        PortalDataStore.save(player, data);
        PortalNetworking.sendPlayerList(player);
    }

    static void openFromRequest(ServerPlayer player, PortalPlayerData data,
                                CompoundTag request, PortalGunLocator.LocatedGun gun) {
        PortalOpenCoordinator.requestPlayerTarget(player, data,
            PortalRequestFields.id(request, "Target"), true,
            PortalOpenOrigin.GUI.resolvePlacement(data.settings().placementMode()), gun);
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
//? if >=1.21.11 {
        /*if (player.level().getServer() == null || !ServerPlayerRoster.isOnline(player.level().getServer(), targetId)) {
*///?} else {
        if (player.getServer() == null || !ServerPlayerRoster.isOnline(player.getServer(), targetId)) {
//?}
            if (fromGui) throw PortalRequestFields.error("message.riftgun.player_target_offline");
            Msg.displayClientMessage(player, Component.translatable("message.riftgun.player_target_offline"), true);
            return true;
        }
        PortalOpenOrigin origin = fromGui ? PortalOpenOrigin.GUI : PortalOpenOrigin.ITEM;
        PortalOpenCoordinator.requestPlayerTarget(player, data, targetId, fromGui,
            origin.resolvePlacement(mode), gun);
        return true;
    }

    static boolean openSelectedSurfaceFace(ServerPlayer player, PortalPlayerData data,
                                           PortalPlacementMode mode,
                                           PortalGunLocator.LocatedGun gun,
                                           SurfaceFaceSelection selection) {
        UUID targetId = data.selectedPlayerId();
        if (targetId == null) return false;
//? if >=1.21.11 {
        /*if (player.level().getServer() == null || !ServerPlayerRoster.isOnline(player.level().getServer(), targetId)) {
*///?} else {
        if (player.getServer() == null || !ServerPlayerRoster.isOnline(player.getServer(), targetId)) {
//?}
            Msg.displayClientMessage(player,
                Component.translatable("message.riftgun.player_target_offline"), true);
            return true;
        }
        PortalOpenCoordinator.requestPlayerTargetSurfaceFace(
            player, data, targetId, mode, gun, selection);
        return true;
    }

    static boolean openSelectedPrecision(ServerPlayer player, PortalPlayerData data,
                                         PortalPlacementMode mode,
                                         PortalGunLocator.LocatedGun gun,
                                         PrecisionPlacementIntent intent) {
        UUID targetId = data.selectedPlayerId();
        if (targetId == null) return false;
//? if >=1.21.11 {
        /*if (player.level().getServer() == null || !ServerPlayerRoster.isOnline(player.level().getServer(), targetId)) {
*///?} else {
        if (player.getServer() == null || !ServerPlayerRoster.isOnline(player.getServer(), targetId)) {
//?}
            Msg.displayClientMessage(player,
                Component.translatable("message.riftgun.player_target_offline"), true);
            return true;
        }
        PortalOpenCoordinator.requestPlayerTargetPrecision(
            player, data, targetId, mode, gun, intent);
        return true;
    }

    private PortalPlayerTargetActions() {}
}
