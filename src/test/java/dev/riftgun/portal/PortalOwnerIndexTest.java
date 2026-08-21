package dev.riftgun.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PortalOwnerIndexTest {
    @Test
    void lifecycleTrackUntrackAndServerClearRemoveEntries() {
        var store = new PortalOwnerIndex.Store<Object, FakePortal>();
        Object server = new Object();
        UUID owner = UUID.randomUUID();
        FakePortal portal = portal("overworld");

        store.track(server, owner, portal);
        store.track(server, owner, portal);
        assertEquals(1, store.indexedCount(server, owner));

        store.untrack(server, owner, portal);
        assertEquals(0, store.indexedCount(server, owner));

        store.track(server, owner, portal);
        store.clear(server);
        assertEquals(0, store.indexedCount(server, owner));
    }

    @Test
    void partitionsUseServerIdentityAndOwnerIsolation() {
        var store = new PortalOwnerIndex.Store<EqualServer, FakePortal>();
        EqualServer first = new EqualServer();
        EqualServer second = new EqualServer();
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        FakePortal firstPortal = portal("overworld");
        FakePortal secondPortal = portal("overworld");
        FakePortal otherPortal = portal("overworld");

        store.track(first, owner, firstPortal);
        store.track(second, owner, secondPortal);
        store.track(first, otherOwner, otherPortal);

        Set<UUID> closed = new HashSet<>();
        int visited = visit(store, first, owner, Set.of(), closed);
        assertEquals(1, visited);
        assertEquals(Set.of(firstPortal.id), closed);
        assertFalse(closed.contains(secondPortal.id));
        assertFalse(closed.contains(otherPortal.id));
    }

    @Test
    void crossDimensionOldPairClosesWhileNewPairIdsStayExcluded() {
        var store = new PortalOwnerIndex.Store<Object, FakePortal>();
        Object server = new Object();
        UUID owner = UUID.randomUUID();
        FakePortal oldEntry = portal("overworld");
        FakePortal oldExit = portal("the_nether");
        FakePortal newEntry = portal("overworld");
        FakePortal newExit = portal("the_end");
        for (FakePortal portal : new FakePortal[] {oldEntry, oldExit, newEntry, newExit}) {
            store.track(server, owner, portal);
        }

        Set<UUID> closed = new HashSet<>();
        int visited = visit(store, server, owner, Set.of(newEntry.id, newExit.id), closed);

        assertEquals(4, visited);
        assertEquals(Set.of(oldEntry.id, oldExit.id), closed);
    }

    @Test
    void lookupPrunesStaleEntries() {
        var store = new PortalOwnerIndex.Store<Object, FakePortal>();
        Object server = new Object();
        UUID owner = UUID.randomUUID();
        FakePortal live = portal("overworld");
        FakePortal stale = portal("the_nether");
        stale.live = false;
        store.track(server, owner, live);
        store.track(server, owner, stale);

        Set<UUID> closed = new HashSet<>();
        assertEquals(2, visit(store, server, owner, Set.of(), closed));
        assertEquals(Set.of(live.id), closed);
        assertEquals(1, store.indexedCount(server, owner));
    }

    @Test
    void lookupWorkDependsOnlyOnRequestedOwnersPortalCount() {
        var store = new PortalOwnerIndex.Store<Object, FakePortal>();
        Object server = new Object();
        UUID owner = UUID.randomUUID();
        UUID unrelatedOwner = UUID.randomUUID();
        store.track(server, owner, portal("overworld"));
        store.track(server, owner, portal("the_nether"));
        for (int i = 0; i < 1_000; i++) {
            store.track(server, unrelatedOwner, portal("unrelated_" + i));
        }

        AtomicInteger closed = new AtomicInteger();
        int visited = store.visit(server, owner, Set.of(), portal -> portal.id,
            portal -> portal.live, portal -> closed.incrementAndGet());

        assertEquals(2, visited);
        assertEquals(2, closed.get());
    }

    @Test
    void lifecycleAndBothNodesAreWiredWithoutAWorldScan() throws IOException {
        String lifecycle = Files.readString(
            Path.of("src/main/java/dev/riftgun/lifecycle/RiftLifecycle.java"));
        assertTrue(lifecycle.contains("PortalOwnerIndex.track(portal)"));
        assertTrue(lifecycle.contains("PortalOwnerIndex.untrack(portal)"));
        assertTrue(lifecycle.contains("PortalOwnerIndex.clear(server)"));

        for (String node : new String[] {"1.21.1", "26.1.2"}) {
            String source = Files.readString(Path.of(
                "versions", node, "src/main/java/dev/riftgun/portal/PortalEntity.java"));
            int close = source.indexOf("private static void closeOwnedPortals");
            int data = source.indexOf("@Override", close);
            String closePath = source.substring(close, data);
            assertTrue(closePath.contains("PortalOwnerIndex.closeOwned(server, owner, excluded)"));
            assertFalse(closePath.contains("getAllEntities"));
            assertFalse(closePath.contains("getAllLevels"));
        }
    }

    private static <S> int visit(PortalOwnerIndex.Store<S, FakePortal> store, S server,
                                 UUID owner, Set<UUID> excluded, Set<UUID> closed) {
        return store.visit(server, owner, excluded, portal -> portal.id,
            portal -> portal.live, portal -> closed.add(portal.id));
    }

    private static FakePortal portal(String dimension) {
        return new FakePortal(UUID.randomUUID(), dimension);
    }

    private static final class FakePortal {
        private final UUID id;
        @SuppressWarnings("unused")
        private final String dimension;
        private boolean live = true;

        private FakePortal(UUID id, String dimension) {
            this.id = id;
            this.dimension = dimension;
        }
    }

    private static final class EqualServer {
        @Override
        public boolean equals(Object ignored) {
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
