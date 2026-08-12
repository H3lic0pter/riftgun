package dev.riftgun.relocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Owns concurrent target locks, per-gun fuel reservations and successful-target cooldowns. */
public final class EntityRelocationRegistry {
    private final int maximumPerGun;
    private final int targetCooldownTicks;
    private final Map<UUID, Reservation> activeByToken = new HashMap<>();
    private final Map<UUID, UUID> activeTokenByTarget = new HashMap<>();
    private final Map<UUID, Integer> activeCountByGun = new HashMap<>();
    private final Map<UUID, Integer> reservedFuelByGun = new HashMap<>();
    private final Map<UUID, Long> targetCooldownUntil = new HashMap<>();

    public EntityRelocationRegistry(int maximumPerGun, int targetCooldownTicks) {
        this.maximumPerGun = Math.max(1, maximumPerGun);
        this.targetCooldownTicks = Math.max(0, targetCooldownTicks);
    }

    public Begin begin(UUID gunId, UUID targetId, int reservedFuel, long now) {
        if (activeTokenByTarget.containsKey(targetId)) return Begin.rejected(BeginStatus.TARGET_BUSY);
        Long cooldownUntil = targetCooldownUntil.get(targetId);
        if (cooldownUntil != null) {
            if (now < cooldownUntil) return Begin.rejected(BeginStatus.TARGET_COOLDOWN);
            targetCooldownUntil.remove(targetId);
        }
        if (activeCountByGun.getOrDefault(gunId, 0) >= maximumPerGun) {
            return Begin.rejected(BeginStatus.GUN_CAPACITY);
        }

        Reservation reservation = new Reservation(
            UUID.randomUUID(), gunId, targetId, Math.max(0, reservedFuel));
        activeByToken.put(reservation.id(), reservation);
        activeTokenByTarget.put(targetId, reservation.id());
        activeCountByGun.merge(gunId, 1, Integer::sum);
        reservedFuelByGun.merge(gunId, reservation.reservedFuel(), Integer::sum);
        return new Begin(BeginStatus.ACCEPTED, reservation);
    }

    public void fail(Reservation reservation) {
        release(reservation);
    }

    public void complete(Reservation reservation, long now) {
        if (!release(reservation)) return;
        if (targetCooldownTicks > 0) {
            long until = now > Long.MAX_VALUE - targetCooldownTicks
                ? Long.MAX_VALUE : now + targetCooldownTicks;
            targetCooldownUntil.put(reservation.targetId(), until);
        }
    }

    public int reservedFuel(UUID gunId) {
        return reservedFuelByGun.getOrDefault(gunId, 0);
    }

    private boolean release(Reservation reservation) {
        Reservation active = activeByToken.remove(reservation.id());
        if (active == null || !active.equals(reservation)) return false;
        activeTokenByTarget.remove(active.targetId(), active.id());
        decrement(activeCountByGun, active.gunId(), 1);
        decrement(reservedFuelByGun, active.gunId(), active.reservedFuel());
        return true;
    }

    private static void decrement(Map<UUID, Integer> values, UUID key, int amount) {
        int remaining = values.getOrDefault(key, 0) - amount;
        if (remaining > 0) values.put(key, remaining);
        else values.remove(key);
    }

    public enum BeginStatus {
        ACCEPTED,
        GUN_CAPACITY,
        TARGET_BUSY,
        TARGET_COOLDOWN
    }

    public record Begin(BeginStatus status, @Nullable Reservation reservation) {
        static Begin rejected(BeginStatus status) {
            return new Begin(status, null);
        }
    }

    public record Reservation(UUID id, UUID gunId, UUID targetId, int reservedFuel) {}
}
