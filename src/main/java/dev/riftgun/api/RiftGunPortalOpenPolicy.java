package dev.riftgun.api;

import net.minecraft.server.level.ServerPlayer;

/** Addon-owned source-level gate; it never creates, closes, or transfers through portals. */
public interface RiftGunPortalOpenPolicy {
    RiftResourceId id();
    PortalOpenPolicyDecision evaluate(ServerPlayer opener);
}
