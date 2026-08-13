package dev.riftgun.relocation;

import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.sound.PortalSoundSnapshot;
import dev.riftgun.sound.PortalSounds;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Owns relocation exit creation, sharing, chunk tickets, lookup, and cleanup. */
final class EntityRelocationExitService {
    private static final EntityRelocationExitIndex INDEX = new EntityRelocationExitIndex();

    static @Nullable Handle open(MinecraftServer server, OpenRequest request) {
        EntityRelocationExitIndex.Lease shared = request.sharedKey() == null ? null
            : reserveShared(server, request.sharedKey(), request.side());
        if (shared != null) {
            EntityRelocationPortalEntity portal = resolve(server, shared);
            if (portal == null) {
                releaseShared(server, shared, false);
                return null;
            }
            portal.acquireChunkTicket();
            return new Handle(shared, null, request.level().dimension());
        }

        EntityRelocationPortalEntity exit = request.followPlayer() == null
            ? EntityRelocationPortalEntity.createExit(request.level(), request.center(), request.side(),
                request.rgb(), request.durationTicks(), request.sounds(), request.orientation(),
                request.yaw(), request.openingTicks())
            : EntityRelocationPortalEntity.createPlayerDestinationExit(request.level(),
                request.followPlayer(), request.side(), request.rgb(), request.durationTicks(),
                request.sounds(), request.openingTicks());
        if (!request.level().addFreshEntity(exit)) return null;
        exit.acquireChunkTicket();
        PortalSounds.playOpening(request.level(), exit.position(), request.sounds());
        if (request.sharedKey() != null) {
            INDEX.register(request.sharedKey(), new EntityRelocationExitIndex.ExitReference(
                exit.getUUID(), request.level().dimension().location()));
        }
        return new Handle(null, exit.getUUID(), request.level().dimension());
    }

    static @Nullable EntityRelocationPortalEntity resolve(MinecraftServer server,
                                                           @Nullable Handle handle) {
        if (handle == null) return null;
        if (handle.sharedExit() != null) return resolve(server, handle.sharedExit());
        if (handle.portalId() == null) return null;
        ServerLevel level = server.getLevel(handle.dimension());
        Entity entity = level == null ? null : level.getEntity(handle.portalId());
        return entity instanceof EntityRelocationPortalEntity portal && portal.isExit() ? portal : null;
    }

    static void close(MinecraftServer server, @Nullable Handle handle, boolean successful) {
        if (handle == null) return;
        EntityRelocationPortalEntity portal = resolve(server, handle);
        if (portal != null) portal.releaseChunkTicket();
        if (handle.sharedExit() != null) {
            releaseShared(server, handle.sharedExit(), successful);
        } else if (!successful && portal != null && portal.phase() != PortalLifecycle.Phase.OPEN) {
            portal.beginClosing();
        }
    }

    static void unregister(UUID portalId) {
        INDEX.unregister(portalId);
    }

    static void clear() {
        INDEX.clear();
    }

    private static @Nullable EntityRelocationExitIndex.Lease reserveShared(
            MinecraftServer server, EntityRelocationExitIndex.DestinationKey key, float side) {
        return INDEX.reserveStable(key, side, new EntityRelocationExitIndex.CandidateAccess() {
            @Override
            public EntityRelocationExitIndex.Candidate inspect(EntityRelocationExitIndex.ExitReference exit) {
                EntityRelocationPortalEntity portal = resolve(server, exit);
                if (portal == null) return EntityRelocationExitIndex.Candidate.missing();
                return switch (portal.phase()) {
                    case OPENING, CHARGING -> EntityRelocationExitIndex.Candidate.opening();
                    case OPEN -> EntityRelocationExitIndex.Candidate.open(portal.remainingOpenTicks());
                    case CLOSING, CLOSED -> EntityRelocationExitIndex.Candidate.closing();
                };
            }

            @Override
            public boolean tryReserve(EntityRelocationExitIndex.ExitReference exit, float requiredSide) {
                EntityRelocationPortalEntity portal = resolve(server, exit);
                return portal != null && portal.tryReserve(requiredSide);
            }
        }).orElse(null);
    }

    private static void releaseShared(MinecraftServer server,
                                      EntityRelocationExitIndex.Lease lease, boolean successful) {
        EntityRelocationPortalEntity portal = resolve(server, lease);
        if (portal != null) portal.releaseReservation(successful);
    }

    private static @Nullable EntityRelocationPortalEntity resolve(
            MinecraftServer server, EntityRelocationExitIndex.Lease lease) {
        return resolve(server, lease.exit());
    }

    private static @Nullable EntityRelocationPortalEntity resolve(
            MinecraftServer server, EntityRelocationExitIndex.ExitReference reference) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, reference.dimension());
        ServerLevel level = server.getLevel(dimension);
        Entity entity = level == null ? null : level.getEntity(reference.portalId());
        return entity instanceof EntityRelocationPortalEntity portal && portal.isExit() ? portal : null;
    }

    record OpenRequest(ServerLevel level, @Nullable ServerPlayer followPlayer, Vec3 center,
                       float side, int rgb, int durationTicks, PortalSoundSnapshot sounds,
                       PortalOrientation orientation, float yaw, int openingTicks,
                       @Nullable EntityRelocationExitIndex.DestinationKey sharedKey) {}

    record Handle(@Nullable EntityRelocationExitIndex.Lease sharedExit, @Nullable UUID portalId,
                  ResourceKey<Level> dimension) {}

    private EntityRelocationExitService() {}
}
