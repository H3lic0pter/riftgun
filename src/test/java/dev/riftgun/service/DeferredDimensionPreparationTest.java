package dev.riftgun.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DeferredDimensionPreparationTest {
    @Test
    void eightQueuedRequestsStartOnePerTickInOrder() {
        var requests = new ArrayList<DeferredDimensionPreparation>();
        for (int i = 0; i < 8; i++) requests.add(new DeferredDimensionPreparation());
        var started = new ArrayList<Integer>();
        for (int tick = 0; tick < 8; tick++) {
            var budget = new DimensionGenerationBudget();
            for (int i = 0; i < requests.size(); i++) {
                int id = i;
                requests.get(i).tick(() -> started.contains(id), () -> false, budget,
                    () -> started.add(id));
            }
            assertEquals(tick + 1, started.size());
        }
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), started);
    }

    @Test
    void installedDimensionDoesNotClaimBudgetOrChargeKey() {
        var budget = new DimensionGenerationBudget();
        var preparation = new DeferredDimensionPreparation();
        assertTrue(preparation.tick(() -> true, () -> false, budget,
            () -> fail("Existing dimension must not consume a key")));
        assertTrue(budget.tryAcquire());
    }

    @Test
    void generationAndKeyConsumptionHappenOnceWhileInstallationIsPending() {
        var preparation = new DeferredDimensionPreparation();
        var keysConsumed = new AtomicInteger();
        for (int tick = 0; tick < 200; tick++) {
            assertFalse(preparation.tick(() -> false, () -> false,
                new DimensionGenerationBudget(), keysConsumed::incrementAndGet));
        }
        assertEquals(1, keysConsumed.get());
        var error = assertThrows(IllegalArgumentException.class, () -> preparation.tick(
            () -> false, () -> false, new DimensionGenerationBudget(), keysConsumed::incrementAndGet));
        assertEquals("message.riftgun.dimension_unavailable", error.getMessage());
        assertEquals(1, keysConsumed.get());
    }

    @Test
    void anotherPlayersPendingDimensionNeverRegeneratesEvenIfPendingFlagDisappears() {
        var preparation = new DeferredDimensionPreparation();
        assertFalse(preparation.tick(() -> false, () -> true, new DimensionGenerationBudget(),
            () -> fail("Duplicate generation")));
        assertFalse(preparation.tick(() -> false, () -> false, new DimensionGenerationBudget(),
            () -> fail("Disappearing upstream queue must not cause another charge")));
        assertTrue(preparation.tick(() -> true, () -> false, new DimensionGenerationBudget(),
            () -> fail("Installed dimension")));
    }

    @Test
    void failedGenerationDoesNotPermitAnotherStartInTheSameTick() {
        var budget = new DimensionGenerationBudget();
        assertThrows(IllegalStateException.class, () -> new DeferredDimensionPreparation().tick(
            () -> false, () -> false, budget, () -> { throw new IllegalStateException("failed"); }));
        assertFalse(budget.tryAcquire());
    }

    @Test
    void serversHaveIndependentBudgets() {
        assertTrue(new DimensionGenerationBudget().tryAcquire());
        assertTrue(new DimensionGenerationBudget().tryAcquire());
    }
}
