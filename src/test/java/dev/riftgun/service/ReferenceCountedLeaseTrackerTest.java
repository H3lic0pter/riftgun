package dev.riftgun.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ReferenceCountedLeaseTrackerTest {
    @Test
    void sharedLeaseReleasesUnderlyingResourceOnlyForTheLastOwner() {
        ReferenceCountedLeaseTracker<String> leases = new ReferenceCountedLeaseTracker<>();

        assertTrue(leases.acquire("same-chunk"));
        assertFalse(leases.acquire("same-chunk"));
        assertEquals(2, leases.count("same-chunk"));
        assertFalse(leases.release("same-chunk"));
        assertEquals(1, leases.count("same-chunk"));
        assertTrue(leases.release("same-chunk"));
        assertTrue(leases.isEmpty());
    }

    @Test
    void unmatchedReleaseFailsFastInsteadOfRemovingAnotherSearchTicket() {
        ReferenceCountedLeaseTracker<String> leases = new ReferenceCountedLeaseTracker<>();

        assertThrows(IllegalStateException.class, () -> leases.release("missing"));
    }
}
