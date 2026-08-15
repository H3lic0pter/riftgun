package dev.riftgun.portal;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.entity.SpecialEntityTransitPolicies;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tracks swept-transit entities and indexes open portal faces by level and chunk. */
public final class SweptPortalIndex {
    private static final Map<ServerLevel, LevelIndex> LEVELS = new IdentityHashMap<>();
    private static final Set<Projectile> PROJECTILES = java.util.Collections.newSetFromMap(
        new IdentityHashMap<>());
    private static final Set<Entity> SPECIAL_ENTITIES = java.util.Collections.newSetFromMap(
        new IdentityHashMap<>());
    private static final Set<ServerLevel> RECONCILED_LEVELS = java.util.Collections.newSetFromMap(
        new IdentityHashMap<>());
    private static boolean reconciliationPending;

    static void refresh(PortalEntity portal) {
        if (!(portal.level() instanceof ServerLevel level)) return;
        boolean projectileSwept = RiftConfigs.server().projectile().sweptCollisionEnabled();
        boolean specialSwept = RiftConfigs.server().specialEntityTransit().sweptCollisionEnabled();
        if (!projectileSwept && !specialSwept) {
            LevelIndex existing = LEVELS.get(level);
            if (existing != null) {
                existing.remove(portal.getUUID());
                removeEmptyLevel(level, existing);
            }
            return;
        }
        LevelIndex index = LEVELS.computeIfAbsent(level, ignored -> new LevelIndex());
        if (portal.phase() == PortalLifecycle.Phase.OPEN
            && (specialSwept || projectileSwept && portal.entityAccessAllowsProjectiles())) {
            index.put(portal);
        } else {
            index.remove(portal.getUUID());
            removeEmptyLevel(level, index);
        }
    }

    static void unregister(PortalEntity portal) {
        if (portal.level() instanceof ServerLevel level) {
            LevelIndex index = LEVELS.get(level);
            if (index != null) {
                index.remove(portal.getUUID());
                removeEmptyLevel(level, index);
            }
        }
    }

    private static void removeEmptyLevel(ServerLevel level, LevelIndex index) {
        if (index.isEmpty()) LEVELS.remove(level);
    }

    public static void track(Projectile projectile) {
        if (!projectile.level().isClientSide()
            && RiftConfigs.server().projectile().sweptCollisionEnabled()) PROJECTILES.add(projectile);
    }

    public static void untrack(Projectile projectile) {
        PROJECTILES.remove(projectile);
    }

    public static void trackSpecialEntity(Entity entity) {
        if (!(entity instanceof Projectile) && !entity.level().isClientSide()
            && RiftConfigs.server().specialEntityTransit().sweptCollisionEnabled()
            && SpecialEntityTransitPolicies.current().isSweptType(entity.getType())) {
            SPECIAL_ENTITIES.add(entity);
        }
    }

    public static void untrackSpecialEntity(Entity entity) {
        SPECIAL_ENTITIES.remove(entity);
    }

    public static void reconcileSpecialEntities() {
        SPECIAL_ENTITIES.clear();
        RECONCILED_LEVELS.clear();
        reconciliationPending = true;
    }

    public static void serverTick() {
        boolean projectileSwept = RiftConfigs.server().projectile().sweptCollisionEnabled();
        boolean specialSwept = RiftConfigs.server().specialEntityTransit().sweptCollisionEnabled();
        if (!projectileSwept && !specialSwept) {
            LEVELS.clear();
            PROJECTILES.clear();
            SPECIAL_ENTITIES.clear();
            return;
        }
        if (!projectileSwept) {
            PROJECTILES.clear();
        } else if (!LEVELS.isEmpty()) {
            for (Projectile projectile : List.copyOf(PROJECTILES)) {
                if (!projectile.isAlive() || !(projectile.level() instanceof ServerLevel)) {
                    PROJECTILES.remove(projectile);
                    continue;
                }
                tryTransit(projectile);
            }
        }
        if (!specialSwept) {
            SPECIAL_ENTITIES.clear();
        }
        reconciliationPending = false;
        RECONCILED_LEVELS.clear();
    }

    /** Runs before vanilla entity ticks so falling blocks cannot place and discard first. */
    public static void beforeEntityTicks(ServerLevel level) {
        if (!RiftConfigs.server().specialEntityTransit().sweptCollisionEnabled()) return;
        if (reconciliationPending && RECONCILED_LEVELS.add(level)) {
            for (Entity entity : level.getAllEntities()) trackSpecialEntity(entity);
        }
        if (SPECIAL_ENTITIES.isEmpty() || !LEVELS.containsKey(level)) return;
        for (Entity entity : List.copyOf(SPECIAL_ENTITIES)) {
            if (!entity.isAlive()) {
                SPECIAL_ENTITIES.remove(entity);
                continue;
            }
            if (entity.level() != level) continue;
            Vec3 start = entity.position();
            Vec3 movement = predictedMovement(entity.getDeltaMovement(), entity.getGravity());
            Vec3 end = collisionLimitedEnd(entity, level, movement);
            tryTransitSpecialEntity(entity, start, end);
        }
    }

    static Vec3 predictedMovement(Vec3 velocity, double gravity) {
        return velocity.add(0.0, -gravity, 0.0);
    }

    private static Vec3 collisionLimitedEnd(Entity entity, ServerLevel level, Vec3 movement) {
        AABB bounds = entity.getBoundingBox();
        List<VoxelShape> entityCollisions = level.getEntityCollisions(
            entity, bounds.expandTowards(movement));
        Vec3 allowed = Entity.collideBoundingBox(
            entity, movement, bounds, level, entityCollisions);
        return entity.position().add(allowed);
    }

    public static boolean tryTransit(Projectile projectile) {
        Vec3 start = new Vec3(projectile.xo, projectile.yo, projectile.zo);
        return tryTransit(projectile, start, projectile.position(), null);
    }

    public static boolean tryTransit(Projectile projectile, HitResult impact) {
        return tryTransit(projectile, projectile.position(), impact.getLocation(), impact);
    }

    private static boolean tryTransitSpecialEntity(Entity entity, Vec3 start, Vec3 end) {
        if (!SpecialEntityTransitPolicies.current().isSweptType(entity.getType())
            || !(entity.level() instanceof ServerLevel level)) return false;
        LevelIndex index = LEVELS.get(level);
        if (index == null) return false;
        for (PortalEntity portal : index.candidates(entity, start, end)) {
            if (portal.isAlive() && portal.trySweptSpecialEntity(entity, start, end)) return true;
        }
        return false;
    }

    private static boolean tryTransit(Projectile projectile, Vec3 start, Vec3 end,
                                      HitResult impact) {
        if (!RiftConfigs.server().projectile().sweptCollisionEnabled()
            || !(projectile.level() instanceof ServerLevel level)) return false;
        LevelIndex index = LEVELS.get(level);
        if (index == null) return false;
        for (PortalEntity portal : index.candidates(projectile, start, end)) {
            if (!portal.isAlive()) continue;
            if (impact != null && !PortalSweptIntersection.crossesBeforeImpact(
                portal.placement(), start, end, projectileRadius(projectile),
                impact.getLocation())) continue;
            if (portal.trySweptProjectile(projectile, start, end)) return true;
        }
        return false;
    }

    private static double projectileRadius(Projectile projectile) {
        return Math.max(projectile.getBbWidth(), projectile.getBbHeight()) * 0.5;
    }

    public static void reset() {
        LEVELS.clear();
        PROJECTILES.clear();
        SPECIAL_ENTITIES.clear();
        RECONCILED_LEVELS.clear();
        reconciliationPending = false;
    }

    private static final class LevelIndex {
        private final Map<Long, Set<PortalEntity>> byChunk = new HashMap<>();
        private final Map<UUID, Set<Long>> chunksByPortal = new HashMap<>();

        boolean isEmpty() {
            return chunksByPortal.isEmpty();
        }

        void put(PortalEntity portal) {
            if (chunksByPortal.containsKey(portal.getUUID())) return;
            Set<Long> chunks = chunks(portal.placement().bounds().inflate(0.5));
            chunksByPortal.put(portal.getUUID(), chunks);
            for (long chunk : chunks) byChunk.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(portal);
        }

        void remove(UUID portalId) {
            Set<Long> chunks = chunksByPortal.remove(portalId);
            if (chunks == null) return;
            for (long chunk : chunks) {
                Set<PortalEntity> portals = byChunk.get(chunk);
                if (portals == null) continue;
                portals.removeIf(portal -> portal.getUUID().equals(portalId));
                if (portals.isEmpty()) byChunk.remove(chunk);
            }
        }

        List<PortalEntity> candidates(Entity entity, Vec3 start, Vec3 end) {
            AABB swept = new AABB(start, end).inflate(entityRadius(entity) + 0.25);
            Set<PortalEntity> result = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (long chunk : chunks(swept)) {
                Set<PortalEntity> portals = byChunk.get(chunk);
                if (portals != null) result.addAll(portals);
            }
            double radius = entityRadius(entity);
            List<PortalEntity> ordered = new ArrayList<>(result);
            ordered.sort(java.util.Comparator.comparingDouble(portal ->
                PortalSweptIntersection.crossingFraction(
                    portal.placement(), start, end, radius)));
            return ordered;
        }

        private static Set<Long> chunks(AABB bounds) {
            Set<Long> result = new HashSet<>();
            int minX = net.minecraft.util.Mth.floor(bounds.minX) >> 4;
            int maxX = net.minecraft.util.Mth.floor(bounds.maxX) >> 4;
            int minZ = net.minecraft.util.Mth.floor(bounds.minZ) >> 4;
            int maxZ = net.minecraft.util.Mth.floor(bounds.maxZ) >> 4;
            for (int x = minX; x <= maxX; x++) {
//? if >=1.21.11 {
                /*for (int z = minZ; z <= maxZ; z++) result.add(ChunkPos.pack(x, z));
*///?} else {
                for (int z = minZ; z <= maxZ; z++) result.add(ChunkPos.asLong(x, z));
//?}
            }
            return result;
        }
    }

    private static double entityRadius(Entity entity) {
        return Math.max(entity.getBbWidth(), entity.getBbHeight()) * 0.5;
    }

    private SweptPortalIndex() {}
}
