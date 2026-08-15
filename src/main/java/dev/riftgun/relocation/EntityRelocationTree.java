package dev.riftgun.relocation;

import dev.riftgun.core.transit.tree.PassengerTreeTransfer;
import dev.riftgun.core.transit.tree.MinecraftEntityTreeAccess;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
//? if >=1.21.11 {
/*import net.minecraft.world.entity.Relative;
*///?} else {
import net.minecraft.world.entity.RelativeMovement;
//?}
//? if >=1.21.11 {
/*import net.minecraft.world.level.portal.TeleportTransition;
*///?} else {
import net.minecraft.world.level.portal.DimensionTransition;
//?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Captured relocation topology plus geometry derived from its current live binding. */
final class EntityRelocationTree {
    private static final PassengerTreeTransfer.Access<Entity, UUID> ACCESS =
        MinecraftEntityTreeAccess.INSTANCE;

    private final PassengerTreeTransfer.Shape<UUID> shape;

    private EntityRelocationTree(PassengerTreeTransfer.Shape<UUID> shape) {
        this.shape = shape;
    }

    static EntityRelocationTree capture(Entity root) {
        return new EntityRelocationTree(PassengerTreeTransfer.captureShape(root, ACCESS));
    }

    List<UUID> memberIds() {
        return shape.identities();
    }

    int size() {
        return shape.size();
    }

    boolean matches(Entity candidateRoot) {
        return bind(candidateRoot) != null;
    }

    List<Entity> members(Entity candidateRoot) {
        PassengerTreeTransfer.Captured<Entity, UUID> bound = bind(candidateRoot);
        return bound == null ? List.of() : bound.nodes();
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
        PassengerTreeTransfer.Captured<Entity, UUID> bound = bind(candidateRoot);
        if (bound == null) return Transfer.failed();
        PassengerTreeTransfer.Result<Entity, UUID> result = PassengerTreeTransfer.transfer(
            bound, (entity, role) -> teleport(entity, targetLevel, rootPosition,
                role == PassengerTreeTransfer.NodeRole.ROOT ? rootMomentum : Vec3.ZERO));
        return new Transfer(result.root(), result.movedNodes(), result.outcome(), result.failures());
    }

    private @Nullable PassengerTreeTransfer.Captured<Entity, UUID> bind(Entity candidateRoot) {
        return PassengerTreeTransfer.bind(shape, candidateRoot, ACCESS).orElse(null);
    }

    private static @Nullable Entity teleport(Entity entity, ServerLevel targetLevel,
                                              Vec3 position, Vec3 momentum) {
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        if (entity.level() == targetLevel) {
            boolean ok = entity.teleportTo(targetLevel, position.x, position.y, position.z,
//? if >=1.21.11 {
                /*Set.<Relative>of(), yaw, pitch, false);
*///?} else {
                Set.<RelativeMovement>of(), yaw, pitch);
//?}
            if (!ok) return null;
            entity.setDeltaMovement(momentum);
            return entity;
        }
//? if >=1.21.11 {
        /*return entity.changeDimension(new TeleportTransition(
*///?} else {
        return entity.changeDimension(new DimensionTransition(
//?}
//? if >=1.21.11 {
            /*targetLevel, position, momentum, yaw, pitch, TeleportTransition.DO_NOTHING));
*///?} else {
            targetLevel, position, momentum, yaw, pitch, DimensionTransition.DO_NOTHING));
//?}
    }

    static Entity promotedRoot(Entity entity) {
        Entity root = entity;
        while (root.getVehicle() != null) root = root.getVehicle();
        return root;
    }

    record Transfer(@Nullable Entity root, List<Entity> members,
                    PassengerTreeTransfer.Outcome outcome,
                    List<PassengerTreeTransfer.Failure<UUID>> failures) {
        static Transfer failed() {
            return new Transfer(null, List.of(), PassengerTreeTransfer.Outcome.FAILED, List.of());
        }

        boolean partial() {
            return outcome == PassengerTreeTransfer.Outcome.PARTIAL;
        }
    }

    record Metrics(double width, double height, double depth,
                   double offsetX, double offsetY, double offsetZ) {}
}
