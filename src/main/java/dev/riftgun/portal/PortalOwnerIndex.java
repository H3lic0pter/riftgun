package dev.riftgun.portal;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-scoped authoritative lookup for live portals grouped by owner.
 *
 * <p>All mutation happens from server lifecycle events, so callers stay on the
 * server thread and no synchronization is required.
 */
public final class PortalOwnerIndex {
    private static final Store<MinecraftServer, PortalEntity> STORE = new Store<>();

    public static void track(PortalEntity portal) {
        UUID owner = portal.ownerId();
        if (owner == null || !(portal.level() instanceof ServerLevel level)) return;
        STORE.track(level.getServer(), owner, portal);
    }

    public static void untrack(PortalEntity portal) {
        UUID owner = portal.ownerId();
        if (owner == null || !(portal.level() instanceof ServerLevel level)) return;
        STORE.untrack(level.getServer(), owner, portal);
    }

    public static int closeOwned(MinecraftServer server, UUID owner, Set<UUID> excluded) {
        return STORE.visit(server, owner, excluded, PortalEntity::getUUID,
            portal -> isLive(server, owner, portal), PortalEntity::startClosing);
    }

    public static List<PortalEntity> owned(MinecraftServer server, UUID owner) {
        return STORE.matching(server, owner, portal -> isLive(server, owner, portal));
    }

    public static int closeOwnedMatching(MinecraftServer server, UUID owner,
                                         Predicate<PortalEntity> predicate) {
        int closed = 0;
        for (PortalEntity portal : owned(server, owner)) {
            if (!predicate.test(portal)) continue;
            portal.startClosing();
            closed++;
        }
        return closed;
    }

    public static void clear(MinecraftServer server) {
        STORE.clear(server);
    }

    private static boolean isLive(MinecraftServer server, UUID owner, PortalEntity portal) {
        return !portal.isRemoved()
            && owner.equals(portal.ownerId())
            && portal.level() instanceof ServerLevel level
            && level.getServer() == server;
    }

    static final class Store<S, P> {
        private final Map<S, Map<UUID, Set<P>>> byServer = new IdentityHashMap<>();

        void track(S server, UUID owner, P portal) {
            byServer.computeIfAbsent(server, ignored -> new java.util.HashMap<>())
                .computeIfAbsent(owner,
                    ignored -> Collections.newSetFromMap(new IdentityHashMap<>()))
                .add(portal);
        }

        void untrack(S server, UUID owner, P portal) {
            Map<UUID, Set<P>> owners = byServer.get(server);
            if (owners == null) return;
            Set<P> portals = owners.get(owner);
            if (portals == null) return;
            portals.remove(portal);
            removeEmpty(server, owner, owners, portals);
        }

        int visit(S server, UUID owner, Set<UUID> excluded, Function<P, UUID> id,
                  Predicate<P> live, Consumer<P> visitor) {
            Map<UUID, Set<P>> owners = byServer.get(server);
            if (owners == null) return 0;
            Set<P> portals = owners.get(owner);
            if (portals == null) return 0;

            int visited = 0;
            var iterator = portals.iterator();
            while (iterator.hasNext()) {
                P portal = iterator.next();
                visited++;
                if (!live.test(portal)) {
                    iterator.remove();
                } else if (!excluded.contains(id.apply(portal))) {
                    visitor.accept(portal);
                }
            }
            removeEmpty(server, owner, owners, portals);
            return visited;
        }

        void clear(S server) {
            byServer.remove(server);
        }

        int indexedCount(S server, UUID owner) {
            Map<UUID, Set<P>> owners = byServer.get(server);
            if (owners == null) return 0;
            Set<P> portals = owners.get(owner);
            return portals == null ? 0 : portals.size();
        }

        List<P> matching(S server, UUID owner, Predicate<P> predicate) {
            Map<UUID, Set<P>> owners = byServer.get(server);
            if (owners == null) return List.of();
            Set<P> portals = owners.get(owner);
            if (portals == null) return List.of();
            return portals.stream().filter(predicate).toList();
        }

        private void removeEmpty(S server, UUID owner, Map<UUID, Set<P>> owners, Set<P> portals) {
            if (!portals.isEmpty()) return;
            owners.remove(owner);
            if (owners.isEmpty()) byServer.remove(server);
        }
    }

    private PortalOwnerIndex() {}
}
