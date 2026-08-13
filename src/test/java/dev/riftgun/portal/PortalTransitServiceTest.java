package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class PortalTransitServiceTest {
    @Test
    void detachesTheWholeTreeBeforeCommittingTheRootAndRestoresSeatOrder() {
        TestNode root = new TestNode("boat");
        TestNode first = new TestNode("front");
        TestNode second = new TestNode("rear");
        TestNode nested = new TestNode("nested");
        first.attachTo(root);
        second.attachTo(root);
        nested.attachTo(first);
        List<TestNode> originals = List.of(root, first, second, nested);
        List<String> transitOrder = new ArrayList<>();

        TestNode movedRoot = PortalTransitService.teleportTree(root, TestNode.ACCESS,
            (node, mounted) -> {
                if (transitOrder.isEmpty()) {
                    assertTrue(originals.stream().allMatch(candidate -> candidate.vehicle == null));
                    assertTrue(originals.stream().allMatch(candidate -> candidate.passengers.isEmpty()));
                }
                transitOrder.add(node.name);
                return node;
            }, ignored -> {}, ignored -> {});

        assertEquals(root, movedRoot);
        assertEquals(List.of("boat", "front", "nested", "rear"), transitOrder);
        assertEquals(List.of(first, second), root.passengers);
        assertEquals(List.of(nested), first.passengers);
        assertFalse(root.vehicle != null);
    }

    @Test
    void synchronizesTheRootOnlyAfterEveryPassengerIsRemounted() {
        TestNode root = new TestNode("pig");
        TestNode rider = new TestNode("player");
        rider.attachTo(root);

        PortalTransitService.teleportTree(root, TestNode.ACCESS,
            (node, mounted) -> node, ignored -> {}, ignored -> {});

        assertEquals(List.of("sync:pig:[player]"), root.events);
    }

    @Test
    void mountedNodesReuseTheRootTransitPlan() {
        PortalTransitService.TransitPlan rootPlan = new PortalTransitService.TransitPlan(
            new net.minecraft.world.phys.Vec3(4.0, 5.0, 6.0),
            new net.minecraft.world.phys.Vec3(0.5, 0.0, 1.5), 90.0F, 10.0F);

        PortalTransitService.TransitPlan mounted = PortalTransitService.mountedPlan(
            rootPlan, 25.0F, -30.0F);

        assertEquals(rootPlan.destination(), mounted.destination());
        assertEquals(rootPlan.momentum(), mounted.momentum());
        assertEquals(25.0F, mounted.yaw());
        assertEquals(-30.0F, mounted.pitch());
    }

    @Test
    void passengerFailureNeverRollsBackAnAlreadyCommittedRoot() {
        TestNode root = new TestNode("boat");
        TestNode passenger = new TestNode("player");
        passenger.attachTo(root);
        List<String> moved = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        TestNode movedRoot = PortalTransitService.teleportTree(root, TestNode.ACCESS,
            (node, mounted) -> {
                if (mounted) return null;
                moved.add(node.name);
                return node;
            }, ignored -> {}, node -> failed.add(node.name));

        assertEquals(root, movedRoot);
        assertEquals(List.of("boat"), moved);
        assertEquals(List.of("player"), failed);
        assertTrue(root.passengers.isEmpty());
        assertEquals(null, passenger.vehicle);
    }

    private static final class TestNode {
        private static final PortalTransitService.TreeAccess<TestNode> ACCESS =
            new PortalTransitService.TreeAccess<>() {
                @Override
                public List<TestNode> passengers(TestNode node) {
                    return List.copyOf(node.passengers);
                }

                @Override
                public void ejectPassengers(TestNode node) {
                    for (TestNode passenger : List.copyOf(node.passengers)) {
                        passenger.vehicle = null;
                    }
                    node.passengers.clear();
                }

                @Override
                public boolean startRiding(TestNode passenger, TestNode vehicle) {
                    passenger.attachTo(vehicle);
                    return true;
                }

                @Override
                public void synchronizeRoot(TestNode root) {
                    root.events.add("sync:" + root.name + ":"
                        + root.passengers.stream().map(node -> node.name).toList());
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
