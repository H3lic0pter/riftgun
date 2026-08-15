package dev.riftgun.network;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.data.PlayerPermissionProfileMode;
import dev.riftgun.data.PortalPermissionPolicy;
import dev.riftgun.service.PortalPrivacyService;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Handles Privacy Terminal actions, which intentionally do not require carrying a Portal Gun. */
final class PortalPrivacyRequestHandler {
    static void handle(ServerPlayer player, PortalAction action, CompoundTag request) {
        switch (action) {
            case SET_PRIVACY -> {
//? if >=1.21.11 {
                /*Identifier permission = Identifier.tryParse(Nbt.getString(request, "Permission"));
*///?} else {
                ResourceLocation permission = ResourceLocation.tryParse(Nbt.getString(request, "Permission"));
//?}
                if (permission == null) return;
                PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                    Nbt.getString(request, "Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                PortalPrivacyService.applyGlobalPermission(player, permission, policy);
                PortalNetworking.sendPrivacyTerminal(player);
            }
            case SET_PRIVACY_OVERRIDE -> {
                if (!Nbt.hasUUID(request, "Target")) return;
                if (request.contains("ProfileMode")) {
                    PlayerPermissionProfileMode mode = PlayerPermissionProfileMode.parse(
                        Nbt.getString(request, "ProfileMode"), PlayerPermissionProfileMode.FOLLOW_GLOBAL);
                    PortalPrivacyService.applyProfileMode(player, Nbt.getUUID(request, "Target"), mode);
                } else {
//? if >=1.21.11 {
                    /*Identifier permission = Identifier.tryParse(
*///?} else {
                    ResourceLocation permission = ResourceLocation.tryParse(
//?}
                        Nbt.getString(request, "Permission"));
                    if (permission == null) return;
                    PortalPermissionPolicy policy = PortalPermissionPolicy.parse(
                        Nbt.getString(request, "Policy"), PortalPermissionPolicy.FOLLOW_GLOBAL);
                    PortalPrivacyService.applyPermissionOverride(
                        player, Nbt.getUUID(request, "Target"), permission, policy);
                }
                PortalNetworking.sendPrivacyTerminal(player);
            }
            case REQUEST_PRIVACY_PLAYERS -> PortalNetworking.sendPrivacyPlayers(player);
            default -> throw new IllegalArgumentException("Not a privacy action: " + action);
        }
    }

    private PortalPrivacyRequestHandler() {}
}
