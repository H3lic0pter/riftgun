package dev.riftgun.portal;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.diagnostics.TransitDiagnostics;
import dev.riftgun.sound.PortalSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Owns deferred-target state and the unloaded-destination bootstrap transaction. */
final class PortalDeferredExitController {
    private final PortalEntity portal;
    private @Nullable PortalExitTarget target;
    private @Nullable UUID excludedPlayer;
    private boolean creatingExit;

    PortalDeferredExitController(PortalEntity portal) {
        this.portal = portal;
    }

    void configure(PortalExitTarget target, @Nullable UUID excludedPlayer) {
        this.target = target;
        this.excludedPlayer = excludedPlayer;
    }

    boolean active() {
        return target != null;
    }

    boolean busy() {
        return creatingExit;
    }

    @Nullable ServerLevel targetLevel() {
        if (target == null || !(portal.level() instanceof ServerLevel level)) return null;
        return level.getServer().getLevel(target.dimension());
    }

    void transit(Entity root) {
        PortalExitTarget currentTarget = target;
        ServerLevel destinationLevel = targetLevel();
        if (currentTarget == null || destinationLevel == null) {
            portal.transit().leave(root.getUUID());
            return;
        }

        creatingExit = true;
        try {
            BlockPos targetPosition = BlockPos.containing(currentTarget.position());
            boolean ticking = destinationLevel.isPositionEntityTicking(targetPosition);
            TransitDiagnostics.portal("deferred trigger portal={} root={} type={} source={} destination={} target={} chunkTicking={}",
                portal.getUUID(), root.getUUID(), root.getType(),
//? if >=1.21.11 {
                /*portal.level().dimension().identifier(), destinationLevel.dimension().identifier(),
*///?} else {
                portal.level().dimension().location(), destinationLevel.dimension().location(),
//?}
                currentTarget.position(), ticking);
            if (ticking) {
                PortalEntity exit = portal.createDeferredExit(
                    destinationLevel, currentTarget, List.of(), excludedPlayer);
                TransitDiagnostics.portal("deferred target already ticking portal={} root={} exitCreated={}",
                    portal.getUUID(), root.getUUID(), exit != null);
                portal.transit().leave(root.getUUID());
                if (exit == null) portal.warnDeferredExitFailure(destinationLevel.getServer(), List.of());
                return;
            }

            ServerLevel sourceLevel = (ServerLevel) portal.level();
            Vec3 sourcePosition = root.position();
            List<Entity> movedEntities = new ArrayList<>();
            long teleportStarted = TransitDiagnostics.enabled() ? System.nanoTime() : 0L;
            TransitDiagnostics.portal("deferred bootstrap before portal={} root={} sourcePos={} destination={} target={}",
                portal.getUUID(), root.getUUID(), sourcePosition,
                destinationLevel.dimension().location(), currentTarget.position());
            Entity movedRoot = portal.transit().bootstrapTree(
                root, destinationLevel, currentTarget, movedEntities);
            TransitDiagnostics.portal("deferred bootstrap after portal={} root={} result={} movedCount={} elapsedMs={} resultPos={} chunkTicking={}",
                portal.getUUID(), root.getUUID(), movedRoot != null,
                movedEntities.size(), TransitDiagnostics.enabled()
                    ? (System.nanoTime() - teleportStarted) / 1_000_000.0 : 0.0,
                movedRoot == null ? "null" : movedRoot.position(),
                destinationLevel.isPositionEntityTicking(targetPosition));
            if (movedRoot == null) {
                portal.transit().leave(root.getUUID());
                portal.warnDeferredTeleportFailure(destinationLevel.getServer(), root);
                return;
            }
            TransitDiagnostics.trackPostcondition(movedRoot, sourceLevel.dimension(),
                currentTarget.position(), "deferred_portal", portal.serverTime());

            PortalEntity exit = portal.createDeferredExit(
                destinationLevel, currentTarget, movedEntities, excludedPlayer);
            TransitDiagnostics.portal("deferred exit creation portal={} root={} exitCreated={} movedCount={}",
                portal.getUUID(), root.getUUID(), exit != null, movedEntities.size());
            if (exit == null) portal.warnDeferredExitFailure(destinationLevel.getServer(), movedEntities);
            if (exit != null) {
                for (Entity moved : movedEntities) {
                    if (moved instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
                        projectile.setDeltaMovement(portal.transformVector(
                            projectile.getDeltaMovement(), exit));
                        projectile.hasImpulse = true;
                        ProjectileMotion.alignToVelocity(projectile, projectile.getDeltaMovement());
                        PortalProjectileState.recordSuccessfulTransit(projectile);
                    }
                }
            }
            if (movedRoot instanceof net.minecraft.world.entity.projectile.Projectile) {
                long now = portal.serverTime();
                if (portal.claimProjectileEffect(now)) {
                    PortalSounds.playTransit(sourceLevel, sourcePosition, portal.soundSnapshot());
                }
                if (exit != null && exit.claimProjectileEffect(now)) {
                    PortalSounds.playTransit(destinationLevel, movedRoot.position(), portal.soundSnapshot());
                }
            } else {
                PortalSounds.playTransit(sourceLevel, sourcePosition, portal.soundSnapshot());
                PortalSounds.playTransit(destinationLevel, movedRoot.position(), portal.soundSnapshot());
            }
        } finally {
            creatingExit = false;
        }
    }

    void complete() {
        target = null;
        excludedPlayer = null;
    }

    void load(CompoundTag tag) {
        target = tag.contains("DeferredTarget")
            ? PortalExitTarget.load(Nbt.getCompound(tag, "DeferredTarget")) : null;
        excludedPlayer = Nbt.hasUUID(tag, "DeferredExitExclude")
            ? Nbt.getUUID(tag, "DeferredExitExclude") : null;
        creatingExit = false;
    }

    void save(CompoundTag tag) {
        if (target != null) tag.put("DeferredTarget", target.save());
        if (excludedPlayer != null) Nbt.putUUID(tag, "DeferredExitExclude", excludedPlayer);
    }
}
