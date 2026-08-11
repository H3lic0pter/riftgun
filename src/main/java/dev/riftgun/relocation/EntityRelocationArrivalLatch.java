package dev.riftgun.relocation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Prevents a normal exit portal materialized around a relocation arrival from immediately
 * returning that entity. The latch clears only after the captured transit cooldown has elapsed
 * and the entity has fully left its arrival volume at least once.
 */
public final class EntityRelocationArrivalLatch {
    static final int MAX_LIFETIME_TICKS = 5 * 60 * 20;
    private static final Map<UUID, Guard> GUARDS = new HashMap<>();

    public static void register(Entity entity, Vec3 landing, Vec3 exitCenter,
                                float portalSide, long now, int cooldownTicks) {
        GUARDS.put(entity.getUUID(), new Guard(entity.level().dimension(),
            guardVolume(landing, exitCenter, portalSide), cooldownUntil(now, cooldownTicks),
            expiresAt(now), false));
    }

    /** Returns true only while some member of the entity tree remains inside its arrival volume. */
    public static boolean blocksExit(Entity root) {
        long now = root.level().getGameTime();
        return root.getSelfAndPassengers().anyMatch(entity -> blocksSingle(entity, now));
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 20L != 0L || GUARDS.isEmpty()) return;
        Iterator<Map.Entry<UUID, Guard>> iterator = GUARDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Guard> entry = iterator.next();
            Guard guard = entry.getValue();
            if (now >= guard.expiresAt()) {
                iterator.remove();
                continue;
            }
            ServerLevel level = server.getLevel(guard.dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (entity == null) {
                iterator.remove();
                continue;
            }
            if (!guard.volume().intersects(entity.getBoundingBox())) guard = guard.afterLeaving();
            if (shouldBlock(guard.leftVolume(), now, guard.cooldownUntil())) entry.setValue(guard);
            else iterator.remove();
        }
    }

    public static void reset() {
        GUARDS.clear();
    }

    static AABB guardVolume(Vec3 landing, Vec3 exitCenter, float portalSide) {
        double half = Math.max(0.5, portalSide * 0.5) + 1.0;
        double minY = Math.min(landing.y, exitCenter.y) - 0.25;
        double maxY = Math.max(landing.y, exitCenter.y) + 0.25;
        return new AABB(exitCenter.x - half, minY, exitCenter.z - half,
            exitCenter.x + half, maxY, exitCenter.z + half);
    }

    private static boolean blocksSingle(Entity entity, long now) {
        Guard guard = GUARDS.get(entity.getUUID());
        if (guard == null) return false;
        if (now >= guard.expiresAt() || !entity.level().dimension().equals(guard.dimension())) {
            GUARDS.remove(entity.getUUID(), guard);
            return false;
        }
        if (!guard.volume().intersects(entity.getBoundingBox())) {
            Guard departed = guard.afterLeaving();
            GUARDS.replace(entity.getUUID(), guard, departed);
            guard = departed;
        }
        if (shouldBlock(guard.leftVolume(), now, guard.cooldownUntil())) return true;
        GUARDS.remove(entity.getUUID(), guard);
        return false;
    }

    static boolean shouldBlock(boolean leftVolume, long now, long cooldownUntil) {
        return !leftVolume || now < cooldownUntil;
    }

    private static long cooldownUntil(long now, int cooldownTicks) {
        int duration = Math.max(0, cooldownTicks);
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    private static long expiresAt(long now) {
        return now > Long.MAX_VALUE - MAX_LIFETIME_TICKS
            ? Long.MAX_VALUE : now + MAX_LIFETIME_TICKS;
    }

    private record Guard(ResourceKey<Level> dimension, AABB volume, long cooldownUntil,
                         long expiresAt, boolean leftVolume) {
        Guard afterLeaving() {
            return leftVolume ? this
                : new Guard(dimension, volume, cooldownUntil, expiresAt, true);
        }
    }

    private EntityRelocationArrivalLatch() {}
}
