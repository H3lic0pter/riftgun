package dev.riftgun.core.fuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PreparedFluidTransferTest {
    @Test
    void successfulTransferPreservesQuantityAcrossBothSides() {
        FakeSource basin = new FakeSource(1000, 1000);
        FakeContainer gun = new FakeContainer(8000, false);

        PreparedFluidTransfer.Outcome<String> result =
            PreparedFluidTransfer.execute(basin, gun, 1000);

        assertTrue(result.success());
        assertEquals("portal_fluid", result.resource());
        assertEquals(1000, result.amount());
        assertEquals(0, basin.amount);
        assertEquals(1000, gun.amount);
    }

    @Test
    void rejectedContainerExecutionLeavesTheSourceUntouched() {
        FakeSource basin = new FakeSource(1000, 1000);
        FakeContainer gun = new FakeContainer(8000, true);

        PreparedFluidTransfer.Outcome<String> result =
            PreparedFluidTransfer.execute(basin, gun, 1000);

        assertFalse(result.success());
        assertEquals(1000, basin.amount);
        assertEquals(0, gun.amount);
    }

    @Test
    void partialSourceDrainTrimsThePreparedContainerToTheExactAmount() {
        FakeSource basin = new FakeSource(1000, 600);
        FakeContainer gun = new FakeContainer(8000, false);

        PreparedFluidTransfer.Outcome<String> result =
            PreparedFluidTransfer.execute(basin, gun, 1000);

        assertTrue(result.success());
        assertEquals(600, result.amount());
        assertEquals(400, basin.amount);
        assertEquals(600, gun.amount);
    }

    @Test
    void simulationRejectionNeverMutatesEitherSide() {
        FakeSource basin = new FakeSource(1000, 1000);
        FakeContainer gun = new FakeContainer(0, false);

        PreparedFluidTransfer.Outcome<String> result =
            PreparedFluidTransfer.execute(basin, gun, 1000);

        assertFalse(result.success());
        assertEquals(1000, basin.amount);
        assertEquals(0, gun.amount);
    }

    @Test
    void partialContainerExecutionNeverDrainsTheSource() {
        FakeSource basin = new FakeSource(1000, 1000);
        FakeContainer gun = new FakeContainer(8000, 500);

        PreparedFluidTransfer.Outcome<String> result =
            PreparedFluidTransfer.execute(basin, gun, 1000);

        assertFalse(result.success());
        assertEquals(1000, basin.amount);
        assertEquals(500, gun.amount);
    }

    @Test
    void failedSourceExecutionNeverReturnsACommittableContainer() {
        FakeSource basin = new FakeSource(1000, 0);
        FakeContainer gun = new FakeContainer(8000, false);

        PreparedFluidTransfer.Outcome<String> result =
            PreparedFluidTransfer.execute(basin, gun, 1000);

        assertFalse(result.success());
        assertEquals(1000, basin.amount);
        assertEquals(1000, gun.amount);
    }

    private static final class FakeSource implements PreparedFluidTransfer.Source<String> {
        private int amount;
        private final int executeLimit;

        FakeSource(int amount, int executeLimit) {
            this.amount = amount;
            this.executeLimit = executeLimit;
        }

        @Override
        public PreparedFluidTransfer.Offer<String> simulate(int maximum) {
            int offered = Math.min(amount, maximum);
            return offered > 0
                ? new PreparedFluidTransfer.Offer<>("portal_fluid", offered)
                : PreparedFluidTransfer.Offer.empty();
        }

        @Override
        public int drain(String resource, int maximum) {
            int drained = Math.min(Math.min(amount, maximum), executeLimit);
            amount -= drained;
            return drained;
        }
    }

    private static final class FakeContainer implements PreparedFluidTransfer.Container<String> {
        private final int capacity;
        private final int executeLimit;
        private int amount;

        FakeContainer(int capacity, boolean rejectExecution) {
            this(capacity, rejectExecution ? 0 : Integer.MAX_VALUE);
        }

        FakeContainer(int capacity, int executeLimit) {
            this.capacity = capacity;
            this.executeLimit = executeLimit;
        }

        @Override
        public int simulateFill(String resource, int maximum) {
            return Math.min(maximum, capacity - amount);
        }

        @Override
        public int fill(String resource, int maximum) {
            int filled = Math.min(simulateFill(resource, maximum), executeLimit);
            amount += filled;
            return filled;
        }

        @Override
        public int drain(String resource, int maximum) {
            int drained = Math.min(amount, maximum);
            amount -= drained;
            return drained;
        }
    }
}
