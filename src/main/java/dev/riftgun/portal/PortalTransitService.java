package dev.riftgun.portal;

import dev.riftgun.core.transit.tree.PassengerTreeTransfer;
import dev.riftgun.core.transit.tree.MinecraftEntityTreeAccess;
import dev.riftgun.service.PortalServices;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
    private static final PassengerTreeTransfer.Access<Entity, UUID> ENTITY_TREE =
        MinecraftEntityTreeAccess.INSTANCE;

    static @Nullable Entity teleportTree(Entity root, SingleEntityTransit transit,
                                         Consumer<Entity> movedEntity,
                                         BiConsumer<Entity, FailureStage> failedEntity) {
        PassengerTreeTransfer.Captured<Entity, UUID> tree =
            PassengerTreeTransfer.capture(root, ENTITY_TREE);
        PassengerTreeTransfer.Result<Entity, UUID> result = PassengerTreeTransfer.transfer(
            tree, (entity, role) -> transit.teleport(
                entity, role == PassengerTreeTransfer.NodeRole.PASSENGER));
        result.movedNodes().forEach(movedEntity);
        for (PassengerTreeTransfer.Failure<UUID> failure : result.failures()) {
            Entity failed = result.moved().get(failure.identity());
            if (failed == null) failed = tree.node(failure.identity()).orElse(root);
            failedEntity.accept(failed, failureStage(failure.stage()));
        }
        return result.root();
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

    private static FailureStage failureStage(PassengerTreeTransfer.Stage stage) {
        return switch (stage) {
            case VALIDATION, TRANSFER -> FailureStage.TRANSIT;
            case REMOUNT -> FailureStage.REMOUNT;
            case SYNCHRONIZE -> FailureStage.SYNCHRONIZE;
        };
    }

    @FunctionalInterface
    interface SingleEntityTransit {
        @Nullable Entity teleport(Entity entity, boolean mountedTransit);
    }

    enum FailureStage {
        PREFLIGHT_CLEARANCE,
        TRANSIT,
        REMOUNT,
        SYNCHRONIZE
    }

    record TransitPlan(Vec3 destination, Vec3 momentum, float yaw, float pitch) {}

    private PortalTransitService() {}
}
