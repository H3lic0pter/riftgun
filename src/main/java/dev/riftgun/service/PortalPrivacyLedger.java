package dev.riftgun.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Ephemeral, server-lifetime state for request-scoped Player Portal authorization. */
final class PortalPrivacyLedger {
    private final Map<UUID, PendingRequest> pendingByToken = new HashMap<>();
    private final Map<RequestKey, UUID> pendingTokenByPair = new HashMap<>();
    private final Map<RequestKey, TimedParties> grants = new HashMap<>();
    private final Map<RequestKey, Long> denyOnceUntil = new HashMap<>();

    Prompt prompt(Parties parties, long now, long ttlTicks) {
        RequestKey key = parties.key();
        UUID existingToken = pendingTokenByPair.get(key);
        PendingRequest existing = existingToken == null ? null : pendingByToken.get(existingToken);
        if (existing != null && now < existing.expiresAt()) {
            return new Prompt(existing.token(), false);
        }
        removePending(key, existingToken);
        UUID token = UUID.randomUUID();
        PendingRequest pending = new PendingRequest(token, parties, expiresAt(now, ttlTicks));
        pendingByToken.put(token, pending);
        pendingTokenByPair.put(key, token);
        return new Prompt(token, true);
    }

    Resolution resolve(UUID token, UUID targetId, long now) {
        PendingRequest pending = pendingByToken.get(token);
        if (pending == null || !pending.parties().targetId().equals(targetId)) {
            return Resolution.missing();
        }
        if (now >= pending.expiresAt()) {
            removePending(pending.parties().key(), token);
            return Resolution.expired(pending.parties());
        }
        removePending(pending.parties().key(), token);
        return Resolution.active(pending.parties());
    }

    void grantOnce(Parties parties, long now, long ttlTicks) {
        grants.put(parties.key(), new TimedParties(parties, expiresAt(now, ttlTicks)));
        denyOnceUntil.remove(parties.key());
    }

    boolean hasGrant(RequestKey key, long now) {
        TimedParties grant = grants.get(key);
        if (grant == null) return false;
        if (now >= grant.expiresAt()) {
            grants.remove(key);
            return false;
        }
        return true;
    }

    boolean consumeGrant(RequestKey key, long now) {
        if (!hasGrant(key, now)) return false;
        grants.remove(key);
        return true;
    }

    void denyOnce(RequestKey key, long now, long ttlTicks) {
        if (ttlTicks <= 0L) {
            denyOnceUntil.remove(key);
            return;
        }
        denyOnceUntil.put(key, expiresAt(now, ttlTicks));
        grants.remove(key);
    }

    long denyRemainingTicks(RequestKey key, long now) {
        Long until = denyOnceUntil.get(key);
        if (until == null) return 0L;
        long remaining = until - now;
        if (remaining <= 0L) {
            denyOnceUntil.remove(key);
            return 0L;
        }
        return remaining;
    }

    void clearTransient(RequestKey key) {
        removePending(key, pendingTokenByPair.get(key));
        grants.remove(key);
        denyOnceUntil.remove(key);
    }

    void clearTarget(UUID targetId) {
        pendingByToken.entrySet().removeIf(entry ->
            entry.getValue().parties().targetId().equals(targetId));
        pendingTokenByPair.keySet().removeIf(key -> key.targetId().equals(targetId));
        grants.keySet().removeIf(key -> key.targetId().equals(targetId));
        denyOnceUntil.keySet().removeIf(key -> key.targetId().equals(targetId));
    }

    List<Expired> expire(long now) {
        List<Expired> expired = new ArrayList<>();
        var pendingIterator = pendingByToken.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            var entry = pendingIterator.next();
            PendingRequest pending = entry.getValue();
            if (now < pending.expiresAt()) continue;
            pendingIterator.remove();
            pendingTokenByPair.remove(pending.parties().key(), entry.getKey());
            expired.add(new Expired(Expiration.REQUEST, pending.parties()));
        }
        var grantIterator = grants.entrySet().iterator();
        while (grantIterator.hasNext()) {
            var entry = grantIterator.next();
            TimedParties grant = entry.getValue();
            if (now < grant.expiresAt()) continue;
            grantIterator.remove();
            expired.add(new Expired(Expiration.GRANT, grant.parties()));
        }
        denyOnceUntil.values().removeIf(until -> now >= until);
        return expired;
    }

    void clear() {
        pendingByToken.clear();
        pendingTokenByPair.clear();
        grants.clear();
        denyOnceUntil.clear();
    }

    private void removePending(RequestKey key, UUID token) {
        pendingTokenByPair.remove(key);
        if (token != null) pendingByToken.remove(token);
    }

    private static long expiresAt(long now, long ttlTicks) {
        long safeTtl = Math.max(1L, ttlTicks);
        return now > Long.MAX_VALUE - safeTtl ? Long.MAX_VALUE : now + safeTtl;
    }

    enum Expiration {
        REQUEST,
        GRANT
    }

    record Prompt(UUID token, boolean fresh) {}

    record Expired(Expiration expiration, Parties parties) {}

    record Resolution(ResolutionStatus status, Parties parties) {
        static Resolution active(Parties parties) {
            return new Resolution(ResolutionStatus.ACTIVE, parties);
        }

        static Resolution expired(Parties parties) {
            return new Resolution(ResolutionStatus.EXPIRED, parties);
        }

        static Resolution missing() {
            return new Resolution(ResolutionStatus.MISSING, null);
        }
    }

    enum ResolutionStatus {
        ACTIVE,
        EXPIRED,
        MISSING
    }

    record Parties(UUID targetId, String targetName, UUID requesterId, String requesterName,
                   PortalRequestPurpose purpose) {
        Parties(UUID targetId, String targetName, UUID requesterId, String requesterName) {
            this(targetId, targetName, requesterId, requesterName, PortalRequestPurpose.PORTAL);
        }

        RequestKey key() {
            return new RequestKey(targetId, requesterId, purpose);
        }
    }

    TimedParties reserveGrant(RequestKey key, long now) {
        if (!hasGrant(key, now)) return null;
        return grants.remove(key);
    }

    void restoreGrant(RequestKey key, TimedParties grant, long now) {
        if (grant != null && now < grant.expiresAt()) grants.putIfAbsent(key, grant);
    }

    record RequestKey(UUID targetId, UUID requesterId, PortalRequestPurpose purpose) {
        RequestKey(UUID targetId, UUID requesterId) {
            this(targetId, requesterId, PortalRequestPurpose.PORTAL);
        }
    }

    private record PendingRequest(UUID token, Parties parties, long expiresAt) {}

    record TimedParties(Parties parties, long expiresAt) {}
}
