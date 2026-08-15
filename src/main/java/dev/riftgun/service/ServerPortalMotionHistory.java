package dev.riftgun.service;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.runtime.RiftRuntime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Samples only players who explicitly enable prediction; disabled players incur no inventory scan. */
public final class ServerPortalMotionHistory implements PortalMotionHistory {
    static final int GUN_RECHECK_TICKS = 20;

    private final Map<UUID, Track> tracks = new HashMap<>();

    @Override
    public Optional<Vec3> recentVelocity(ServerPlayer player) {
        Track track = tracks.get(player.getUUID());
        return track == null || !track.hasGun ? Optional.empty() : track.window.estimatedVelocity();
    }

    @Override
    public void setPredictionEnabled(ServerPlayer player, boolean enabled) {
        if (enabled) tracks.computeIfAbsent(player.getUUID(), ignored -> new Track());
        else tracks.remove(player.getUUID());
    }

    @Override
    public void tick(MinecraftServer server) {
        var iterator = tracks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Track> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            Track track = entry.getValue();
            if (player.isSpectator() || !player.isAlive()) {
                track.window.clear();
                track.hasGun = false;
                track.recheckTicks = 0;
                continue;
            }
            if (track.recheckTicks-- <= 0) {
                track.recheckTicks = GUN_RECHECK_TICKS - 1;
                boolean hasGun = PortalGunLocator.anyHasPortalGun(player);
                if (!hasGun) track.window.clear();
                track.hasGun = hasGun;
            }
            if (track.hasGun) {
                track.window.record(player.level().dimension(), player.position(),
                    server.overworld().getGameTime(),
                    RiftRuntime.current().placementCapabilities().motionHistoryTeleportThreshold(player));
            }
        }
    }

    @Override
    public void reset(ServerPlayer player) {
        Track track = tracks.get(player.getUUID());
        if (track != null) track.window.clear();
    }

    @Override
    public void remove(UUID playerId) {
        tracks.remove(playerId);
    }

    int trackedPlayers() {
        return tracks.size();
    }

    private static final class Track {
        private final RecentMotionWindow window = new RecentMotionWindow();
        private int recheckTicks;
        private boolean hasGun;
    }
}
