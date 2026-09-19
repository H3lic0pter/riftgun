package dev.riftgun.portal;

import com.mojang.serialization.Codec;
import java.util.Locale;

/** Per-gun preference for unobstructed attached portals; collision fallback is unchanged. */
public enum SurfacePortalSize {
    FULL_SUPPORT, ADAPTIVE, PREFER_LARGE;

    public static final Codec<SurfacePortalSize> CODEC = Codec.STRING.xmap(SurfacePortalSize::parse, SurfacePortalSize::id);
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "screen.riftgun.surface_portal_size." + id(); }
    public SurfacePortalSize next() { return values()[(ordinal() + 1) % values().length]; }
    public static SurfacePortalSize parse(String value) {
        for (var mode : values()) if (mode.id().equals(value)) return mode;
        return ADAPTIVE;
    }
    public PortalAperture aperture() {
        return switch (this) {
            case FULL_SUPPORT -> PortalAperture.EXPANDED;
            case ADAPTIVE -> PortalAperture.EXPANDED_ADAPTIVE;
            case PREFER_LARGE -> PortalAperture.EXPANDED_PREFER_LARGE;
        };
    }
}
