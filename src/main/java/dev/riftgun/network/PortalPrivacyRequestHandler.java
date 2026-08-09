package dev.riftgun.network;

import dev.riftgun.data.PlayerPermissionOverride;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.TargetPrivacy;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Handles Privacy Terminal actions, which intentionally do not require carrying a Portal Gun. */
final class PortalPrivacyRequestHandler {
    static void handle(ServerPlayer player, PortalAction action, CompoundTag request) {
        PortalPlayerData data = PortalDataStore.load(player);
        switch (action) {
            case SET_PRIVACY -> {
                data.targetPrivacy(TargetPrivacy.parse(request.getString("Privacy"), TargetPrivacy.PUBLIC));
                data.transitPrivacyEnabled(request.getBoolean("TransitPrivacy"));
                PortalDataStore.save(player, data);
                PortalNetworking.sendPrivacyTerminal(player);
            }
            case SET_PRIVACY_OVERRIDE -> {
                if (!request.hasUUID("Target")) return;
                PlayerPermissionOverride mode = PlayerPermissionOverride.parse(
                    request.getString("Mode"), PlayerPermissionOverride.DEFAULT);
                data.privacyOverride(request.getUUID("Target"), mode);
                PortalDataStore.save(player, data);
                PortalNetworking.sendPrivacyTerminal(player);
            }
            case REQUEST_PRIVACY_PLAYERS -> PortalNetworking.sendPrivacyPlayers(player);
            default -> throw new IllegalArgumentException("Not a privacy action: " + action);
        }
    }

    private PortalPrivacyRequestHandler() {}
}
