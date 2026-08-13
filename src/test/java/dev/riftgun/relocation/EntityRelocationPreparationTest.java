package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class EntityRelocationPreparationTest {
    @Test
    void waitsWithoutStartingAnimationAndReleasesTicketWhenReady() {
        AtomicInteger releases = new AtomicInteger();
        EntityRelocationPreparation preparation = new EntityRelocationPreparation(
            100L, 100, releases::incrementAndGet);

        assertEquals(EntityRelocationPreparation.Outcome.WAITING,
            preparation.advance(101L, false));
        assertFalse(preparation.terminal());
        assertEquals(0, releases.get());

        assertEquals(EntityRelocationPreparation.Outcome.READY,
            preparation.advance(102L, true));
        assertTrue(preparation.terminal());
        assertEquals(1, releases.get());
    }

    @Test
    void timesOutAtTheConfiguredDeadlineAndReleasesTicketOnce() {
        AtomicInteger releases = new AtomicInteger();
        EntityRelocationPreparation preparation = new EntityRelocationPreparation(
            40L, 100, releases::incrementAndGet);

        assertEquals(EntityRelocationPreparation.Outcome.WAITING,
            preparation.advance(139L, false));
        assertEquals(EntityRelocationPreparation.Outcome.TIMED_OUT,
            preparation.advance(140L, false));
        preparation.close();

        assertTrue(preparation.terminal());
        assertEquals(1, releases.get());
    }

    @Test
    void preparingMessageIsDelayedAndShownOnlyOnce() {
        EntityRelocationPreparation preparation = new EntityRelocationPreparation(
            100L, 100, () -> {});

        assertFalse(preparation.shouldShowPreparingMessage(109L));
        assertFalse(preparation.preparingMessageShown());
        assertTrue(preparation.shouldShowPreparingMessage(110L));
        assertTrue(preparation.preparingMessageShown());
        assertFalse(preparation.shouldShowPreparingMessage(111L));
    }
}
