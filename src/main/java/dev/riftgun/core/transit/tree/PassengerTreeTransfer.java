package dev.riftgun.core.transit.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** Captures, validates and transfers a passenger tree without loader-specific types. */
public final class PassengerTreeTransfer {
    public static <N, K> Shape<K> captureShape(N root, Access<N, K> access) {
        return new Shape<>(captureNode(root, access));
    }

    public static <N, K> Captured<N, K> capture(N root, Access<N, K> access) {
        return bind(captureShape(root, access), root, access).orElseThrow();
    }

    public static <N, K> Optional<Captured<N, K>> bind(
            Shape<K> shape, N root, Access<N, K> access) {
        LinkedHashMap<K, N> nodes = new LinkedHashMap<>();
        BoundNode<N, K> bound = bindNode(shape.root, root, access, nodes);
        return bound == null ? Optional.empty()
            : Optional.of(new Captured<>(shape, bound, access, nodes));
    }

    public static <N, K> Result<N, K> transfer(
            Captured<N, K> tree, NodeTransfer<N> transfer) {
        detach(tree.root, tree.access);
        List<Failure<K>> failures = new ArrayList<>();
        LinkedHashMap<K, N> moved = new LinkedHashMap<>();

        N movedRoot = transfer.transfer(tree.root.node, NodeRole.ROOT);
        if (movedRoot == null) {
            restore(tree.root, tree.access);
            failures.add(new Failure<>(tree.root.identity, Stage.TRANSFER));
            return new Result<>(Outcome.FAILED, null, moved, failures);
        }
        moved.put(tree.root.identity, movedRoot);
        transferPassengers(tree.root, tree.access, transfer, moved, failures);
        remount(tree.root, tree.access, moved, failures);
        if (!tree.access.synchronizeRoot(movedRoot)) {
            failures.add(new Failure<>(tree.root.identity, Stage.SYNCHRONIZE));
        }
        return new Result<>(failures.isEmpty() ? Outcome.SUCCESS : Outcome.PARTIAL,
            movedRoot, moved, failures);
    }

    private static <N, K> ShapeNode<K> captureNode(N node, Access<N, K> access) {
        K identity = Objects.requireNonNull(access.identity(node), "passenger identity");
        List<ShapeNode<K>> passengers = access.passengers(node).stream()
            .map(passenger -> captureNode(passenger, access))
            .toList();
        return new ShapeNode<>(identity, passengers);
    }

    private static <N, K> @Nullable BoundNode<N, K> bindNode(
            ShapeNode<K> expected, N actual, Access<N, K> access,
            LinkedHashMap<K, N> nodes) {
        if (!expected.identity.equals(access.identity(actual))) return null;
        List<N> actualPassengers = access.passengers(actual);
        if (expected.passengers.size() != actualPassengers.size()
            || nodes.putIfAbsent(expected.identity, actual) != null) return null;
        List<BoundNode<N, K>> passengers = new ArrayList<>(actualPassengers.size());
        for (int index = 0; index < actualPassengers.size(); index++) {
            BoundNode<N, K> passenger = bindNode(expected.passengers.get(index),
                actualPassengers.get(index), access, nodes);
            if (passenger == null) return null;
            passengers.add(passenger);
        }
        return new BoundNode<>(expected.identity, actual, List.copyOf(passengers));
    }

    private static <N, K> void detach(BoundNode<N, K> node, Access<N, K> access) {
        access.detachPassengers(node.node);
        for (BoundNode<N, K> passenger : node.passengers) detach(passenger, access);
    }

    private static <N, K> void restore(BoundNode<N, K> node, Access<N, K> access) {
        for (BoundNode<N, K> passenger : node.passengers) {
            restore(passenger, access);
            access.attach(passenger.node, node.node);
        }
    }

    private static <N, K> void transferPassengers(
            BoundNode<N, K> parent, Access<N, K> access, NodeTransfer<N> transfer,
            LinkedHashMap<K, N> moved, List<Failure<K>> failures) {
        for (BoundNode<N, K> passenger : parent.passengers) {
            N movedPassenger = transfer.transfer(passenger.node, NodeRole.PASSENGER);
            if (movedPassenger == null) {
                failures.add(new Failure<>(passenger.identity, Stage.TRANSFER));
            } else {
                moved.put(passenger.identity, movedPassenger);
            }
            transferPassengers(passenger, access, transfer, moved, failures);
        }
    }

    private static <N, K> void remount(
            BoundNode<N, K> parent, Access<N, K> access, Map<K, N> moved,
            List<Failure<K>> failures) {
        N movedParent = moved.get(parent.identity);
        for (BoundNode<N, K> passenger : parent.passengers) {
            N movedPassenger = moved.get(passenger.identity);
            if (movedParent != null && movedPassenger != null
                && !access.attach(movedPassenger, movedParent)) {
                failures.add(new Failure<>(passenger.identity, Stage.REMOUNT));
            }
            remount(passenger, access, moved, failures);
        }
    }

    @FunctionalInterface
    public interface NodeTransfer<N> {
        @Nullable N transfer(N node, NodeRole role);
    }

    public interface Access<N, K> {
        K identity(N node);

        List<N> passengers(N node);

        void detachPassengers(N node);

        boolean attach(N passenger, N vehicle);

        boolean synchronizeRoot(N root);
    }

    public enum NodeRole {
        ROOT,
        PASSENGER
    }

    public enum Outcome {
        SUCCESS,
        PARTIAL,
        FAILED
    }

    public enum Stage {
        VALIDATION,
        TRANSFER,
        REMOUNT,
        SYNCHRONIZE
    }

    public record Failure<K>(K identity, Stage stage) {}

    public record Result<N, K>(Outcome outcome, @Nullable N root, Map<K, N> moved,
                               List<Failure<K>> failures) {
        public Result {
            moved = Collections.unmodifiableMap(new LinkedHashMap<>(moved));
            failures = List.copyOf(failures);
        }

        public List<N> movedNodes() {
            return List.copyOf(moved.values());
        }
    }

    public static final class Shape<K> {
        private final ShapeNode<K> root;
        private final List<K> identities;

        private Shape(ShapeNode<K> root) {
            this.root = root;
            List<K> collected = new ArrayList<>();
            collectIdentities(root, collected);
            this.identities = List.copyOf(collected);
        }

        public List<K> identities() {
            return identities;
        }

        public int size() {
            return identities.size();
        }

        private static <K> void collectIdentities(ShapeNode<K> node, List<K> identities) {
            identities.add(node.identity);
            for (ShapeNode<K> passenger : node.passengers) collectIdentities(passenger, identities);
        }
    }

    public static final class Captured<N, K> {
        private final Shape<K> shape;
        private final BoundNode<N, K> root;
        private final Access<N, K> access;
        private final Map<K, N> nodes;

        private Captured(Shape<K> shape, BoundNode<N, K> root, Access<N, K> access,
                         Map<K, N> nodes) {
            this.shape = shape;
            this.root = root;
            this.access = access;
            this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        }

        public Shape<K> shape() {
            return shape;
        }

        public List<N> nodes() {
            return List.copyOf(nodes.values());
        }

        public Optional<N> node(K identity) {
            return Optional.ofNullable(nodes.get(identity));
        }
    }

    private record ShapeNode<K>(K identity, List<ShapeNode<K>> passengers) {}

    private record BoundNode<N, K>(K identity, N node, List<BoundNode<N, K>> passengers) {}

    private PassengerTreeTransfer() {}
}
