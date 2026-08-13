package dev.riftgun.relocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EntityRelocationCompletionPolicyTest {
    @Test
    void proceedsWhenTransactionDeadlineArrivesEvenIfExitVisualNeverTicks() {
        assertEquals(EntityRelocationCompletionPolicy.Decision.READY,
            EntityRelocationCompletionPolicy.decide(5, 5, 100, true, true));
    }

    @Test
    void waitsForOpeningAndChunkReadinessButCannotWaitForever() {
        assertEquals(EntityRelocationCompletionPolicy.Decision.WAITING,
            EntityRelocationCompletionPolicy.decide(4, 5, 100, true, true));
        assertEquals(EntityRelocationCompletionPolicy.Decision.WAITING,
            EntityRelocationCompletionPolicy.decide(20, 5, 100, true, false));
        assertEquals(EntityRelocationCompletionPolicy.Decision.TIMED_OUT,
            EntityRelocationCompletionPolicy.decide(100, 5, 100, true, false));
    }

    @Test
    void failsImmediatelyWhenExitDisappears() {
        assertEquals(EntityRelocationCompletionPolicy.Decision.FAILED,
            EntityRelocationCompletionPolicy.decide(5, 5, 100, false, true));
    }
}
