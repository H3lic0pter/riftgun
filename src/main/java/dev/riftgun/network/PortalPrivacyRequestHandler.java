package dev.riftgun.network;

import dev.riftgun.data.PlayerPermissionProfileMode;
import dev.riftgun.data.PortalPermissionPolicy;
import dev.riftgun.service.PortalPrivacyService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Handles Privacy Terminal actions, which intentionally do not require carrying a Portal Gun. */
final class PortalPrivacyRequestHandler {
    static void handle(ServerPlayer player, PortalAction action, CompoundTag request) {
        switch (action) {
            case SET_PRIVACY -> {
                ResourceLocation permission = ResourceLocation.tryParse(request.getString("Permission"));
                if (permission == null) return;
                PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                    request.getString("Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                PortalPrivacyService.applyGlobalPermission(player, permission, policy);
                PortalNetworking.sendPrivacyTerminal(player);
            }
            case SET_PRIVACY_OVERRIDE -> {
                if (!request.hasUUID("Target")) return;
                if (request.contains("ProfileMode")) {
                    PlayerPermissionProfileMode mode = PlayerPermissionProfileMode.parse(
                        request.getString("ProfileMode"), PlayerPermissionProfileMode.FOLLOW_GLOBAL);
                    PortalPrivacyService.applyProfileMode(player, request.getUUID("Target"), mode);
                } else {
                    ResourceLocation permission = ResourceLocation.tryParse(
                        request.getString("Permission"));
                    if (permission == null) return;
                    PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                        request.getString("Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                    PortalPrivacyService.applyPermissionOverride(
                        player, request.getUUID("Target"), permission, policy);
                }
                PortalNetworking.sendPrivacyTerminal(player);
            }
            case REQUEST_PRIVACY_PLAYERS -> PortalNetworking.sendPrivacyPlayers(player);
            default -> throw new IllegalArgumentException("Not a privacy action: " + action);
        }
    }

    private PortalPrivacyRequestHandler() {}
}
