package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PortalTransitGateTest {
    @Test
    void entityRearmsOnlyAfterItCompletelyLeavesTheTrigger() {
        PortalTransitGate gate = new PortalTransitGate();
        UUID entityId = UUID.randomUUID();

        assertTrue(gate.enter(entityId, 0L, 20));
        gate.retainInside(Set.of(entityId), 1L, 20);
        assertFalse(gate.enter(entityId, 1L, 20));

        gate.retainInside(Set.of(), 20L, 20);
        assertTrue(gate.enter(entityId, 20L, 20));
    }

    @Test
    void arrivalRegistrationPreventsImmediateReturn() {
        PortalTransitGate exitGate = new PortalTransitGate();
        UUID entityId = UUID.randomUUID();

        exitGate.markInside(entityId, 5L, 20);

        assertTrue(exitGate.contains(entityId));
        assertFalse(exitGate.enter(entityId, 6L, 20));
    }

    @Test
    void failedTransferCanReleaseItsReservationForRetry() {
        PortalTransitGate gate = new PortalTransitGate();
        UUID entityId = UUID.randomUUID();

        assertTrue(gate.enter(entityId, 0L, 0));
        gate.leave(entityId);

        assertTrue(gate.enter(entityId, 0L, 0));
    }

    @Test
    void expiredCooldownEntriesAreRemoved() {
        PortalTransitGate gate = new PortalTransitGate();
        UUID entityId = UUID.randomUUID();
        assertTrue(gate.enter(entityId, 10L, 20));
        gate.retainInside(Set.of(), 29L, 20);
        assertEquals(1, gate.rememberedTransitCount());
        gate.retainInside(Set.of(), 30L, 20);
        assertEquals(0, gate.rememberedTransitCount());
    }
}
