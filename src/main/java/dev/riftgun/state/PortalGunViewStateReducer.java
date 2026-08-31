package dev.riftgun.state;

import dev.riftgun.pairing.PortalFloatingFallback;

/** Central optimistic-update policy; server snapshots always replace its result. */
public final class PortalGunViewStateReducer {
    public static boolean booleanValue(PortalGunViewState state, BooleanSetting setting) {
        return switch (setting) {
            case PLAYER_TARGET -> state.playerTargetEnabled();
            case EXPANDED_APERTURE -> state.expandedApertureEnabled();
            case FALL_GUARD -> state.fallGuardEnabled();
            case FALL_GUARD_ENTITIES -> state.entityFallGuardEnabled();
            case ENTITY_RELOCATION -> state.entityRelocationEnabled();
            case ENTITY_RELOCATION_SMART_ROUTING -> state.entityRelocationSmartRouting();
            case REMOTE_SCROLL_ADJUSTMENT -> state.remoteScrollAdjustmentEnabled();
            case REMOTE_RADIAL_SLIDER -> state.remoteRadialSliderEnabled();
            case REMOTE_PLACEMENT_PREVIEW -> state.remotePreviewEnabled();
            case PASSIVE_TRANSIT -> state.passiveTransitEnabled();
            case HOSTILE_TRANSIT -> state.hostileTransitEnabled();
            case BOSS_TRANSIT -> state.bossTransitEnabled();
            case PROJECTILE_TRANSIT -> state.projectileTransitEnabled();
        };
    }

    public static PortalGunViewState withBoolean(PortalGunViewState state, BooleanSetting setting,
                                                 boolean value) {
        var placement = state.placement();
        switch (setting) {
            case REMOTE_SCROLL_ADJUSTMENT -> placement = new PortalGunViewState.Placement(
                placement.maximumSurfaceRange(), placement.remoteDistance(), placement.smartDistance(),
                placement.remoteInstalled(), value, placement.remoteRadialSliderEnabled(),
                placement.remotePreviewEnabled(), placement.precisionInstalled(),
                placement.pairingInstalled(), placement.functionMode(),
                placement.coordinateSmartFallback(), placement.pairingSmartFallback());
            case REMOTE_RADIAL_SLIDER -> placement = new PortalGunViewState.Placement(
                placement.maximumSurfaceRange(), placement.remoteDistance(), placement.smartDistance(),
                placement.remoteInstalled(), placement.remoteScrollAdjustmentEnabled(), value,
                placement.remotePreviewEnabled(), placement.precisionInstalled(),
                placement.pairingInstalled(), placement.functionMode(),
                placement.coordinateSmartFallback(), placement.pairingSmartFallback());
            case REMOTE_PLACEMENT_PREVIEW -> placement = new PortalGunViewState.Placement(
                placement.maximumSurfaceRange(), placement.remoteDistance(), placement.smartDistance(),
                placement.remoteInstalled(), placement.remoteScrollAdjustmentEnabled(),
                placement.remoteRadialSliderEnabled(), value, placement.precisionInstalled(),
                placement.pairingInstalled(), placement.functionMode(),
                placement.coordinateSmartFallback(), placement.pairingSmartFallback());
            default -> {
                return state.withTransit(transitWith(state.transit(), setting, value));
            }
        }
        return state.withPlacement(placement);
    }

    public static PortalFloatingFallback fallbackValue(PortalGunViewState state,
                                                        FallbackSetting setting) {
        return switch (setting) {
            case COORDINATE_SMART -> state.placement().coordinateSmartFallback();
            case PAIRING_SMART -> state.placement().pairingSmartFallback();
        };
    }

    public static PortalGunViewState withFallback(PortalGunViewState state, FallbackSetting setting,
                                                  PortalFloatingFallback value) {
        var p = state.placement();
        return state.withPlacement(new PortalGunViewState.Placement(
            p.maximumSurfaceRange(), p.remoteDistance(), p.smartDistance(), p.remoteInstalled(),
            p.remoteScrollAdjustmentEnabled(), p.remoteRadialSliderEnabled(), p.remotePreviewEnabled(),
            p.precisionInstalled(), p.pairingInstalled(), p.functionMode(),
            setting == FallbackSetting.COORDINATE_SMART ? value : p.coordinateSmartFallback(),
            setting == FallbackSetting.PAIRING_SMART ? value : p.pairingSmartFallback()));
    }

    public static PortalGunViewState stepPlayerExclude(PortalGunViewState state, int amount) {
        var t = state.transit();
        return state.withTransit(new PortalGunViewState.Transit(
            t.entityAccessMask(), t.passiveEnabled(), t.hostileEnabled(), t.bossEnabled(),
            t.projectileEnabled(), t.portalDurationSeconds(), t.maximumPortalDurationSeconds(),
            t.eternalDurationInstalled(), t.expandedApertureEnabled(), t.transitCooldownTenths(),
            t.maximumTransitCooldownTenths(), t.playerTargetInstalled(), t.playerTargetEnabled(),
            t.playerExcludeMode().step(amount), t.fallGuardInstalled(), t.fallGuardEnabled(),
            t.entityFallGuardEnabled(), t.entityRelocationInstalled(), t.entityRelocationEnabled(),
            t.entityRelocationSmartRouting()));
    }

    public static PortalGunViewState withDistance(PortalGunViewState state,
                                                  DistanceSetting setting, int value) {
        var p = state.placement();
        if (setting == DistanceSetting.SMART_DISTANCE || setting == DistanceSetting.REMOTE_DISTANCE) {
            return state.withPlacement(new PortalGunViewState.Placement(
                p.maximumSurfaceRange(), setting == DistanceSetting.REMOTE_DISTANCE
                    ? value : p.remoteDistance(), setting == DistanceSetting.SMART_DISTANCE
                    ? value : p.smartDistance(), p.remoteInstalled(),
                p.remoteScrollAdjustmentEnabled(), p.remoteRadialSliderEnabled(), p.remotePreviewEnabled(),
                p.precisionInstalled(), p.pairingInstalled(), p.functionMode(),
                p.coordinateSmartFallback(), p.pairingSmartFallback()));
        }
        var t = state.transit();
        return state.withTransit(new PortalGunViewState.Transit(
            t.entityAccessMask(), t.passiveEnabled(), t.hostileEnabled(), t.bossEnabled(),
            t.projectileEnabled(), setting == DistanceSetting.PORTAL_DURATION
                ? value : t.portalDurationSeconds(),
            t.maximumPortalDurationSeconds(), t.eternalDurationInstalled(),
            t.expandedApertureEnabled(), setting == DistanceSetting.TRANSIT_COOLDOWN
                ? value : t.transitCooldownTenths(), t.maximumTransitCooldownTenths(),
            t.playerTargetInstalled(), t.playerTargetEnabled(), t.playerExcludeMode(),
            t.fallGuardInstalled(), t.fallGuardEnabled(), t.entityFallGuardEnabled(),
            t.entityRelocationInstalled(), t.entityRelocationEnabled(),
            t.entityRelocationSmartRouting()));
    }

    private static PortalGunViewState.Transit transitWith(PortalGunViewState.Transit t,
                                                           BooleanSetting setting, boolean value) {
        return new PortalGunViewState.Transit(
            t.entityAccessMask(),
            setting == BooleanSetting.PASSIVE_TRANSIT ? value : t.passiveEnabled(),
            setting == BooleanSetting.HOSTILE_TRANSIT ? value : t.hostileEnabled(),
            setting == BooleanSetting.BOSS_TRANSIT ? value : t.bossEnabled(),
            setting == BooleanSetting.PROJECTILE_TRANSIT ? value : t.projectileEnabled(),
            t.portalDurationSeconds(), t.maximumPortalDurationSeconds(), t.eternalDurationInstalled(),
            setting == BooleanSetting.EXPANDED_APERTURE ? value : t.expandedApertureEnabled(),
            t.transitCooldownTenths(), t.maximumTransitCooldownTenths(), t.playerTargetInstalled(),
            setting == BooleanSetting.PLAYER_TARGET ? value : t.playerTargetEnabled(),
            t.playerExcludeMode(), t.fallGuardInstalled(),
            setting == BooleanSetting.FALL_GUARD ? value : t.fallGuardEnabled(),
            setting == BooleanSetting.FALL_GUARD_ENTITIES ? value : t.entityFallGuardEnabled(),
            t.entityRelocationInstalled(),
            setting == BooleanSetting.ENTITY_RELOCATION ? value : t.entityRelocationEnabled(),
            setting == BooleanSetting.ENTITY_RELOCATION_SMART_ROUTING
                ? value : t.entityRelocationSmartRouting());
    }

    public enum BooleanSetting {
        PLAYER_TARGET("PlayerTarget"),
        EXPANDED_APERTURE("ExpandedAperture"),
        FALL_GUARD("FallGuard"),
        FALL_GUARD_ENTITIES("FallGuardEntities"),
        ENTITY_RELOCATION("EntityRelocation"),
        ENTITY_RELOCATION_SMART_ROUTING("EntityRelocationSmartRouting"),
        REMOTE_SCROLL_ADJUSTMENT("RemoteScrollAdjustment"),
        REMOTE_RADIAL_SLIDER("RemoteRadialSlider"),
        REMOTE_PLACEMENT_PREVIEW("RemotePlacementPreview"),
        PASSIVE_TRANSIT("PassiveTransit"),
        HOSTILE_TRANSIT("HostileTransit"),
        BOSS_TRANSIT("BossTransit"),
        PROJECTILE_TRANSIT("ProjectileTransit");

        private final String wireName;

        BooleanSetting(String wireName) { this.wireName = wireName; }
        public String wireName() { return wireName; }
    }

    public enum FallbackSetting {
        COORDINATE_SMART("CoordinateSmartFallback"),
        PAIRING_SMART("PairingSmartFallback");

        private final String wireName;

        FallbackSetting(String wireName) { this.wireName = wireName; }
        public String wireName() { return wireName; }
    }

    public enum DistanceSetting {
        PORTAL_DURATION("PortalDuration"),
        TRANSIT_COOLDOWN("TransitCooldown"),
        SMART_DISTANCE("SmartDistance"),
        REMOTE_DISTANCE("RemoteDistance");

        private final String wireName;

        DistanceSetting(String wireName) { this.wireName = wireName; }
        public String wireName() { return wireName; }
    }

    private PortalGunViewStateReducer() {}
}
