package dev.riftgun.client.compat.immersiveportal;

import dev.riftgun.client.render.PortalVisualPreferences;
import dev.riftgun.client.render.PortalVisualRegistry;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalVisualTarget;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.shape.SpecialFlatPortalShape;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Mesh2D;

/** Owns client-only, non-teleportable IP entity pairs keyed by Rift portal UUID. */
final class ImmersivePortalVisualCache {
    private static final int CIRCLE_SEGMENTS = 56;
    private static final double INNER_RADIUS = 0.91875;
    private static final double MIN_SIZE = 0.001;
    private static final Map<UUID, PortalEntity> SOURCES = new HashMap<>();
    private static final Map<UUID, ProxyPair> PROXIES = new HashMap<>();
    private static final Map<UUID, ImmersivePortalCoverState> COVERS = new HashMap<>();
    private static final Set<UUID> PENDING = new HashSet<>();
    private static final Set<UUID> FADING_COVERS = new HashSet<>();

    static void tick(Minecraft minecraft) {
        if (minecraft.level == null
            || !PortalVisualPreferences.selectedId().equals(PortalVisualRegistry.IMMERSIVE_PORTAL_ID)) {
            suspendProxies();
            return;
        }

        createPending(minecraft);
        updateFadingCovers(minecraft);
    }

    private static void createPending(Minecraft minecraft) {
        Iterator<UUID> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            UUID portalId = iterator.next();
            PortalEntity source = SOURCES.get(portalId);
            if (source == null || source.isRemoved() || source.level() != minecraft.level) {
                if (source != null) SOURCES.remove(portalId, source);
                remove(portalId);
                iterator.remove();
                continue;
            }
            if (source.visualTarget().isEmpty()) continue;
            if (create(source) != null) iterator.remove();
        }
    }

    private static void updateFadingCovers(Minecraft minecraft) {
        Iterator<UUID> iterator = FADING_COVERS.iterator();
        while (iterator.hasNext()) {
            UUID portalId = iterator.next();
            ProxyPair pair = PROXIES.get(portalId);
            if (pair == null || pair.source.isRemoved() || pair.source.level() != minecraft.level) {
                iterator.remove();
                continue;
            }
            ImmersivePortalCoverState cover = COVERS.computeIfAbsent(
                portalId, ignored -> new ImmersivePortalCoverState());
            if (cover.needsDestinationCheck() && targetChunkReady(pair.source)) cover.markReady();
            cover.tick();
            if (!cover.needsTick()) iterator.remove();
        }
    }

    static float readiness(UUID riftPortalId) {
        ImmersivePortalCoverState cover = COVERS.get(riftPortalId);
        return cover == null ? 0.0F : cover.readiness();
    }

    static boolean sync(PortalEntity source, float progress) {
        PortalVisualTarget target = source.visualTarget().orElse(null);
        if (target == null) {
            remove(source.getUUID());
            PENDING.add(source.getUUID());
            return false;
        }

        ProxyPair pair = PROXIES.get(source.getUUID());
        if (pair == null || pair.source != source || pair.front.level() != source.level()) {
            remove(source.getUUID());
            PENDING.add(source.getUUID());
            return false;
        }

        pair.sync(VisualState.capture(source, target, progress));
        return true;
    }

    static void remove(UUID riftPortalId) {
        ProxyPair removed = PROXIES.remove(riftPortalId);
        if (removed != null) discard(removed);
        FADING_COVERS.remove(riftPortalId);
    }

    static void track(Entity entity) {
        if (!(entity instanceof PortalEntity portal)) return;
        PortalEntity previous = SOURCES.put(portal.getUUID(), portal);
        if (previous != portal) remove(portal.getUUID());
        PENDING.add(portal.getUUID());
    }

    static void untrack(Entity entity) {
        if (!(entity instanceof PortalEntity portal)) return;
        if (!SOURCES.remove(portal.getUUID(), portal)) return;
        PENDING.remove(portal.getUUID());
        remove(portal.getUUID());
        Entity.RemovalReason reason = portal.getRemovalReason();
        if (reason != null && reason.shouldDestroy()) COVERS.remove(portal.getUUID());
    }

    static void reset() {
        clearProxies();
        SOURCES.clear();
        COVERS.clear();
        PENDING.clear();
        FADING_COVERS.clear();
    }

    private static ProxyPair create(PortalEntity source) {
        ProxyPair cached = PROXIES.get(source.getUUID());
        if (cached != null) return cached;
        if (!(source.level() instanceof ClientLevel level)) return null;
        PortalVisualTarget target = source.visualTarget().orElse(null);
        if (target == null) return null;
        // Prepare IP's secondary world during the client tick, never from the render stack.
        if (!target.dimension().equals(level.dimension())
            && ClientWorldLoader.getOptionalWorld(target.dimension()) == null) {
            return null;
        }

        Portal front = Portal.ENTITY_TYPE.create(level);
        if (front == null) return null;
        front.setPortalShape(circleShape());
        VisualState initial = VisualState.capture(source, target,
            Math.max(source.visualProgress(1.0F), (float) MIN_SIZE));
        apply(front, initial, false);
        front.setTeleportable(false);
        level.addEntity(front);

        Portal back = PortalAPI.createFlippedPortal(front);
        apply(back, initial, true);
        back.setTeleportable(false);
        level.addEntity(back);

        ProxyPair created = new ProxyPair(source, front, back, initial);
        PROXIES.put(source.getUUID(), created);
        ImmersivePortalCoverState cover = COVERS.computeIfAbsent(
            source.getUUID(), ignored -> new ImmersivePortalCoverState());
        if (cover.needsTick()) FADING_COVERS.add(source.getUUID());
        return created;
    }

    private static void apply(Portal proxy, VisualState state, boolean flipped) {
        Vec3 sourceRight = flipped ? state.sourceRight().scale(-1.0) : state.sourceRight();
        proxy.setOriginPos(state.sourcePosition());
        double scale = Math.max(MIN_SIZE,
            Mth.sin(Mth.clamp(state.progress(), 0.0F, 1.0F) * Mth.HALF_PI));
        proxy.setOrientationAndSize(sourceRight, state.sourceUp(),
            state.width() * scale, state.height() * scale);
        proxy.setDestinationDimension(state.target().dimension());
        proxy.setDestination(state.target().position());
        proxy.setRotation(connectionRotation(state.sourceRight(), state.sourceUp(),
            state.target().right(), state.target().up()));
        proxy.setScaleTransformation(1.0);
        proxy.setTeleportable(false);
    }

    private static SpecialFlatPortalShape circleShape() {
        Mesh2D mesh = new Mesh2D();
        for (int segment = 0; segment < CIRCLE_SEGMENTS; segment++) {
            double first = Math.PI * 2.0 * segment / CIRCLE_SEGMENTS;
            double second = Math.PI * 2.0 * (segment + 1) / CIRCLE_SEGMENTS;
            mesh.addTriangle(0.0, 0.0,
                Math.cos(first) * INNER_RADIUS, Math.sin(first) * INNER_RADIUS,
                Math.cos(second) * INNER_RADIUS, Math.sin(second) * INNER_RADIUS);
        }
        return new SpecialFlatPortalShape(mesh);
    }

    private static boolean targetChunkReady(PortalEntity source) {
        PortalVisualTarget target = source.visualTarget().orElse(null);
        if (target == null) return false;
        ClientLevel targetLevel = ClientWorldLoader.getOptionalWorld(target.dimension());
        if (targetLevel == null) return false;
        int chunkX = SectionPos.blockToSectionCoord(Mth.floor(target.position().x));
        int chunkZ = SectionPos.blockToSectionCoord(Mth.floor(target.position().z));
        return targetLevel.getChunkSource().getChunk(
            chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }

    private static DQuaternion connectionRotation(Vec3 sourceRight, Vec3 sourceUp,
                                                  Vec3 targetRight, Vec3 targetUp) {
        DQuaternion source = DQuaternion.fromFacingVecs(sourceRight, sourceUp);
        DQuaternion target = DQuaternion.fromFacingVecs(targetRight, targetUp);
        DQuaternion delta = target.hamiltonProduct(source.getConjugated());
        return DQuaternion.rotationByDegrees(targetUp, 180.0).hamiltonProduct(delta);
    }

    private static void discard(ProxyPair pair) {
        discard(pair.front);
        discard(pair.back);
    }

    private static void discard(Portal portal) {
        if (!(portal.level() instanceof ClientLevel level) || portal.isRemoved()) return;
        level.removeEntity(portal.getId(), Entity.RemovalReason.DISCARDED);
    }

    private static void clearProxies() {
        PROXIES.values().forEach(ImmersivePortalVisualCache::discard);
        PROXIES.clear();
        FADING_COVERS.clear();
    }

    private static void suspendProxies() {
        clearProxies();
        PENDING.addAll(SOURCES.keySet());
    }

    private static final class ProxyPair {
        private final PortalEntity source;
        private final Portal front;
        private final Portal back;
        private final ImmersivePortalDirtyState<VisualState> state =
            new ImmersivePortalDirtyState<>();

        private ProxyPair(PortalEntity source, Portal front, Portal back, VisualState initial) {
            this.source = source;
            this.front = front;
            this.back = back;
            state.update(initial);
        }

        private void sync(VisualState next) {
            if (!state.update(next)) return;
            apply(front, next, false);
            apply(back, next, true);
        }
    }

    private record VisualState(
        Vec3 sourcePosition,
        Vec3 sourceRight,
        Vec3 sourceUp,
        float width,
        float height,
        PortalVisualTarget target,
        float progress
    ) {
        private static VisualState capture(PortalEntity source, PortalVisualTarget target,
                                           float progress) {
            return new VisualState(source.position(), source.right(), source.up(),
                source.portalWidth(), source.portalHeight(), target, progress);
        }
    }

    private ImmersivePortalVisualCache() {}
}
