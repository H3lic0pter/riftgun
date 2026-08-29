package dev.riftgun.remote;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.riftgun.pairing.PortalFloatingFallback;

/** Persisted per-gun preferences owned by the Remote Module. */
public record RemoteSettings(
    PortalFloatingFallback coordinateSmartFallback,
    boolean scrollAdjustmentEnabled,
    boolean radialSliderEnabled,
    boolean placementPreviewEnabled
) {
    public static final Codec<RemoteSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.optionalFieldOf("coordinate_smart_fallback", PortalFloatingFallback.FRONT.name())
            .xmap(RemoteSettings::parseFallback, PortalFloatingFallback::name)
            .forGetter(RemoteSettings::coordinateSmartFallback),
        Codec.BOOL.optionalFieldOf("scroll_adjustment_enabled", true)
            .forGetter(RemoteSettings::scrollAdjustmentEnabled),
        Codec.BOOL.optionalFieldOf("radial_slider_enabled", true)
            .forGetter(RemoteSettings::radialSliderEnabled),
        Codec.BOOL.optionalFieldOf("placement_preview_enabled", true)
            .forGetter(RemoteSettings::placementPreviewEnabled)
    ).apply(instance, RemoteSettings::new));

    public RemoteSettings {
        if (coordinateSmartFallback == null) coordinateSmartFallback = PortalFloatingFallback.FRONT;
    }

    public static RemoteSettings defaults() {
        return new RemoteSettings(PortalFloatingFallback.FRONT, true, true, true);
    }

    public RemoteSettings withCoordinateSmartFallback(PortalFloatingFallback value) {
        return new RemoteSettings(value, scrollAdjustmentEnabled, radialSliderEnabled,
            placementPreviewEnabled);
    }

    public RemoteSettings withScrollAdjustmentEnabled(boolean value) {
        return new RemoteSettings(coordinateSmartFallback, value, radialSliderEnabled,
            placementPreviewEnabled);
    }

    public RemoteSettings withRadialSliderEnabled(boolean value) {
        return new RemoteSettings(coordinateSmartFallback, scrollAdjustmentEnabled, value,
            placementPreviewEnabled);
    }

    public RemoteSettings withPlacementPreviewEnabled(boolean value) {
        return new RemoteSettings(coordinateSmartFallback, scrollAdjustmentEnabled,
            radialSliderEnabled, value);
    }

    private static PortalFloatingFallback parseFallback(String value) {
        try {
            return PortalFloatingFallback.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return PortalFloatingFallback.FRONT;
        }
    }
}
