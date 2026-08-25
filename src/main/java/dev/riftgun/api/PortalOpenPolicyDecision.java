package dev.riftgun.api;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Addon policy result evaluated before Rift Gun creates an entry portal. */
public record PortalOpenPolicyDecision(boolean allowed, Component message) {
    public PortalOpenPolicyDecision {
        Objects.requireNonNull(message, "message");
    }

    public static PortalOpenPolicyDecision allow() {
        return new PortalOpenPolicyDecision(true, Component.empty());
    }

    public static PortalOpenPolicyDecision deny(Component message) {
        return new PortalOpenPolicyDecision(false, Objects.requireNonNull(message, "message"));
    }
}
