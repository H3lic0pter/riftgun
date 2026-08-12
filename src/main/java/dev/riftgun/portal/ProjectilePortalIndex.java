package dev.riftgun.portal;

import dev.riftgun.config.ServerConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Event-maintained projectile set plus per-level chunk index of projectile-enabled portals. */
public final class ProjectilePortalIndex {
    private static final Map<ServerLevel, LevelIndex> LEVELS = new IdentityHashMap<>();
    private static final Set<Projectile> PROJECTILES = java.util.Collections.newSetFromMap(
        new IdentityHashMap<>());

    static void refresh(PortalEntity portal) {
        if (!(portal.level() instanceof ServerLevel level)) return;
        if (!ServerConfig.VALUES.enableProjectileSweptCollision.get()) {
            LevelIndex existing = LEVELS.get(level);
            if (existing != null) existing.remove(portal.getUUID());
            return;
        }
        LevelIndex index = LEVELS.computeIfAbsent(level, ignored -> new LevelIndex());
        if (portal.phase() == PortalLifecycle.Phase.OPEN && portal.entityAccessAllowsProjectiles()) {
            index.put(portal);
        } else {
            index.remove(portal.getUUID());
        }
    }

    static void unregister(PortalEntity portal) {
        if (portal.level() instanceof ServerLevel level) {
            LevelIndex index = LEVELS.get(level);
            if (index != null) index.remove(portal.getUUID());
        }
    }

    public static void track(Projectile projectile) {
        if (!projectile.level().isClientSide()
            && ServerConfig.VALUES.enableProjectileSweptCollision.get()) PROJECTILES.add(projectile);
    }

    public static void untrack(Projectile projectile) {
        PROJECTILES.remove(projectile);
    }

    public static void tick(MinecraftServer server) {
        if (!ServerConfig.VALUES.enableProjectileSweptCollision.get()) {
            LEVELS.clear();
            PROJECTILES.clear();
            return;
        }
        for (Projectile projectile : List.copyOf(PROJECTILES)) {
            if (!projectile.isAlive() || !(projectile.level() instanceof ServerLevel)) continue;
            tryTransit(projectile);
        }
    }

    public static boolean tryTransit(Projectile projectile) {
        if (!ServerConfig.VALUES.enableProjectileSweptCollision.get()
            || !(projectile.level() instanceof ServerLevel level)) return false;
        LevelIndex index = LEVELS.get(level);
        if (index == null) return false;
        for (PortalEntity portal : index.candidates(projectile)) {
            if (portal.isAlive() && portal.trySweptProjectile(projectile)) return true;
        }
        return false;
    }

    public static void reset() {
        LEVELS.clear();
        PROJECTILES.clear();
    }

    private static final class LevelIndex {
        private final Map<Long, Set<PortalEntity>> byChunk = new HashMap<>();
        private final Map<UUID, Set<Long>> chunksByPortal = new HashMap<>();

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

        List<PortalEntity> candidates(Projectile projectile) {
            AABB swept = projectile.getBoundingBox().expandTowards(
                new net.minecraft.world.phys.Vec3(projectile.xo, projectile.yo, projectile.zo)
                    .subtract(projectile.position())).inflate(0.25);
            Set<PortalEntity> result = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (long chunk : chunks(swept)) {
                Set<PortalEntity> portals = byChunk.get(chunk);
                if (portals != null) result.addAll(portals);
            }
            Vec3 start = new Vec3(projectile.xo, projectile.yo, projectile.zo);
            Vec3 end = projectile.position();
            double radius = Math.max(projectile.getBbWidth(), projectile.getBbHeight()) * 0.5;
            List<PortalEntity> ordered = new ArrayList<>(result);
            ordered.sort(java.util.Comparator.comparingDouble(portal ->
                PortalProjectileIntersection.crossingFraction(
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
                for (int z = minZ; z <= maxZ; z++) result.add(ChunkPos.asLong(x, z));
            }
            return result;
        }
    }

    private ProjectilePortalIndex() {}
}
