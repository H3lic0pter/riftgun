package dev.riftgun.portal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

final class PortalTransitGate {
    private final Set<UUID> occupants = new HashSet<>();

    boolean enter(UUID entityId) {
        return occupants.add(entityId);
    }

    void markInside(UUID entityId) {
        occupants.add(entityId);
    }

    void leave(UUID entityId) {
        occupants.remove(entityId);
    }

    void retainInside(Collection<UUID> entityIds) {
        occupants.retainAll(entityIds);
    }

    boolean contains(UUID entityId) {
        return occupants.contains(entityId);
    }
}
