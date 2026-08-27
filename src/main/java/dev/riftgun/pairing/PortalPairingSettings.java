package dev.riftgun.pairing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persisted per-gun Portal Pairing preferences. */
public record PortalPairingSettings(
    PortalFunctionMode functionMode,
    PortalFloatingFallback coordinateSmartFallback,
    PortalFloatingFallback pairingSmartFallback
) {
    public static final Codec<PortalPairingSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("function_mode", PortalFunctionMode.COORDINATE_TRAVEL.name())
            .xmap(PortalPairingSettings::parseFunction, PortalFunctionMode::name)
            .forGetter(PortalPairingSettings::functionMode),
        Codec.STRING.optionalFieldOf("coordinate_smart_fallback", PortalFloatingFallback.FRONT.name())
            .xmap(PortalPairingSettings::parseFallback, PortalFloatingFallback::name)
            .forGetter(PortalPairingSettings::coordinateSmartFallback),
        Codec.STRING.optionalFieldOf("pairing_smart_fallback", PortalFloatingFallback.FRONT.name())
            .xmap(PortalPairingSettings::parseFallback, PortalFloatingFallback::name)
            .forGetter(PortalPairingSettings::pairingSmartFallback)
    ).apply(instance, PortalPairingSettings::new));

    public PortalPairingSettings {
        if (functionMode == null) functionMode = PortalFunctionMode.COORDINATE_TRAVEL;
        if (coordinateSmartFallback == null) coordinateSmartFallback = PortalFloatingFallback.FRONT;
        if (pairingSmartFallback == null) pairingSmartFallback = PortalFloatingFallback.FRONT;
    }

    public static PortalPairingSettings defaults() {
        return new PortalPairingSettings(PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.FRONT, PortalFloatingFallback.FRONT);
    }

    public PortalPairingSettings withFunctionMode(PortalFunctionMode value) {
        return new PortalPairingSettings(value, coordinateSmartFallback, pairingSmartFallback);
    }

    public PortalPairingSettings withCoordinateSmartFallback(PortalFloatingFallback value) {
        return new PortalPairingSettings(functionMode, value, pairingSmartFallback);
    }

    public PortalPairingSettings withPairingSmartFallback(PortalFloatingFallback value) {
        return new PortalPairingSettings(functionMode, coordinateSmartFallback, value);
    }

    public PortalFloatingFallback smartFallback() {
        return functionMode == PortalFunctionMode.PORTAL_PAIRING
            ? pairingSmartFallback : coordinateSmartFallback;
    }

    private static PortalFunctionMode parseFunction(String value) {
        try {
            return PortalFunctionMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return PortalFunctionMode.COORDINATE_TRAVEL;
        }
    }

    private static PortalFloatingFallback parseFallback(String value) {
        try {
            return PortalFloatingFallback.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return PortalFloatingFallback.FRONT;
        }
    }
}
