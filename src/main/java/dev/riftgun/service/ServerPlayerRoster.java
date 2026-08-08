package dev.riftgun.service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Read-only view of the online player roster used by the Player target category. */
public final class ServerPlayerRoster {
    public static Set<UUID> onlinePlayerIds(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
            .map(ServerPlayer::getUUID)
            .collect(Collectors.toSet());
    }

    public static boolean isOnline(MinecraftServer server, UUID playerId) {
        return server.getPlayerList().getPlayer(playerId) != null;
    }

    public static @org.jetbrains.annotations.Nullable ServerPlayer onlinePlayer(MinecraftServer server, UUID playerId) {
        return server.getPlayerList().getPlayer(playerId);
    }

    private ServerPlayerRoster() {}
}
