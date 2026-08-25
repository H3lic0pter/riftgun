package dev.riftgun.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Process-wide registration point for composable portal-open policies. */
public final class RiftGunPortalOpenPolicies {
    private static final Map<RiftResourceId, RiftGunPortalOpenPolicy> POLICIES = new LinkedHashMap<>();

    public static synchronized void register(RiftGunPortalOpenPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        RiftResourceId id = Objects.requireNonNull(policy.id(), "policy.id()");
        if (POLICIES.putIfAbsent(id, policy) != null) {
            throw new IllegalStateException("Rift Gun portal-open policy already registered: " + id);
        }
    }

    public static synchronized List<RiftGunPortalOpenPolicy> policies() {
        return List.copyOf(POLICIES.values());
    }

    public static PortalOpenPolicyDecision evaluate(ServerPlayer opener) {
        for (RiftGunPortalOpenPolicy policy : policies()) {
            PortalOpenPolicyDecision decision = Objects.requireNonNull(
                policy.evaluate(opener), "policy decision");
            if (!decision.allowed()) return decision;
        }
        return PortalOpenPolicyDecision.allow();
    }

    private RiftGunPortalOpenPolicies() {}
}
