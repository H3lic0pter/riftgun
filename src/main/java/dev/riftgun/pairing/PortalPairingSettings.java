package dev.riftgun.pairing;

/** Persisted per-gun preferences owned by Portal Pairing. */
public record PortalPairingSettings(
    PortalFunctionMode functionMode,
    PortalFloatingFallback smartFallback
) {
    public PortalPairingSettings {
        if (functionMode == null) functionMode = PortalFunctionMode.COORDINATE_TRAVEL;
        if (smartFallback == null) smartFallback = PortalFloatingFallback.FRONT;
    }

    public static PortalPairingSettings defaults() {
        return new PortalPairingSettings(
            PortalFunctionMode.COORDINATE_TRAVEL, PortalFloatingFallback.FRONT);
    }

    public PortalPairingSettings withFunctionMode(PortalFunctionMode value) {
        return new PortalPairingSettings(value, smartFallback);
    }

    public PortalPairingSettings withSmartFallback(PortalFloatingFallback value) {
        return new PortalPairingSettings(functionMode, value);
    }
}
