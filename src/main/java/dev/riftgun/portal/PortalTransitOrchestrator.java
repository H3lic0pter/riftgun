package dev.riftgun.portal;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.diagnostics.TransitDiagnostics;
import dev.riftgun.entity.SpecialEntityTransitPolicies;
import dev.riftgun.relocation.EntityRelocationExitImmunity;
import dev.riftgun.sound.PortalSounds;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Routes portal contact through normal, deferred and crisis-return transit pipelines. */
final class PortalTransitOrchestrator {
    private static final float FACING_THRESHOLD = 0.35F;

    private final PortalEntity portal;
    private final PortalTransitGate gate = new PortalTransitGate();
    private long lastBlockedRouteDiagnosticAt = Long.MIN_VALUE;

    PortalTransitOrchestrator(PortalEntity portal) {
        this.portal = portal;
    }

    void tick() {
        long now = portal.serverTime();
        AABB search = portal.placement().bounds().inflate(0.6, 2.0, 0.6);
        boolean crisisExit = portal.crisis().isReturnExit();
        PortalTransitEligibility eligibility = new PortalTransitEligibility(
            portal.placement(), portal.entityAccess(), SpecialEntityTransitPolicies.current(), portal.ownerId(),
            crisisExit ? null : portal.excludedPlayerId(),
            crisisExit ? false : portal.isExitPortal(), portal.horizontalTriggerExtend());
        List<Entity> touching = portal.level().getEntities(portal, search, entity ->
            eligibility.allows(entity) && projectileBudgetAllows(entity)
                && (!crisisExit || entity instanceof ServerPlayer
                && portal.crisis().allowsReturn(entity)));
        if (TransitDiagnostics.enabled() && touching.isEmpty()
            && now - lastBlockedRouteDiagnosticAt >= 20L) {
            List<Entity> nearby = portal.level().getEntities(portal, search, entity ->
                !(entity instanceof PortalEntity) && !entity.isPassenger()
                    && PortalTriggerShape.intersects(portal.placement(), entity.getBoundingBox(),
                        portal.horizontalTriggerExtend()));
            if (!nearby.isEmpty()) {
                Entity first = nearby.getFirst();
                String reason = eligibility.rejectionReason(first);
                if (reason == null && !projectileBudgetAllows(first)) reason = "projectile_budget";
                if (reason == null && crisisExit) reason = "crisis_return_denied";
                lastBlockedRouteDiagnosticAt = now;
                long immunityRemaining = "relocation_exit_immunity".equals(reason)
                    ? EntityRelocationExitImmunity.remainingTicks(first) : 0L;
                TransitDiagnostics.portal("nearby entity rejected portal={} root={} type={} dimension={} reason={} remainingTicks={}",
                    portal.getUUID(), first.getUUID(), first.getType(),
//? if >=1.21.11 {
                    /*portal.level().dimension().identifier(), reason == null ? "unknown" : reason,
*///?} else {
                    portal.level().dimension().location(), reason == null ? "unknown" : reason,
//?}
                    immunityRemaining);
            }
        }
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
            //? if >=1.21.11 {
            /*// 26.1.2 loads distant exit chunks asynchronously, so an exit whose
            // chunk is not ticking yet keeps a stale pre-open phase. Treat it as
            // open so transit proceeds while the chunk loads; a ticking exit
            // keeps the strict open-phase requirement (full opening animation).
            boolean targetTicking = target.level() instanceof ServerLevel serverLevel
                && serverLevel.isPositionEntityTicking(target.blockPosition());
            if (target.phase() != PortalLifecycle.Phase.OPEN && targetTicking) {
                logBlockedRoute(touching, now, "linked_target_" + target.phase(), target);
                return;
            }
            *///?} else {
            if (target.phase() != PortalLifecycle.Phase.OPEN) {
                logBlockedRoute(touching, now, "linked_target_" + target.phase(), target);
                return;
            }
            //?}
            for (Entity entity : touching) {
                if (!gate.enter(entity.getUUID(), now, portal.transitCooldownTicks())) continue;
                transitNormalTree(entity, target);
            }
            return;
        }

        PortalDeferredExitController deferred = portal.deferredExit();
        if (!deferred.active() || deferred.busy()) {
            logBlockedRoute(touching, now,
                deferred.busy() ? "deferred_busy" : "linked_target_missing_and_no_deferred_target", null);
            return;
        }
        for (Entity entity : touching) {
            if (!gate.enter(entity.getUUID(), now, portal.transitCooldownTicks())) continue;
            deferred.transit(entity);
            return;
        }
    }

    @Nullable Entity bootstrapTree(Entity root, ServerLevel targetLevel,
                                   PortalExitTarget target, List<Entity> movedEntities) {
        PortalTransitService.TransitPlan[] rootPlan = new PortalTransitService.TransitPlan[1];
        return PortalTransitService.teleportTree(root,
            (entity, mounted) -> {
                Vec3 momentum = entity instanceof Projectile
                    ? entity.getDeltaMovement() : Vec3.ZERO;
                PortalTransitService.TransitPlan plan = new PortalTransitService.TransitPlan(
                    target.position(), momentum, target.yaw(), entity.getXRot());
                if (mounted) {
                    plan = PortalTransitService.mountedPlan(
                        rootPlan[0], plan.yaw(), plan.pitch());
                } else {
                    rootPlan[0] = plan;
                }
                return transitSingle(entity, targetLevel, plan, mounted);
            }, movedEntities::add,
            (failed, stage) -> logTreeFailure(
                root, failed, targetLevel, stage, movedEntities.size()));
    }

    void markInside(UUID entityId, long now) {
        gate.markInside(entityId, now, portal.transitCooldownTicks());
    }

    private void logBlockedRoute(List<Entity> touching, long now, String reason,
                                 @Nullable PortalEntity target) {
        if (touching.isEmpty() || now - lastBlockedRouteDiagnosticAt < 20L) return;
        lastBlockedRouteDiagnosticAt = now;
        Entity first = touching.getFirst();
        TransitDiagnostics.portal("contact blocked portal={} root={} type={} dimension={} reason={} target={} targetDimension={}",
            portal.getUUID(), first.getUUID(), first.getType(),
//? if >=1.21.11 {
            /*portal.level().dimension().identifier(), reason,
*///?} else {
            portal.level().dimension().location(), reason,
//?}
            target == null ? "null" : target.getUUID(),
//? if >=1.21.11 {
            /*target == null ? "null" : target.level().dimension().identifier());
*///?} else {
            target == null ? "null" : target.level().dimension().location());
//?}
    }

    void leave(UUID entityId) {
        gate.leave(entityId);
    }

    boolean trySweptProjectile(Projectile projectile, Vec3 start, Vec3 end) {
        if (!portal.allowsProjectile(projectile)) return false;
        PortalEntity target = portal.linkedPortal();
        if (target == null || target.phase() != PortalLifecycle.Phase.OPEN) return false;
        double radius = Math.max(projectile.getBbWidth(), projectile.getBbHeight()) * 0.5;
        if (!PortalSweptIntersection.crosses(
            portal.placement(), start, end, radius)) return false;
        long now = portal.serverTime();
        if (!gate.enter(projectile.getUUID(), now, portal.transitCooldownTicks())) return false;
        return transitNormalTree(projectile, target) != null;
    }

    boolean trySweptSpecialEntity(Entity entity, Vec3 start, Vec3 end) {
        var policy = SpecialEntityTransitPolicies.current();
        if (!policy.isSweptType(entity.getType())) return false;
        PortalTransitEligibility eligibility = new PortalTransitEligibility(
            portal.placement(), portal.entityAccess(), policy, portal.ownerId(),
            portal.excludedPlayerId(), portal.isExitPortal(), portal.horizontalTriggerExtend());
        if (!eligibility.allowsSwept(entity)) return false;
        PortalEntity target = portal.linkedPortal();
        if (target == null || target.phase() != PortalLifecycle.Phase.OPEN) return false;
        double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.5;
        if (!PortalSweptIntersection.crosses(portal.placement(), start, end, radius)) return false;
        long now = portal.serverTime();
        if (!gate.enter(entity.getUUID(), now, portal.transitCooldownTicks())) return false;
        return transitNormalTree(entity, target) != null;
    }

    private @Nullable Entity transitNormalTree(Entity root, PortalEntity target) {
        ServerLevel sourceLevel = (ServerLevel) portal.level();
        ServerLevel targetLevel = (ServerLevel) target.level();
        Vec3 sourcePosition = root.position();
        TransitDiagnostics.portal("normal trigger sourcePortal={} targetPortal={} root={} type={} source={} destination={} targetChunkTicking={}",
            portal.getUUID(), target.getUUID(), root.getUUID(), root.getType(),
//? if >=1.21.11 {
            /*sourceLevel.dimension().identifier(), targetLevel.dimension().identifier(),
*///?} else {
            sourceLevel.dimension().location(), targetLevel.dimension().location(),
//?}
            targetLevel.isPositionEntityTicking(target.blockPosition()));
        Vec3 rootDestination = treeDestination(root, target);
        if (rootDestination == null) {
            TransitDiagnostics.warning("normal portal preflight failed sourcePortal={} targetPortal={} root={}",
                portal.getUUID(), target.getUUID(), root.getUUID());
            leave(root.getUUID());
            logTreeFailure(root, root, (ServerLevel) target.level(),
                PortalTransitService.FailureStage.PREFLIGHT_CLEARANCE, 0);
            return null;
        }
        PortalTransitService.TransitPlan[] rootPlan = new PortalTransitService.TransitPlan[1];
        int[] movedCount = new int[1];
        long teleportStarted = TransitDiagnostics.enabled() ? System.nanoTime() : 0L;
        TransitDiagnostics.portal("normal teleport before sourcePortal={} targetPortal={} root={} destination={}",
            portal.getUUID(), target.getUUID(), root.getUUID(), rootDestination);
        Entity movedRoot = PortalTransitService.teleportTree(root,
            (entity, mounted) -> {
                PortalTransitService.TransitPlan plan = normalPlan(entity, target);
                if (mounted) {
                    plan = PortalTransitService.mountedPlan(
                        rootPlan[0], plan.yaw(), plan.pitch());
                } else {
                    plan = new PortalTransitService.TransitPlan(
                        rootDestination, plan.momentum(), plan.yaw(), plan.pitch());
                    rootPlan[0] = plan;
                }
                return transitSingle(entity, (ServerLevel) target.level(), plan, mounted);
            },
            moved -> {
                movedCount[0]++;
                long now = portal.serverTime();
                markInside(moved.getUUID(), now);
                target.transit().markInside(moved.getUUID(), now);
            }, (failed, stage) -> {
                leave(failed.getUUID());
                logTreeFailure(
                    root, failed, (ServerLevel) target.level(), stage, movedCount[0]);
            });
        TransitDiagnostics.portal("normal teleport after sourcePortal={} targetPortal={} root={} result={} movedCount={} elapsedMs={} resultDimension={} resultPos={}",
            portal.getUUID(), target.getUUID(), root.getUUID(), movedRoot != null,
            movedCount[0], TransitDiagnostics.enabled()
                ? (System.nanoTime() - teleportStarted) / 1_000_000.0 : 0.0,
//? if >=1.21.11 {
            /*movedRoot == null ? "null" : movedRoot.level().dimension().identifier(),
*///?} else {
            movedRoot == null ? "null" : movedRoot.level().dimension().location(),
//?}
            movedRoot == null ? "null" : movedRoot.position());
        if (movedRoot != null) {
            TransitDiagnostics.trackPostcondition(movedRoot, sourceLevel.dimension(),
                rootDestination, "normal_portal", portal.serverTime());
            if (movedRoot instanceof Projectile projectile) {
                PortalProjectileState.recordSuccessfulTransit(projectile);
                playProjectileTransitEffects(sourceLevel, sourcePosition, target, movedRoot);
            } else {
                PortalSounds.playTransit(sourceLevel, sourcePosition, portal.soundSnapshot());
                PortalSounds.playTransit(
                    (ServerLevel) target.level(), movedRoot.position(), portal.soundSnapshot());
            }
        }
        return movedRoot;
    }

    private @Nullable Vec3 treeDestination(Entity root, PortalEntity target) {
        Vec3 destination = target.outputPosition(root);
        Vec3 translation = destination.subtract(root.position());
        List<AABB> predicted = root.getSelfAndPassengers()
            .map(entity -> entity.getBoundingBox().move(translation))
            .toList();
        double correction = PortalTreeClearance.outwardCorrection(
            target.placement(), predicted, target.horizontalTriggerExtend());
        Vec3 correctionVector = target.normal().scale(correction);
        ServerLevel targetLevel = (ServerLevel) target.level();
        for (AABB bounds : predicted) {
            AABB corrected = bounds.move(correctionVector).deflate(0.001);
            if (targetLevel.getBlockCollisions(null, corrected).iterator().hasNext()) return null;
        }
        return destination.add(correctionVector);
    }

    private boolean projectileBudgetAllows(Entity entity) {
        return !(entity instanceof Projectile projectile) || PortalProjectileState.canTransit(projectile);
    }

    private void playProjectileTransitEffects(ServerLevel sourceLevel, Vec3 sourcePosition,
                                               PortalEntity target, Entity movedRoot) {
        long now = portal.serverTime();
        if (portal.claimProjectileEffect(now)) {
            PortalSounds.playTransit(sourceLevel, sourcePosition, portal.soundSnapshot());
        }
        if (target.claimProjectileEffect(now)) {
            PortalSounds.playTransit(
                (ServerLevel) target.level(), movedRoot.position(), portal.soundSnapshot());
        }
    }

    private PortalTransitService.TransitPlan normalPlan(Entity entity, PortalEntity target) {
        Vec3 momentum = portal.transformVector(entity.getDeltaMovement(), target);
        if (!(entity instanceof Projectile)) {
            double outwardSpeed = momentum.dot(target.normal());
            if (outwardSpeed < 0.12) momentum = momentum.add(
                target.normal().scale(0.12 - outwardSpeed));
        }

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
        return new PortalTransitService.TransitPlan(
            target.outputPosition(entity), momentum, yaw, pitch);
    }

    private @Nullable Entity transitSingle(Entity entity, ServerLevel targetLevel,
                                           PortalTransitService.TransitPlan plan,
                                           boolean mountedTransit) {
        Vec3 destination = plan.destination();
        Vec3 momentum = plan.momentum();
        float yaw = plan.yaw();
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
            new PortalTransitService.TransitPlan(destination, momentum, yaw, plan.pitch()),
            portal.fallGuard(), portal.entityFallGuard());
        if (moved instanceof ServerPlayer player && prepared != null) prepared.commit(player);
        else if (moved == null && prepared != null) prepared.abort();
        return moved;
    }

    private void logTreeFailure(Entity root, Entity failed, ServerLevel targetLevel,
                                PortalTransitService.FailureStage stage, int movedCount) {
        PortalEntity linked = portal.linkedPortal();
        TransitDiagnostics.warning(
            "portal passenger-tree failure stage={} portal={} linked={} sourceDimension={} "
                + "targetDimension={} rootType={} rootUuid={} failedType={} failedUuid={} movedCount={}",
            stage, portal.getUUID(), linked == null ? "deferred" : linked.getUUID(),
//? if >=1.21.11 {
            /*portal.level().dimension().identifier(), targetLevel.dimension().identifier(),
*///?} else {
            portal.level().dimension().location(), targetLevel.dimension().location(),
//?}
            EntityType.getKey(root.getType()), root.getUUID(),
            EntityType.getKey(failed.getType()), failed.getUUID(), movedCount);
    }

}
