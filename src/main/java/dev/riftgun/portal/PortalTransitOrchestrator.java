package dev.riftgun.portal;

import dev.riftgun.sound.PortalSounds;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Routes portal contact through normal, deferred and crisis-return transit pipelines. */
final class PortalTransitOrchestrator {
    private static final float FACING_THRESHOLD = 0.35F;

    private final PortalEntity portal;
    private final PortalTransitGate gate = new PortalTransitGate();

    PortalTransitOrchestrator(PortalEntity portal) {
        this.portal = portal;
    }

    void tick() {
        long now = portal.serverTime();
        AABB search = portal.placement().bounds().inflate(0.6, 2.0, 0.6);
        boolean crisisExit = portal.crisis().isReturnExit();
        PortalTransitEligibility eligibility = new PortalTransitEligibility(
            portal.placement(), portal.entityAccess(), portal.ownerId(),
            crisisExit ? null : portal.excludedPlayerId(),
            crisisExit ? false : portal.isExitPortal(), portal.horizontalTriggerExtend());
        List<Entity> touching = portal.level().getEntities(portal, search, entity ->
            eligibility.allows(entity) && (!crisisExit || entity instanceof ServerPlayer
                && portal.crisis().allowsReturn(entity)));
        Set<UUID> touchingIds = new HashSet<>(touching.size());
        for (Entity entity : touching) touchingIds.add(entity.getUUID());
        gate.retainInside(touchingIds, now, portal.transitCooldownTicks());

        if (crisisExit) {
            for (Entity entity : touching) {
                if (!gate.enter(entity.getUUID(), now, portal.transitCooldownTicks())) continue;
                portal.crisis().transitReturn((ServerPlayer) entity);
            }
            return;
        }

        PortalEntity target = portal.linkedPortal();
        if (target != null) {
            if (target.phase() != PortalLifecycle.Phase.OPEN) return;
            for (Entity entity : touching) {
                if (!gate.enter(entity.getUUID(), now, portal.transitCooldownTicks())) continue;
                transitNormalTree(entity, target);
            }
            return;
        }

        PortalDeferredExitController deferred = portal.deferredExit();
        if (!deferred.active() || deferred.busy()) return;
        for (Entity entity : touching) {
            if (!gate.enter(entity.getUUID(), now, portal.transitCooldownTicks())) continue;
            deferred.transit(entity);
            return;
        }
    }

    @Nullable Entity bootstrapTree(Entity root, ServerLevel targetLevel,
                                   PortalExitTarget target, List<Entity> movedEntities) {
        return PortalTransitService.teleportTree(root,
            (entity, mounted) -> transitBootstrapSingle(entity, targetLevel, target, mounted),
            movedEntities::add, ignored -> {});
    }

    void markInside(UUID entityId, long now) {
        gate.markInside(entityId, now, portal.transitCooldownTicks());
    }

    void leave(UUID entityId) {
        gate.leave(entityId);
    }

    private @Nullable Entity transitNormalTree(Entity root, PortalEntity target) {
        ServerLevel sourceLevel = (ServerLevel) portal.level();
        Vec3 sourcePosition = root.position();
        Entity movedRoot = PortalTransitService.teleportTree(root,
            (entity, mounted) -> transitNormalSingle(entity, target, mounted),
            moved -> {
                long now = portal.serverTime();
                markInside(moved.getUUID(), now);
                target.transit().markInside(moved.getUUID(), now);
            }, failed -> leave(failed.getUUID()));
        if (movedRoot != null) {
            PortalSounds.playTransit(sourceLevel, sourcePosition, portal.soundSnapshot());
            PortalSounds.playTransit(
                (ServerLevel) target.level(), movedRoot.position(), portal.soundSnapshot());
        }
        return movedRoot;
    }

    private @Nullable Entity transitNormalSingle(Entity entity, PortalEntity target,
                                                  boolean mountedTransit) {
        Vec3 momentum = portal.transformVector(entity.getDeltaMovement(), target);
        double outwardSpeed = momentum.dot(target.normal());
        if (outwardSpeed < 0.12) momentum = momentum.add(target.normal().scale(0.12 - outwardSpeed));

        Vec3 look = portal.transformVector(entity.getLookAngle(), target).normalize();
        if (entity instanceof Player) {
            float dot = (float) entity.getLookAngle().normalize().dot(portal.normal());
            if (dot > 0.0F) {
                float blend = Mth.clamp(dot / FACING_THRESHOLD, 0.0F, 1.0F);
                Vec3 mirrored = PortalTransform.betweenFactors(entity.getLookAngle(),
                    portal.orientation(), portal.getYRot(), target.orientation(), target.getYRot(),
                    -1.0F, 1.0F).normalize();
                Vec3 flipped = PortalTransform.betweenFactors(entity.getLookAngle(),
                    portal.orientation(), portal.getYRot(), target.orientation(), target.getYRot(),
                    -1.0F, -1.0F).normalize();
                look = mirrored.lerp(flipped, blend).normalize();
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(-look.y, -1.0, 1.0)));
        return transitSingle(entity, (ServerLevel) target.level(), target.outputPosition(entity),
            momentum, yaw, pitch, mountedTransit);
    }

    private @Nullable Entity transitBootstrapSingle(Entity entity, ServerLevel targetLevel,
                                                     PortalExitTarget target, boolean mountedTransit) {
        return transitSingle(entity, targetLevel, target.position(), Vec3.ZERO,
            target.yaw(), entity.getXRot(), mountedTransit);
    }

    private @Nullable Entity transitSingle(Entity entity, ServerLevel targetLevel,
                                           Vec3 destination, Vec3 momentum,
                                           float yaw, float pitch, boolean mountedTransit) {
        PortalCrisisController.Prepared prepared = entity instanceof ServerPlayer player
            ? portal.crisis().prepare(
                player, targetLevel, destination, momentum, yaw, mountedTransit) : null;
        if (prepared != null && prepared.plan().relocation() != null) {
            var relocation = prepared.plan().relocation();
            destination = relocation.destination();
            momentum = relocation.momentum();
            yaw = relocation.exitPlacement().yaw();
        }
        Entity moved = PortalTransitService.complete(entity, targetLevel,
            new PortalTransitService.TransitPlan(destination, momentum, yaw, pitch),
            portal.fallGuard());
        if (moved instanceof ServerPlayer player && prepared != null) prepared.commit(player);
        else if (moved == null && prepared != null) prepared.abort();
        return moved;
    }
}
