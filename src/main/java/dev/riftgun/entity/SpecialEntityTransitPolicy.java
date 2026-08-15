package dev.riftgun.entity;

import java.util.Objects;
import java.util.Set;

/** Immutable, server-scoped decisions compiled from special-entity datapack tags. */
public final class SpecialEntityTransitPolicy<K> {
    private final AccessRules<K> portal;
    private final AccessRules<K> relocation;
    private final Set<K> portalSwept;

    private SpecialEntityTransitPolicy(
            AccessRules<K> portal,
            AccessRules<K> relocation,
            Set<K> portalSwept) {
        this.portal = Objects.requireNonNull(portal, "portal");
        this.relocation = Objects.requireNonNull(relocation, "relocation");
        this.portalSwept = Set.copyOf(portalSwept);
    }

    public static <K> SpecialEntityTransitPolicy<K> compile(
            AccessRules<K> portal,
            AccessRules<K> relocation,
            Set<K> portalSwept) {
        return new SpecialEntityTransitPolicy<>(
            portal,
            relocation,
            Objects.requireNonNull(portalSwept, "portalSwept"));
    }

    public boolean allowsPortal(K key, boolean conventionallyAllowed) {
        Objects.requireNonNull(key, "key");
        return portal.allows(key, conventionallyAllowed);
    }

    public boolean allowsRelocation(K key, boolean conventionallyAllowed) {
        Objects.requireNonNull(key, "key");
        return relocation.allows(key, conventionallyAllowed);
    }

    public boolean hasPortalDenials() {
        return portal.hasDenials();
    }

    public boolean hasRelocationDenials() {
        return relocation.hasDenials();
    }

    public boolean isSweptType(K key) {
        Objects.requireNonNull(key, "key");
        return portalSwept.contains(key)
            && (!portal.hasDenials() || !portal.denies(key));
    }

    public record AccessRules<K>(Set<K> allowed, Set<K> denied) {
        public AccessRules {
            allowed = Set.copyOf(Objects.requireNonNull(allowed, "allowed"));
            denied = Set.copyOf(Objects.requireNonNull(denied, "denied"));
        }

        boolean allows(K key, boolean conventionallyAllowed) {
            return !denied.contains(key) && (conventionallyAllowed || allowed.contains(key));
        }

        boolean hasDenials() {
            return !denied.isEmpty();
        }

        boolean denies(K key) {
            return denied.contains(key);
        }
    }
}
