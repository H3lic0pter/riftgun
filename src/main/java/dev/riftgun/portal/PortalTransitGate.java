package dev.riftgun.portal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class PortalTransitGate {
    private final Set<UUID> occupants = new HashSet<>();
    private final Map<UUID, Long> lastTransit = new HashMap<>();

    boolean enter(UUID entityId, long now, int cooldownTicks) {
        if (occupants.contains(entityId)) return false;
        if (cooldownTicks > 0) {
            Long last = lastTransit.get(entityId);
            if (last != null && now - last < cooldownTicks) return false;
        }
        occupants.add(entityId);
        if (cooldownTicks > 0) lastTransit.put(entityId, now);
        return true;
    }

    void markInside(UUID entityId, long now, int cooldownTicks) {
        if (cooldownTicks > 0) lastTransit.put(entityId, now);
        occupants.add(entityId);
    }

    void leave(UUID entityId) {
        occupants.remove(entityId);
    }

    void retainInside(Collection<UUID> entityIds, long now, int cooldownTicks) {
        occupants.retainAll(entityIds);
        if (cooldownTicks <= 0) {
            lastTransit.clear();
            return;
        }
        lastTransit.entrySet().removeIf(entry -> now - entry.getValue() >= cooldownTicks);
    }

    boolean contains(UUID entityId) {
        return occupants.contains(entityId);
    }

    int rememberedTransitCount() {
        return lastTransit.size();
    }
}
