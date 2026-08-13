package dev.riftgun.relocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Immutable passenger-tree identity plus its detach, transfer and best-effort remount operation. */
final class EntityRelocationTree {
    private final Node root;
    private final List<UUID> memberIds;

    private EntityRelocationTree(Node root) {
        this.root = root;
        List<UUID> ids = new ArrayList<>();
        collectIds(root, ids);
        this.memberIds = List.copyOf(ids);
    }

    static EntityRelocationTree capture(Entity root) {
        return new EntityRelocationTree(captureNode(root));
    }

    List<UUID> memberIds() {
        return memberIds;
    }

    int size() {
        return memberIds.size();
    }

    boolean matches(Entity candidateRoot) {
        return matches(root, candidateRoot);
    }

    List<Entity> members(Entity candidateRoot) {
        if (!matches(candidateRoot)) return List.of();
        List<Entity> members = new ArrayList<>(size());
        collectEntities(candidateRoot, members);
        return List.copyOf(members);
    }

    AABB envelope(Entity candidateRoot) {
        List<Entity> members = members(candidateRoot);
        if (members.isEmpty()) return candidateRoot.getBoundingBox();
        AABB result = members.getFirst().getBoundingBox();
        for (int index = 1; index < members.size(); index++) {
            result = result.minmax(members.get(index).getBoundingBox());
        }
        return result;
    }

    Metrics metrics(Entity candidateRoot) {
        AABB bounds = envelope(candidateRoot);
        return new Metrics(bounds.getXsize(), bounds.getYsize(), bounds.getZsize(),
            bounds.minX - candidateRoot.getX(), bounds.minY - candidateRoot.getY(),
            bounds.minZ - candidateRoot.getZ());
    }

    AABB destinationEnvelope(Entity candidateRoot, Vec3 rootPosition) {
        Metrics metrics = metrics(candidateRoot);
        return new AABB(rootPosition.x + metrics.offsetX(), rootPosition.y + metrics.offsetY(),
            rootPosition.z + metrics.offsetZ(),
            rootPosition.x + metrics.offsetX() + metrics.width(),
            rootPosition.y + metrics.offsetY() + metrics.height(),
            rootPosition.z + metrics.offsetZ() + metrics.depth());
    }

    Transfer transfer(Entity candidateRoot, ServerLevel targetLevel,
                      Vec3 rootPosition, Vec3 rootMomentum) {
        if (!matches(candidateRoot)) return Transfer.failed(candidateRoot);
        Map<UUID, Entity> originals = new HashMap<>();
        collectById(candidateRoot, originals);
        detach(candidateRoot);
        Map<UUID, Entity> results = new HashMap<>();
        boolean[] complete = {true};
        Entity movedRoot = transferNode(root, originals, results, targetLevel,
            rootPosition, rootMomentum, true, complete);
        remount(root, originals, results, complete);
        if (movedRoot != null && movedRoot.getControllingPassenger() instanceof ServerPlayer player) {
            player.connection.send(new ClientboundMoveVehiclePacket(movedRoot));
        }
        List<Entity> movedMembers = memberIds.stream().map(results::get)
            .filter(java.util.Objects::nonNull).toList();
        return new Transfer(movedRoot, movedMembers, complete[0]);
    }

    private static @Nullable Entity transferNode(
            Node node, Map<UUID, Entity> originals, Map<UUID, Entity> results,
            ServerLevel targetLevel, Vec3 rootPosition, Vec3 rootMomentum,
            boolean rootNode, boolean[] complete) {
        Entity original = originals.get(node.id());
        if (original == null) {
            complete[0] = false;
            return null;
        }
        Entity moved = teleport(original, targetLevel, rootPosition,
            rootNode ? rootMomentum : Vec3.ZERO);
        if (moved == null) {
            complete[0] = false;
            results.put(node.id(), original);
        } else {
            results.put(node.id(), moved);
        }
        for (Node passenger : node.passengers()) {
            transferNode(passenger, originals, results, targetLevel,
                rootPosition, Vec3.ZERO, false, complete);
        }
        return moved;
    }

    private static void remount(Node parent, Map<UUID, Entity> originals,
                                Map<UUID, Entity> results, boolean[] complete) {
        Entity movedParent = results.getOrDefault(parent.id(), originals.get(parent.id()));
        for (Node child : parent.passengers()) {
            Entity movedChild = results.getOrDefault(child.id(), originals.get(child.id()));
            if (movedParent == null || movedChild == null
                || movedParent.level() != movedChild.level()
                || !movedChild.startRiding(movedParent, true)) {
                complete[0] = false;
            }
            remount(child, originals, results, complete);
        }
    }

    private static @Nullable Entity teleport(Entity entity, ServerLevel targetLevel,
                                              Vec3 position, Vec3 momentum) {
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        if (entity.level() == targetLevel) {
            boolean ok = entity.teleportTo(targetLevel, position.x, position.y, position.z,
                Set.<RelativeMovement>of(), yaw, pitch);
            if (!ok) return null;
            entity.setDeltaMovement(momentum);
            return entity;
        }
        return entity.changeDimension(new DimensionTransition(
            targetLevel, position, momentum, yaw, pitch, DimensionTransition.DO_NOTHING));
    }

    private static Entity rootOf(Entity entity) {
        Entity root = entity;
        while (root.getVehicle() != null) root = root.getVehicle();
        return root;
    }

    static Entity promotedRoot(Entity entity) {
        return rootOf(entity);
    }

    private static Node captureNode(Entity entity) {
        return new Node(entity.getUUID(), entity.getPassengers().stream()
            .map(EntityRelocationTree::captureNode).toList());
    }

    private static boolean matches(Node expected, Entity actual) {
        if (!expected.id().equals(actual.getUUID())
            || expected.passengers().size() != actual.getPassengers().size()) return false;
        for (int index = 0; index < expected.passengers().size(); index++) {
            if (!matches(expected.passengers().get(index), actual.getPassengers().get(index))) return false;
        }
        return true;
    }

    private static void collectIds(Node node, List<UUID> ids) {
        ids.add(node.id());
        node.passengers().forEach(passenger -> collectIds(passenger, ids));
    }

    private static void collectEntities(Entity entity, List<Entity> entities) {
        entities.add(entity);
        entity.getPassengers().forEach(passenger -> collectEntities(passenger, entities));
    }

    private static void collectById(Entity entity, Map<UUID, Entity> entities) {
        entities.put(entity.getUUID(), entity);
        entity.getPassengers().forEach(passenger -> collectById(passenger, entities));
    }

    private static void detach(Entity entity) {
        List<Entity> passengers = List.copyOf(entity.getPassengers());
        entity.ejectPassengers();
        passengers.forEach(EntityRelocationTree::detach);
    }

    record Transfer(@Nullable Entity root, List<Entity> members, boolean complete) {
        static Transfer failed(Entity root) { return new Transfer(root, List.of(root), false); }
    }

    record Metrics(double width, double height, double depth,
                   double offsetX, double offsetY, double offsetZ) {}

    private record Node(UUID id, List<Node> passengers) {}
}
