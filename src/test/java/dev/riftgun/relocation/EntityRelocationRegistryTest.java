package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EntityRelocationRegistryTest {
    @Test
    void limitsEachGunToEightConcurrentTargetsAndReservesFuel() {
        EntityRelocationRegistry registry = new EntityRelocationRegistry(8, 10);
        UUID gun = UUID.randomUUID();
        var reservations = new ArrayList<EntityRelocationRegistry.Reservation>();

        for (int index = 0; index < 8; index++) {
            EntityRelocationRegistry.Begin result = registry.begin(
                gun, UUID.randomUUID(), 8, 100L);
            assertEquals(EntityRelocationRegistry.BeginStatus.ACCEPTED, result.status());
            reservations.add(result.reservation());
        }

        assertEquals(64, registry.reservedFuel(gun));
        assertEquals(EntityRelocationRegistry.BeginStatus.GUN_CAPACITY,
            registry.begin(gun, UUID.randomUUID(), 8, 100L).status());
        registry.fail(reservations.getFirst());
        assertEquals(56, registry.reservedFuel(gun));
        assertEquals(EntityRelocationRegistry.BeginStatus.ACCEPTED,
            registry.begin(gun, UUID.randomUUID(), 8, 100L).status());
    }

    @Test
    void targetCannotBeDuplicatedAndSuccessfulTransitStartsCooldown() {
        EntityRelocationRegistry registry = new EntityRelocationRegistry(8, 10);
        UUID target = UUID.randomUUID();
        EntityRelocationRegistry.Reservation reservation = registry.begin(
            UUID.randomUUID(), target, 5, 20L).reservation();

        assertEquals(EntityRelocationRegistry.BeginStatus.TARGET_BUSY,
            registry.begin(UUID.randomUUID(), target, 5, 20L).status());
        registry.complete(reservation, 20L);
        assertEquals(EntityRelocationRegistry.BeginStatus.TARGET_COOLDOWN,
            registry.begin(UUID.randomUUID(), target, 5, 29L).status());
        assertEquals(EntityRelocationRegistry.BeginStatus.ACCEPTED,
            registry.begin(UUID.randomUUID(), target, 5, 30L).status());
        assertTrue(registry.reservedFuel(reservation.gunId()) == 0);
    }

    @Test
    void passengerTreeLocksAndCoolsDownEveryMember() {
        EntityRelocationRegistry registry = new EntityRelocationRegistry(8, 10);
        UUID root = UUID.randomUUID();
        UUID passenger = UUID.randomUUID();
        EntityRelocationRegistry.Reservation reservation = registry.begin(
            UUID.randomUUID(), List.of(root, passenger), 5, 20L).reservation();

        assertEquals(EntityRelocationRegistry.BeginStatus.TARGET_BUSY,
            registry.begin(UUID.randomUUID(), passenger, 5, 20L).status());
        registry.complete(reservation, 20L);
        assertEquals(EntityRelocationRegistry.BeginStatus.TARGET_COOLDOWN,
            registry.begin(UUID.randomUUID(), passenger, 5, 29L).status());
    }
}
