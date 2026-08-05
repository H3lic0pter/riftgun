package dev.riftgun.service;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Transient, server-side movement samples used only by opt-in prediction. */
public interface PortalMotionHistory {
    Optional<Vec3> recentVelocity(ServerPlayer player);

    void setPredictionEnabled(ServerPlayer player, boolean enabled);

    void tick(MinecraftServer server);

    void reset(ServerPlayer player);

    void remove(UUID playerId);
}
