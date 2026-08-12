package dev.riftgun.portal;

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
            if (destinationLevel.isPositionEntityTicking(BlockPos.containing(currentTarget.position()))) {
                PortalEntity exit = portal.createDeferredExit(
                    destinationLevel, currentTarget, List.of(), excludedPlayer);
                portal.transit().leave(root.getUUID());
                if (exit == null) portal.warnDeferredExitFailure(destinationLevel.getServer(), List.of());
                return;
            }

            ServerLevel sourceLevel = (ServerLevel) portal.level();
            Vec3 sourcePosition = root.position();
            List<Entity> movedEntities = new ArrayList<>();
            Entity movedRoot = portal.transit().bootstrapTree(
                root, destinationLevel, currentTarget, movedEntities);
            if (movedRoot == null) {
                portal.transit().leave(root.getUUID());
                portal.warnDeferredTeleportFailure(destinationLevel.getServer(), root);
                return;
            }

            PortalEntity exit = portal.createDeferredExit(
                destinationLevel, currentTarget, movedEntities, excludedPlayer);
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
            ? PortalExitTarget.load(tag.getCompound("DeferredTarget")) : null;
        excludedPlayer = tag.hasUUID("DeferredExitExclude")
            ? tag.getUUID("DeferredExitExclude") : null;
        creatingExit = false;
    }

    void save(CompoundTag tag) {
        if (target != null) tag.put("DeferredTarget", target.save());
        if (excludedPlayer != null) tag.putUUID("DeferredExitExclude", excludedPlayer);
    }
}
