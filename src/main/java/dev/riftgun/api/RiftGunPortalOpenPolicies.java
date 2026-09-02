package dev.riftgun.api;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Process-wide registration point for composable portal-open policies. */
public final class RiftGunPortalOpenPolicies {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<RiftResourceId, RiftGunPortalOpenPolicy> POLICIES = new LinkedHashMap<>();
    private static final Set<RiftResourceId> WARNED_POLICIES = ConcurrentHashMap.newKeySet();

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
        for (var entry : snapshot().entrySet()) {
            try {
                PortalOpenPolicyDecision decision = Objects.requireNonNull(
                    entry.getValue().evaluate(opener), "policy decision");
                if (!decision.allowed()) return decision;
            } catch (RuntimeException exception) {
                if (WARNED_POLICIES.add(entry.getKey())) {
                    LOGGER.warn("Rift Gun portal-open policy '{}' failed; denying the request",
                        entry.getKey(), exception);
                }
                return PortalOpenPolicyDecision.deny(
                    Component.translatable("message.riftgun.portal_open_policy_failed"));
            }
        }
        return PortalOpenPolicyDecision.allow();
    }

    private static synchronized Map<RiftResourceId, RiftGunPortalOpenPolicy> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(POLICIES));
    }

    private RiftGunPortalOpenPolicies() {}
}
