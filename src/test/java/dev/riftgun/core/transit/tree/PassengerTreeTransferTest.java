package dev.riftgun.core.transit.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PassengerTreeTransferTest {
    @Test
    void transfersTheWholeTreeAndRestoresSeatOrder() {
        TestNode boat = new TestNode("boat");
        TestNode front = new TestNode("front");
        TestNode rear = new TestNode("rear");
        TestNode nested = new TestNode("nested");
        front.attachTo(boat);
        rear.attachTo(boat);
        nested.attachTo(front);

        PassengerTreeTransfer.Result<TestNode, String> result = PassengerTreeTransfer.transfer(
            PassengerTreeTransfer.capture(boat, TestNode.ACCESS),
            (node, role) -> node);

        assertEquals(PassengerTreeTransfer.Outcome.SUCCESS, result.outcome());
        assertEquals(List.of(front, rear), boat.passengers);
        assertEquals(List.of(nested), front.passengers);
        assertEquals(List.of("boat", "front", "nested", "rear"), result.moved().keySet().stream().toList());
        assertEquals(List.of("sync:boat:[front, rear]"), boat.events);
    }

    @Test
    void restoresTheOriginalTreeWhenTheRootCannotMove() {
        TestNode pig = new TestNode("pig");
        TestNode rider = new TestNode("rider");
        rider.attachTo(pig);

        PassengerTreeTransfer.Result<TestNode, String> result = PassengerTreeTransfer.transfer(
            PassengerTreeTransfer.capture(pig, TestNode.ACCESS),
            (node, role) -> null);

        assertEquals(PassengerTreeTransfer.Outcome.FAILED, result.outcome());
        assertNull(result.root());
        assertEquals(List.of(rider), pig.passengers);
        assertEquals(pig, rider.vehicle);
    }

    @Test
    void commitsTheRootAndReportsPartialWhenAPassengerCannotMove() {
        TestNode boat = new TestNode("boat");
        TestNode rider = new TestNode("rider");
        rider.attachTo(boat);

        PassengerTreeTransfer.Result<TestNode, String> result = PassengerTreeTransfer.transfer(
            PassengerTreeTransfer.capture(boat, TestNode.ACCESS),
            (node, role) -> role == PassengerTreeTransfer.NodeRole.ROOT ? node : null);

        assertEquals(PassengerTreeTransfer.Outcome.PARTIAL, result.outcome());
        assertEquals(boat, result.root());
        assertEquals(List.of("boat"), result.moved().keySet().stream().toList());
        assertEquals(new PassengerTreeTransfer.Failure<>("rider",
            PassengerTreeTransfer.Stage.TRANSFER), result.failures().getFirst());
        assertTrue(boat.passengers.isEmpty());
        assertNull(rider.vehicle);
    }

    private static final class TestNode {
        private static final PassengerTreeTransfer.Access<TestNode, String> ACCESS =
            new PassengerTreeTransfer.Access<>() {
                @Override
                public String identity(TestNode node) {
                    return node.name;
                }

                @Override
                public List<TestNode> passengers(TestNode node) {
                    return List.copyOf(node.passengers);
                }

                @Override
                public void detachPassengers(TestNode node) {
                    for (TestNode passenger : List.copyOf(node.passengers)) passenger.vehicle = null;
                    node.passengers.clear();
                }

                @Override
                public boolean attach(TestNode passenger, TestNode vehicle) {
                    passenger.attachTo(vehicle);
                    return true;
                }

                @Override
                public boolean synchronizeRoot(TestNode root) {
                    root.events.add("sync:" + root.name + ":"
                        + root.passengers.stream().map(node -> node.name).toList());
                    return true;
                }
            };

        private final String name;
        private final List<TestNode> passengers = new ArrayList<>();
        private final List<String> events = new ArrayList<>();
        private TestNode vehicle;

        private TestNode(String name) {
            this.name = name;
        }

        private void attachTo(TestNode parent) {
            vehicle = parent;
            parent.passengers.add(this);
        }
    }
}
