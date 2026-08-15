package dev.riftgun.crisis;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

/** Server-memory overrides used by operator commands to reproduce one crisis on the next transit. */
public final class PortalCrisisTestOverrides {
//? if >=1.21.11 {
    /*private static final Map<UUID, Identifier> FORCED = new ConcurrentHashMap<>();
*///?} else {
    private static final Map<UUID, ResourceLocation> FORCED = new ConcurrentHashMap<>();
//?}

//? if >=1.21.11 {
    /*public static Optional<Identifier> forced(UUID playerId) {
*///?} else {
    public static Optional<ResourceLocation> forced(UUID playerId) {
//?}
        return Optional.ofNullable(FORCED.get(playerId));
    }

//? if >=1.21.11 {
    /*public static Optional<Identifier> force(UUID playerId, Identifier crisisId) {
*///?} else {
    public static Optional<ResourceLocation> force(UUID playerId, ResourceLocation crisisId) {
//?}
        return Optional.ofNullable(FORCED.put(playerId, crisisId));
    }

//? if >=1.21.11 {
    /*public static Optional<Identifier> clear(UUID playerId) {
*///?} else {
    public static Optional<ResourceLocation> clear(UUID playerId) {
//?}
        return Optional.ofNullable(FORCED.remove(playerId));
    }

//? if >=1.21.11 {
    /*public static boolean consume(UUID playerId, Identifier expectedCrisisId) {
*///?} else {
    public static boolean consume(UUID playerId, ResourceLocation expectedCrisisId) {
//?}
        return FORCED.remove(playerId, expectedCrisisId);
    }

    public static void reset() {
        FORCED.clear();
    }

    private PortalCrisisTestOverrides() {}
}
