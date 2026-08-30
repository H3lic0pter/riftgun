package dev.riftgun.pairing;

import com.mojang.serialization.Codec;

public enum PortalFloatingFallback {
    FRONT,
    REMOTE;

    public static final Codec<PortalFloatingFallback> CODEC = Codec.STRING.xmap(
        PortalFloatingFallback::byName, PortalFloatingFallback::name);

    private static PortalFloatingFallback byName(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return FRONT;
        }
    }
}
