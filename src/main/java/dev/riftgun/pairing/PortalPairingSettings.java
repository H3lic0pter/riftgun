package dev.riftgun.pairing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftgun.remote.RemoteSettings;
import java.util.Optional;

/** Persisted per-gun Portal Pairing preferences. */
public record PortalPairingSettings(
    PortalFunctionMode functionMode,
    PortalFloatingFallback pairingSmartFallback,
    RemoteSettings remote
) {
    public static final Codec<PortalPairingSettings> CODEC = Stored.CODEC.xmap(
        Stored::toSettings, Stored::fromSettings);

    public PortalPairingSettings {
        if (functionMode == null) functionMode = PortalFunctionMode.COORDINATE_TRAVEL;
        if (pairingSmartFallback == null) pairingSmartFallback = PortalFloatingFallback.FRONT;
        if (remote == null) remote = RemoteSettings.defaults();
    }

    public static PortalPairingSettings defaults() {
        return new PortalPairingSettings(PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.FRONT, RemoteSettings.defaults());
    }

    public PortalPairingSettings withFunctionMode(PortalFunctionMode value) {
        return new PortalPairingSettings(value, pairingSmartFallback, remote);
    }

    public PortalPairingSettings withCoordinateSmartFallback(PortalFloatingFallback value) {
        return new PortalPairingSettings(functionMode, pairingSmartFallback,
            remote.withCoordinateSmartFallback(value));
    }

    public PortalPairingSettings withPairingSmartFallback(PortalFloatingFallback value) {
        return new PortalPairingSettings(functionMode, value, remote);
    }

    public PortalPairingSettings withRemote(RemoteSettings value) {
        return new PortalPairingSettings(functionMode, pairingSmartFallback, value);
    }

    public PortalFloatingFallback coordinateSmartFallback() {
        return remote.coordinateSmartFallback();
    }

    public PortalFloatingFallback smartFallback() {
        return functionMode == PortalFunctionMode.PORTAL_PAIRING
            ? pairingSmartFallback : coordinateSmartFallback();
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

    /** Reads the legacy flat fallback while writing both legacy and grouped Remote data. */
    private record Stored(
        PortalFunctionMode functionMode,
        PortalFloatingFallback coordinateSmartFallback,
        PortalFloatingFallback pairingSmartFallback,
        Optional<RemoteSettings> remote
    ) {
        private static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("function_mode", PortalFunctionMode.COORDINATE_TRAVEL.name())
                .xmap(PortalPairingSettings::parseFunction, PortalFunctionMode::name)
                .forGetter(Stored::functionMode),
            Codec.STRING.optionalFieldOf("coordinate_smart_fallback", PortalFloatingFallback.FRONT.name())
                .xmap(PortalPairingSettings::parseFallback, PortalFloatingFallback::name)
                .forGetter(Stored::coordinateSmartFallback),
            Codec.STRING.optionalFieldOf("pairing_smart_fallback", PortalFloatingFallback.FRONT.name())
                .xmap(PortalPairingSettings::parseFallback, PortalFloatingFallback::name)
                .forGetter(Stored::pairingSmartFallback),
            RemoteSettings.CODEC.optionalFieldOf("remote").forGetter(Stored::remote)
        ).apply(instance, Stored::new));

        PortalPairingSettings toSettings() {
            return new PortalPairingSettings(functionMode, pairingSmartFallback,
                remote.orElseGet(() -> new RemoteSettings(coordinateSmartFallback, true, true)));
        }

        static Stored fromSettings(PortalPairingSettings settings) {
            return new Stored(settings.functionMode(), settings.coordinateSmartFallback(),
                settings.pairingSmartFallback(), Optional.of(settings.remote()));
        }
    }
}
