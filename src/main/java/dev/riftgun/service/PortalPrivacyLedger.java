package dev.riftgun.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ephemeral, server-lifetime state for request-scoped Player Portal authorization. */
final class PortalPrivacyLedger {
    private final Map<RequestKey, Long> pendingUntil = new HashMap<>();
    private final Map<RequestKey, Long> grantsUntil = new HashMap<>();

    boolean prompt(UUID targetId, UUID requesterId, long now, long ttlTicks) {
        RequestKey key = new RequestKey(targetId, requesterId);
        Long until = pendingUntil.get(key);
        if (until != null && now <= until) return false;
        pendingUntil.put(key, expiresAt(now, ttlTicks));
        removeExpired(now);
        return true;
    }

    boolean allowOnce(UUID targetId, UUID requesterId, long now, long ttlTicks) {
        RequestKey key = new RequestKey(targetId, requesterId);
        Long pending = pendingUntil.remove(key);
        if (pending == null || now > pending) return false;
        grantsUntil.put(key, expiresAt(now, ttlTicks));
        removeExpired(now);
        return true;
    }

    boolean consume(UUID targetId, UUID requesterId, long now) {
        RequestKey key = new RequestKey(targetId, requesterId);
        Long until = grantsUntil.remove(key);
        return until != null && now <= until;
    }

    void clearPrompt(UUID targetId, UUID requesterId) {
        pendingUntil.remove(new RequestKey(targetId, requesterId));
    }

    private void removeExpired(long now) {
        pendingUntil.values().removeIf(until -> now > until);
        grantsUntil.values().removeIf(until -> now > until);
    }

    private static long expiresAt(long now, long ttlTicks) {
        long safeTtl = Math.max(1L, ttlTicks);
        return now > Long.MAX_VALUE - safeTtl ? Long.MAX_VALUE : now + safeTtl;
    }

    record RequestKey(UUID targetId, UUID requesterId) {}
}
