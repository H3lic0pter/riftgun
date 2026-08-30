package dev.riftgun.pairing;

import com.mojang.serialization.Codec;

public enum PortalFunctionMode {
    COORDINATE_TRAVEL,
    PORTAL_PAIRING;

    public static final Codec<PortalFunctionMode> CODEC = Codec.STRING.xmap(
        PortalFunctionMode::byName, PortalFunctionMode::name);

    public PortalFunctionMode toggle() {
        return this == COORDINATE_TRAVEL ? PORTAL_PAIRING : COORDINATE_TRAVEL;
    }

    private static PortalFunctionMode byName(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return COORDINATE_TRAVEL;
        }
    }
}
