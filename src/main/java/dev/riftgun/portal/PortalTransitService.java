package dev.riftgun.portal;

import dev.riftgun.service.PortalServices;
import java.util.ArrayList;
import java.util.Set;
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
    static @Nullable Entity teleportTree(Entity root, SingleEntityTransit transit,
                                         Consumer<Entity> movedEntity,
                                         Consumer<Entity> failedEntity) {
        return teleportTree(root, false, transit, movedEntity, failedEntity);
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

    private static @Nullable Entity teleportTree(Entity root, boolean mountedTransit,
                                                  SingleEntityTransit transit,
                                                  Consumer<Entity> movedEntity,
                                                  Consumer<Entity> failedEntity) {
        var passengers = new ArrayList<>(root.getPassengers());
        root.ejectPassengers();
        Entity movedRoot = transit.teleport(root, mountedTransit);
        if (movedRoot == null) {
            for (Entity passenger : passengers) passenger.startRiding(root, true);
            failedEntity.accept(root);
            return null;
        }
        movedEntity.accept(movedRoot);
        for (Entity passenger : passengers) {
            Entity movedPassenger = teleportTree(passenger, true, transit, movedEntity, failedEntity);
            if (movedPassenger != null) movedPassenger.startRiding(movedRoot, true);
        }
        return movedRoot;
    }

    @FunctionalInterface
    interface SingleEntityTransit {
        @Nullable Entity teleport(Entity entity, boolean mountedTransit);
    }

    record TransitPlan(Vec3 destination, Vec3 momentum, float yaw, float pitch) {}

    private PortalTransitService() {}
}
