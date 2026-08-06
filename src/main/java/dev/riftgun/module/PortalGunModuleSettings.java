package dev.riftgun.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalOpenDuration;

public record PortalGunModuleSettings(
    int smartDistance,
    int desiredSurfaceRange,
    boolean passiveTransitEnabled,
    boolean hostileTransitEnabled,
    boolean bossTransitEnabled,
    int portalDurationSeconds,
    boolean expandedApertureEnabled
) {
    public static final int DEFAULT_SMART_DISTANCE = 8;
    public static final Codec<PortalGunModuleSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("smart_distance", DEFAULT_SMART_DISTANCE)
            .forGetter(PortalGunModuleSettings::smartDistance),
        Codec.INT.optionalFieldOf("desired_surface_range", PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE)
            .forGetter(PortalGunModuleSettings::desiredSurfaceRange),
        Codec.BOOL.optionalFieldOf("passive_transit_enabled", true)
            .forGetter(PortalGunModuleSettings::passiveTransitEnabled),
        Codec.BOOL.optionalFieldOf("hostile_transit_enabled", true)
            .forGetter(PortalGunModuleSettings::hostileTransitEnabled),
        Codec.BOOL.optionalFieldOf("boss_transit_enabled", true)
            .forGetter(PortalGunModuleSettings::bossTransitEnabled),
        Codec.INT.optionalFieldOf("portal_duration_seconds", PortalOpenDuration.DEFAULT_SECONDS)
            .forGetter(PortalGunModuleSettings::portalDurationSeconds),
        Codec.BOOL.optionalFieldOf("expanded_aperture_enabled", true)
            .forGetter(PortalGunModuleSettings::expandedApertureEnabled)
    ).apply(instance, PortalGunModuleSettings::new));

    public PortalGunModuleSettings {
        smartDistance = Math.max(1, smartDistance);
        desiredSurfaceRange = Math.max(1, desiredSurfaceRange);
        portalDurationSeconds = Math.max(PortalOpenDuration.MINIMUM_SECONDS, portalDurationSeconds);
    }

    public static PortalGunModuleSettings defaults(int legacySmartDistance) {
        return new PortalGunModuleSettings(Math.max(1, legacySmartDistance),
            PortalModuleRules.DEFAULT_BASE_SURFACE_RANGE, true, true, true,
            PortalOpenDuration.DEFAULT_SECONDS, true);
    }

    public static PortalGunModuleSettings get(ItemStack gun, int legacySmartDistance) {
        return gun.getOrDefault(PortalGunComponents.MODULE_SETTINGS, defaults(legacySmartDistance));
    }

    public static PortalGunModuleSettings ensure(ItemStack gun, int legacySmartDistance) {
        PortalGunModuleSettings settings = gun.get(PortalGunComponents.MODULE_SETTINGS);
        if (settings != null) return settings;
        settings = defaults(legacySmartDistance);
        gun.set(PortalGunComponents.MODULE_SETTINGS, settings);
        return settings;
    }

    public void save(ItemStack gun) {
        gun.set(PortalGunComponents.MODULE_SETTINGS, this);
    }

    public PortalGunModuleSettings withSmartDistance(int value) {
        return new PortalGunModuleSettings(value, desiredSurfaceRange,
            passiveTransitEnabled, hostileTransitEnabled, bossTransitEnabled,
            portalDurationSeconds, expandedApertureEnabled);
    }

    public PortalGunModuleSettings withDesiredSurfaceRange(int value) {
        return new PortalGunModuleSettings(smartDistance, value,
            passiveTransitEnabled, hostileTransitEnabled, bossTransitEnabled,
            portalDurationSeconds, expandedApertureEnabled);
    }

    public PortalGunModuleSettings withTransit(PortalModuleKind kind, boolean enabled) {
        return switch (kind) {
            case PASSIVE_TRANSIT -> new PortalGunModuleSettings(smartDistance, desiredSurfaceRange,
                enabled, hostileTransitEnabled, bossTransitEnabled, portalDurationSeconds,
                expandedApertureEnabled);
            case HOSTILE_TRANSIT -> new PortalGunModuleSettings(smartDistance, desiredSurfaceRange,
                passiveTransitEnabled, enabled, bossTransitEnabled, portalDurationSeconds,
                expandedApertureEnabled);
            case BOSS_TRANSIT -> new PortalGunModuleSettings(smartDistance, desiredSurfaceRange,
                passiveTransitEnabled, hostileTransitEnabled, enabled, portalDurationSeconds,
                expandedApertureEnabled);
            default -> this;
        };
    }

    public PortalGunModuleSettings withPortalDurationSeconds(int value) {
        return new PortalGunModuleSettings(smartDistance, desiredSurfaceRange,
            passiveTransitEnabled, hostileTransitEnabled, bossTransitEnabled,
            value, expandedApertureEnabled);
    }

    public PortalGunModuleSettings withExpandedApertureEnabled(boolean enabled) {
        return new PortalGunModuleSettings(smartDistance, desiredSurfaceRange,
            passiveTransitEnabled, hostileTransitEnabled, bossTransitEnabled,
            portalDurationSeconds, enabled);
    }
}
