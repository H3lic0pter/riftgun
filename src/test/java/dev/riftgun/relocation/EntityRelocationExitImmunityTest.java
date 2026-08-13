package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class EntityRelocationExitImmunityTest {
    @AfterEach
    void resetImmunity() {
        EntityRelocationExitImmunity.reset();
    }

    @Test
    void blocksAnyExitUntilTheConfiguredDeadline() {
        UUID entity = UUID.randomUUID();
        EntityRelocationExitImmunity.register(entity, 20L, 100);

        assertTrue(EntityRelocationExitImmunity.blocksAny(List.of(entity), 119L));
        assertEquals(1L, EntityRelocationExitImmunity.remainingTicks(List.of(entity), 119L));
        assertFalse(EntityRelocationExitImmunity.blocksAny(List.of(entity), 120L));
    }

    @Test
    void aLaterRelocationRefreshesRatherThanAccumulatesTheDeadline() {
        UUID entity = UUID.randomUUID();
        EntityRelocationExitImmunity.register(entity, 20L, 100);
        EntityRelocationExitImmunity.register(entity, 80L, 100);

        assertTrue(EntityRelocationExitImmunity.blocksAny(List.of(entity), 179L));
        assertFalse(EntityRelocationExitImmunity.blocksAny(List.of(entity), 180L));
    }

    @Test
    void oneProtectedPassengerBlocksTheWholeTree() {
        UUID vehicle = UUID.randomUUID();
        UUID passenger = UUID.randomUUID();
        EntityRelocationExitImmunity.register(passenger, 20L, 100);

        assertTrue(EntityRelocationExitImmunity.blocksAny(
            List.of(vehicle, passenger), 50L));
    }

    @Test
    void zeroDurationDisablesImmunity() {
        UUID entity = UUID.randomUUID();
        EntityRelocationExitImmunity.register(entity, 20L, 0);

        assertFalse(EntityRelocationExitImmunity.blocksAny(List.of(entity), 20L));
    }

    @Test
    void expiryCleanupDoesNotRemoveARefreshedDeadline() {
        UUID entity = UUID.randomUUID();
        EntityRelocationExitImmunity.register(entity, 20L, 100);
        EntityRelocationExitImmunity.register(entity, 80L, 100);

        EntityRelocationExitImmunity.tick(120L);

        assertTrue(EntityRelocationExitImmunity.blocksAny(List.of(entity), 179L));
    }
}
