package dev.riftgun.api;

import java.util.Objects;
import net.minecraft.network.chat.Component;

/** Structured result returned without taking over caller UI or control flow. */
public record PortalOpenResult(PortalOpenStatus status, Component message) {
    public PortalOpenResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(message, "message");
    }

    public static PortalOpenResult success() {
        return new PortalOpenResult(PortalOpenStatus.OPENED, Component.empty());
    }

    public static PortalOpenResult rejected(PortalOpenStatus status, Component message) {
        if (status == PortalOpenStatus.OPENED) {
            throw new IllegalArgumentException("Use success() for an opened portal");
        }
        return new PortalOpenResult(status, message);
    }

    public boolean opened() {
        return status == PortalOpenStatus.OPENED;
    }
}
