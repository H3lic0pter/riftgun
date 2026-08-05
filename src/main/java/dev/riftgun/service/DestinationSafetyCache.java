package dev.riftgun.service;

import dev.riftgun.data.Destination;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Short-lived, position-keyed safety cache. Browsing never asks the chunk system to load data. */
public final class DestinationSafetyCache {
    private static final long TTL_TICKS = 100L;
    private static final long RATE_LIMIT_TICKS = 6L;
    private static final Map<UUID, Map<DestinationSafetyFingerprint, Entry>> CACHE = new HashMap<>();
    private static final Map<UUID, Long> NEXT_CHECK = new HashMap<>();

    public static Lookup inspectLoaded(ServerPlayer player, ServerLevel level, Destination destination) {
        long now = serverTime(player);
        DestinationSafetyFingerprint fingerprint = DestinationSafetyFingerprint.of(destination);
        SafetyReport cached = cached(player.getUUID(), fingerprint, now);
        if (cached != null) return Lookup.available(cached);

        if (!safetyAreaLoaded(level, destination)) {
            return Lookup.unloaded();
        }
        long next = NEXT_CHECK.getOrDefault(player.getUUID(), 0L);
        if (now < next) return Lookup.rateLimited();
        NEXT_CHECK.put(player.getUUID(), now + RATE_LIMIT_TICKS);
        return Lookup.available(inspectAndStore(player, level, destination, fingerprint, now));
    }

    private static boolean safetyAreaLoaded(ServerLevel level, Destination destination) {
        int minChunkX = ((int) Math.floor(destination.x() - 0.31)) >> 4;
        int maxChunkX = ((int) Math.floor(destination.x() + 0.31)) >> 4;
        int minChunkZ = ((int) Math.floor(destination.z() - 0.31)) >> 4;
        int maxChunkZ = ((int) Math.floor(destination.z() + 0.31)) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return false;
            }
        }
        return true;
    }

    public static SafetyReport inspectPrepared(ServerPlayer player, ServerLevel level, Destination destination) {
        long now = serverTime(player);
        DestinationSafetyFingerprint fingerprint = DestinationSafetyFingerprint.of(destination);
        SafetyReport cached = cached(player.getUUID(), fingerprint, now);
        return cached != null ? cached : inspectAndStore(player, level, destination, fingerprint, now);
    }

    public static void clear(UUID playerId) {
        CACHE.remove(playerId);
        NEXT_CHECK.remove(playerId);
    }

    private static SafetyReport inspectAndStore(ServerPlayer player, ServerLevel level, Destination destination,
                                                DestinationSafetyFingerprint fingerprint, long now) {
        SafetyReport report = PortalServices.SAFETY_INSPECTOR.inspect(level, destination);
        CACHE.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
            .put(fingerprint, new Entry(report, now + TTL_TICKS));
        return report;
    }

    private static SafetyReport cached(UUID playerId, DestinationSafetyFingerprint fingerprint, long now) {
        Map<DestinationSafetyFingerprint, Entry> playerCache = CACHE.get(playerId);
        if (playerCache == null) return null;
        Entry entry = playerCache.get(fingerprint);
        if (entry == null || now >= entry.expiresAt) {
            playerCache.remove(fingerprint);
            return null;
        }
        return entry.report;
    }

    private static long serverTime(ServerPlayer player) {
        return player.getServer() == null ? 0L : player.getServer().overworld().getGameTime();
    }

    public record Lookup(Status status, SafetyReport report) {
        public static Lookup available(SafetyReport report) {
            return new Lookup(Status.AVAILABLE, report);
        }

        public static Lookup unloaded() {
            return new Lookup(Status.UNLOADED, SafetyReport.SAFE);
        }

        public static Lookup rateLimited() {
            return new Lookup(Status.RATE_LIMITED, SafetyReport.SAFE);
        }
    }

    public enum Status {
        AVAILABLE,
        UNLOADED,
        RATE_LIMITED
    }

    private record Entry(SafetyReport report, long expiresAt) {}

    private DestinationSafetyCache() {}
}
