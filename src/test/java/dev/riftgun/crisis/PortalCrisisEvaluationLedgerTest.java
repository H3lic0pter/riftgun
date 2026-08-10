package dev.riftgun.crisis;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortalCrisisEvaluationLedgerTest {
    @Test
    void capacityEvictsLeastRecentlyUsedPlayer() {
        PortalCrisisEvaluationLedger ledger = new PortalCrisisEvaluationLedger();
        UUID first = UUID.randomUUID();
        UUID leastRecent = UUID.randomUUID();
        UUID newcomer = UUID.randomUUID();

        assertTrue(ledger.reserve(first, null, 2));
        assertTrue(ledger.reserve(leastRecent, null, 2));
        assertFalse(ledger.reserve(first, null, 2), "Returning players are not evaluated twice while retained");
        assertTrue(ledger.reserve(newcomer, null, 2));

        assertTrue(ledger.reserve(leastRecent, null, 2), "An evicted player may be evaluated again");
    }

    @Test
    void linkedPortalsShareOneEvaluationAndOneLruOrder() {
        PortalCrisisEvaluationLedger entry = new PortalCrisisEvaluationLedger();
        PortalCrisisEvaluationLedger exit = new PortalCrisisEvaluationLedger();
        UUID first = UUID.randomUUID();
        UUID leastRecent = UUID.randomUUID();
        UUID newcomer = UUID.randomUUID();

        assertTrue(entry.reserve(first, exit, 2));
        assertTrue(exit.reserve(leastRecent, entry, 2));
        assertFalse(exit.reserve(first, entry, 2));
        assertTrue(entry.reserve(newcomer, exit, 2));

        assertTrue(exit.reserve(leastRecent, entry, 2));
        assertFalse(entry.reserve(leastRecent, exit, 2));
    }

    @Test
    void nbtRoundTripPreservesLeastRecentlyUsedOrder() {
        PortalCrisisEvaluationLedger original = new PortalCrisisEvaluationLedger();
        UUID recent = UUID.randomUUID();
        UUID leastRecent = UUID.randomUUID();
        UUID newcomer = UUID.randomUUID();
        original.reserve(recent, null, 2);
        original.reserve(leastRecent, null, 2);
        original.reserve(recent, null, 2);

        CompoundTag saved = original.save();
        PortalCrisisEvaluationLedger restored = new PortalCrisisEvaluationLedger();
        restored.load(saved, 2);

        assertTrue(restored.reserve(newcomer, null, 2));
        assertTrue(restored.reserve(leastRecent, null, 2),
            "The least recently used player before save must be evicted first after load");
    }

    @Test
    void deferredExitCopyPreservesLeastRecentlyUsedOrder() {
        PortalCrisisEvaluationLedger source = new PortalCrisisEvaluationLedger();
        UUID recent = UUID.randomUUID();
        UUID leastRecent = UUID.randomUUID();
        UUID newcomer = UUID.randomUUID();
        source.reserve(recent, null, 2);
        source.reserve(leastRecent, null, 2);
        source.reserve(recent, null, 2);

        PortalCrisisEvaluationLedger copied = new PortalCrisisEvaluationLedger();
        copied.copyFrom(source, 2);

        assertTrue(copied.reserve(newcomer, null, 2));
        assertTrue(copied.reserve(leastRecent, null, 2));
    }
}
