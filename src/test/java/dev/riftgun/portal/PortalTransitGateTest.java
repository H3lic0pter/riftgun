package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PortalTransitGateTest {
    @Test
    void entityRearmsOnlyAfterItCompletelyLeavesTheTrigger() {
        PortalTransitGate gate = new PortalTransitGate();
        UUID entityId = UUID.randomUUID();

        assertTrue(gate.enter(entityId));
        gate.retainInside(Set.of(entityId));
        assertFalse(gate.enter(entityId));

        gate.retainInside(Set.of());
        assertTrue(gate.enter(entityId));
    }

    @Test
    void arrivalRegistrationPreventsImmediateReturn() {
        PortalTransitGate exitGate = new PortalTransitGate();
        UUID entityId = UUID.randomUUID();

        exitGate.markInside(entityId);

        assertTrue(exitGate.contains(entityId));
        assertFalse(exitGate.enter(entityId));
    }
}
