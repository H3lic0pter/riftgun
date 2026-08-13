package dev.riftgun.portal;

import dev.riftgun.service.PortalServices;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Shared passenger-tree and post-teleport pipeline for normal and bootstrap routes. */
final class PortalTransitService {
    private static final TreeAccess<Entity> ENTITY_TREE = new TreeAccess<>() {
        @Override
        public java.util.List<Entity> passengers(Entity entity) {
            return entity.getPassengers();
        }

        @Override
        public void ejectPassengers(Entity entity) {
            entity.ejectPassengers();
        }

        @Override
        public boolean startRiding(Entity passenger, Entity vehicle) {
            return passenger.startRiding(vehicle, true);
        }

        @Override
        public void synchronizeRoot(Entity root) {
            if (root.getControllingPassenger() instanceof ServerPlayer player) {
                player.connection.send(new ClientboundMoveVehiclePacket(root));
            }
        }
    };

    static @Nullable Entity teleportTree(Entity root, SingleEntityTransit transit,
                                         Consumer<Entity> movedEntity,
                                         BiConsumer<Entity, FailureStage> failedEntity) {
        TreeSnapshot<Entity> snapshot = snapshot(root, ENTITY_TREE);
        detach(snapshot, ENTITY_TREE);
        Entity movedRoot = teleportSnapshot(
            snapshot, false, ENTITY_TREE, transit::teleport, movedEntity, failedEntity);
        if (movedRoot != null) ENTITY_TREE.synchronizeRoot(movedRoot);
        return movedRoot;
    }

    static @Nullable Entity complete(Entity entity, ServerLevel targetLevel,
                                     TransitPlan plan, boolean playerFallGuard,
                                     boolean entityFallGuard) {
        Entity moved;
        if (entity.level() == targetLevel) {
            boolean successful = entity.teleportTo(targetLevel,
                plan.destination().x, plan.destination().y, plan.destination().z,
                Set.<RelativeMovement>of(), plan.yaw(), plan.pitch());
            moved = successful ? entity : null;
        } else {
            moved = entity.changeDimension(new DimensionTransition(targetLevel, plan.destination(),
                plan.momentum(), plan.yaw(), plan.pitch(), DimensionTransition.DO_NOTHING));
        }
        if (moved == null) return null;

        moved.setDeltaMovement(plan.momentum());
        moved.hasImpulse = true;
        if (moved instanceof Projectile projectile) {
            ProjectileMotion.alignToVelocity(projectile, plan.momentum());
        }
        if (PortalFallGuardPolicy.applies(moved, playerFallGuard, entityFallGuard)) {
            moved.fallDistance = 0.0F;
        }
        if (moved instanceof ServerPlayer player) PortalServices.MOTION_HISTORY.reset(player);
        return moved;
    }

    static TransitPlan mountedPlan(TransitPlan rootPlan, float passengerYaw, float passengerPitch) {
        return new TransitPlan(rootPlan.destination(), rootPlan.momentum(),
            passengerYaw, passengerPitch);
    }

    static <T> @Nullable T teleportTree(T root, TreeAccess<T> access,
                                        SingleNodeTransit<T> transit,
                                        Consumer<T> movedEntity,
                                        Consumer<T> failedEntity) {
        TreeSnapshot<T> snapshot = snapshot(root, access);
        detach(snapshot, access);
        T movedRoot = teleportSnapshot(snapshot, false, access, transit, movedEntity,
            (node, ignored) -> failedEntity.accept(node));
        if (movedRoot != null) access.synchronizeRoot(movedRoot);
        return movedRoot;
    }

    private static <T> TreeSnapshot<T> snapshot(T root, TreeAccess<T> access) {
        java.util.List<TreeSnapshot<T>> passengers = access.passengers(root).stream()
            .map(passenger -> snapshot(passenger, access))
            .toList();
        return new TreeSnapshot<>(root, passengers);
    }

    private static <T> void detach(TreeSnapshot<T> snapshot, TreeAccess<T> access) {
        access.ejectPassengers(snapshot.entity());
        for (TreeSnapshot<T> passenger : snapshot.passengers()) detach(passenger, access);
    }

    private static <T> @Nullable T teleportSnapshot(
        TreeSnapshot<T> snapshot, boolean mountedTransit, TreeAccess<T> access,
        SingleNodeTransit<T> transit, Consumer<T> movedEntity,
        BiConsumer<T, FailureStage> failedEntity
    ) {
        T movedRoot = transit.teleport(snapshot.entity(), mountedTransit);
        if (movedRoot == null) {
            restore(snapshot, access);
            failedEntity.accept(snapshot.entity(), FailureStage.TRANSIT);
            return null;
        }
        movedEntity.accept(movedRoot);
        for (TreeSnapshot<T> passenger : snapshot.passengers()) {
            T movedPassenger = teleportSnapshot(
                passenger, true, access, transit, movedEntity, failedEntity);
            if (movedPassenger != null && !access.startRiding(movedPassenger, movedRoot)) {
                failedEntity.accept(movedPassenger, FailureStage.REMOUNT);
            }
        }
        return movedRoot;
    }

    private static <T> void restore(TreeSnapshot<T> snapshot, TreeAccess<T> access) {
        for (TreeSnapshot<T> passenger : snapshot.passengers()) {
            restore(passenger, access);
            access.startRiding(passenger.entity(), snapshot.entity());
        }
    }

    @FunctionalInterface
    interface SingleEntityTransit {
        @Nullable Entity teleport(Entity entity, boolean mountedTransit);
    }

    @FunctionalInterface
    interface SingleNodeTransit<T> {
        @Nullable T teleport(T node, boolean mountedTransit);
    }

    interface TreeAccess<T> {
        java.util.List<T> passengers(T node);

        void ejectPassengers(T node);

        boolean startRiding(T passenger, T vehicle);

        void synchronizeRoot(T root);
    }

    enum FailureStage {
        PREFLIGHT_CLEARANCE,
        TRANSIT,
        REMOUNT
    }

    private record TreeSnapshot<T>(T entity, java.util.List<TreeSnapshot<T>> passengers) {}

    record TransitPlan(Vec3 destination, Vec3 momentum, float yaw, float pitch) {}

    private PortalTransitService() {}
}
