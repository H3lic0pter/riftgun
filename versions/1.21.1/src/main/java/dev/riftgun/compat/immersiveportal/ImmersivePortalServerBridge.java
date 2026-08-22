package dev.riftgun.compat.immersiveportal;

import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalVisualTarget;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.DQuaternion;

/** Maintains per-player invisible IP portals solely for destination chunk streaming. */
final class ImmersivePortalServerBridge {
    private static final String PROXY_TAG_PREFIX = "riftgun_visual:";
    private static final double TRACKING_RANGE_SQUARED = 160.0 * 160.0;
    private static final Map<UUID, PortalEntity> SOURCES = new HashMap<>();
    private static final Set<UUID> SELECTED_PLAYERS = new HashSet<>();
    private static final Map<Key, Proxy> PROXIES = new HashMap<>();
    private static MinecraftServer activeServer;

    static void tick(MinecraftServer server) {
        if (activeServer != server) {
            clear();
            activeServer = server;
            collectLoadedSourcesAndRemovePersistedProxies(server);
        }

        SOURCES.values().removeIf(source -> source.isRemoved() || source.getServer() != server);
        SELECTED_PLAYERS.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        Set<Key> live = new HashSet<>();
        for (UUID playerId : SELECTED_PLAYERS) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) continue;
            for (PortalEntity source : SOURCES.values()) {
                if (!source.level().dimension().equals(player.level().dimension())
                    || source.distanceToSqr(player) > TRACKING_RANGE_SQUARED
                    || source.visualTarget().isEmpty()) continue;
                Key key = new Key(playerId, source.getUUID());
                live.add(key);
                sync(key, player, source);
            }
        }
        PROXIES.entrySet().removeIf(entry -> {
            if (live.contains(entry.getKey())) return false;
            entry.getValue().portal().discard();
            return true;
        });
    }

    static void setSelected(ServerPlayer player, boolean selected) {
        if (selected) SELECTED_PLAYERS.add(player.getUUID());
        else removePlayer(player.getUUID());
    }

    static void removePlayer(UUID playerId) {
        SELECTED_PLAYERS.remove(playerId);
        PROXIES.entrySet().removeIf(entry -> {
            if (!entry.getKey().playerId().equals(playerId)) return false;
            entry.getValue().portal().discard();
            return true;
        });
    }

    static void track(Entity entity) {
        if (entity instanceof PortalEntity portal) SOURCES.put(portal.getUUID(), portal);
    }

    static void untrack(Entity entity) {
        if (!(entity instanceof PortalEntity portal)) return;
        SOURCES.remove(portal.getUUID());
        PROXIES.entrySet().removeIf(entry -> {
            if (!entry.getKey().sourceId().equals(portal.getUUID())) return false;
            entry.getValue().portal().discard();
            return true;
        });
    }

    private static void sync(Key key, ServerPlayer player, PortalEntity source) {
        PortalVisualTarget target = source.visualTarget().orElse(null);
        if (target == null || !(source.level() instanceof ServerLevel level)) return;
        Proxy cached = PROXIES.get(key);
        if (cached == null || cached.portal().isRemoved() || cached.portal().level() != level) {
            if (cached != null) cached.portal().discard();
            Portal portal = Portal.ENTITY_TYPE.create(level);
            if (portal == null) return;
            portal.portalTag = PROXY_TAG_PREFIX + player.getUUID() + ":" + source.getUUID();
            portal.specificPlayerId = player.getUUID();
            portal.setIsVisible(false);
            portal.setTeleportable(false);
            apply(portal, source, target);
            if (!level.addFreshEntity(portal)) return;
            cached = new Proxy(portal);
            PROXIES.put(key, cached);
        }
        apply(cached.portal(), source, target);
    }

    private static void apply(Portal proxy, PortalEntity source, PortalVisualTarget target) {
        Vec3 sourceRight = source.right();
        Vec3 sourceUp = source.up();
        proxy.setOriginPos(source.position());
        proxy.setOrientationAndSize(sourceRight, sourceUp, source.portalWidth(), source.portalHeight());
        proxy.setDestinationDimension(target.dimension());
        proxy.setDestination(target.position());
        proxy.setRotation(connectionRotation(sourceRight, sourceUp, target.right(), target.up()));
        proxy.setScaleTransformation(1.0);
        proxy.setIsVisible(false);
        proxy.setTeleportable(false);
    }

    private static DQuaternion connectionRotation(Vec3 sourceRight, Vec3 sourceUp,
                                                  Vec3 targetRight, Vec3 targetUp) {
        DQuaternion source = DQuaternion.fromFacingVecs(sourceRight, sourceUp);
        DQuaternion target = DQuaternion.fromFacingVecs(targetRight, targetUp);
        DQuaternion delta = target.hamiltonProduct(source.getConjugated());
        return DQuaternion.rotationByDegrees(targetUp, 180.0).hamiltonProduct(delta);
    }

    private static void collectLoadedSourcesAndRemovePersistedProxies(MinecraftServer server) {
        ArrayList<Portal> stale = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof PortalEntity portal) {
                    SOURCES.put(portal.getUUID(), portal);
                } else if (entity instanceof Portal portal && portal.portalTag != null
                    && portal.portalTag.startsWith(PROXY_TAG_PREFIX)) {
                    stale.add(portal);
                }
            }
        }
        stale.forEach(Entity::discard);
    }

    static void clear() {
        PROXIES.values().forEach(proxy -> proxy.portal().discard());
        SOURCES.clear();
        SELECTED_PLAYERS.clear();
        PROXIES.clear();
        activeServer = null;
    }

    private record Key(UUID playerId, UUID sourceId) {}
    private record Proxy(Portal portal) {}

    private ImmersivePortalServerBridge() {}
}
