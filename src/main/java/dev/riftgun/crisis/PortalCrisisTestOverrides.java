package dev.riftgun.crisis;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;

/** Server-memory overrides used by operator commands to reproduce one crisis on the next transit. */
public final class PortalCrisisTestOverrides {
    private static final Map<UUID, ResourceLocation> FORCED = new ConcurrentHashMap<>();

    public static Optional<ResourceLocation> forced(UUID playerId) {
        return Optional.ofNullable(FORCED.get(playerId));
    }

    public static Optional<ResourceLocation> force(UUID playerId, ResourceLocation crisisId) {
        return Optional.ofNullable(FORCED.put(playerId, crisisId));
    }

    public static Optional<ResourceLocation> clear(UUID playerId) {
        return Optional.ofNullable(FORCED.remove(playerId));
    }

    public static boolean consume(UUID playerId, ResourceLocation expectedCrisisId) {
        return FORCED.remove(playerId, expectedCrisisId);
    }

    public static void reset() {
        FORCED.clear();
    }

    private PortalCrisisTestOverrides() {}
}
