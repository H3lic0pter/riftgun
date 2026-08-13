package dev.riftgun.relocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Temporarily prevents relocated non-player entities from triggering any normal portal exit. */
public final class EntityRelocationExitImmunity {
    private static final Map<UUID, Long> IMMUNITY_UNTIL = new HashMap<>();
    private static final PriorityQueue<Expiry> EXPIRIES = new PriorityQueue<>(
        Comparator.comparingLong(Expiry::until));

    public static void registerTree(Entity root, long now, int durationTicks) {
        if (durationTicks <= 0) return;
        root.getSelfAndPassengers()
            .filter(entity -> !(entity instanceof Player))
            .forEach(entity -> register(entity.getUUID(), now, durationTicks));
    }

    public static boolean blocksExit(Entity root) {
        long now = root.level().getGameTime();
        return root.getSelfAndPassengers()
            .anyMatch(entity -> remainingTicks(entity.getUUID(), now) > 0L);
    }

    public static long remainingTicks(Entity root) {
        long now = root.level().getGameTime();
        return root.getSelfAndPassengers()
            .mapToLong(entity -> remainingTicks(entity.getUUID(), now))
            .max().orElse(0L);
    }

    static void register(UUID entityId, long now, int durationTicks) {
        if (durationTicks <= 0) {
            IMMUNITY_UNTIL.remove(entityId);
            return;
        }
        long until = now > Long.MAX_VALUE - durationTicks
            ? Long.MAX_VALUE : now + durationTicks;
        IMMUNITY_UNTIL.put(entityId, until);
        EXPIRIES.add(new Expiry(entityId, until));
    }

    static boolean blocksAny(Iterable<UUID> entityIds, long now) {
        return remainingTicks(entityIds, now) > 0L;
    }

    static long remainingTicks(Iterable<UUID> entityIds, long now) {
        long maximum = 0L;
        for (UUID entityId : entityIds) {
            maximum = Math.max(maximum, remainingTicks(entityId, now));
        }
        return maximum;
    }

    private static long remainingTicks(UUID entityId, long now) {
        Long until = IMMUNITY_UNTIL.get(entityId);
        if (until == null) return 0L;
        if (now < until) return until - now;
        IMMUNITY_UNTIL.remove(entityId, until);
        return 0L;
    }

    public static void reset() {
        IMMUNITY_UNTIL.clear();
        EXPIRIES.clear();
    }

    public static void tick(long now) {
        while (!EXPIRIES.isEmpty() && now >= EXPIRIES.peek().until()) {
            Expiry expiry = EXPIRIES.remove();
            IMMUNITY_UNTIL.remove(expiry.entityId(), expiry.until());
        }
    }

    private record Expiry(UUID entityId, long until) {}

    private EntityRelocationExitImmunity() {}
}
