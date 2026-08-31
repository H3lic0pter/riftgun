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
        return switch (setting) {
            case REMOTE_SCROLL_ADJUSTMENT -> state.withPlacement(
                placement.withRemoteScrollAdjustment(value));
            case REMOTE_RADIAL_SLIDER -> state.withPlacement(
                placement.withRemoteRadialSlider(value));
            case REMOTE_PLACEMENT_PREVIEW -> state.withPlacement(
                placement.withRemotePreview(value));
            default -> state.withTransit(transitWith(state.transit(), setting, value));
        };
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
        return state.withPlacement(setting == FallbackSetting.COORDINATE_SMART
            ? p.withCoordinateSmartFallback(value) : p.withPairingSmartFallback(value));
    }

    public static PortalGunViewState stepPlayerExclude(PortalGunViewState state, int amount) {
        var t = state.transit();
        return state.withTransit(t.withPlayerExcludeMode(t.playerExcludeMode().step(amount)));
    }

    public static PortalGunViewState withDistance(PortalGunViewState state,
                                                  DistanceSetting setting, int value) {
        var p = state.placement();
        if (setting == DistanceSetting.SMART_DISTANCE) {
            return state.withPlacement(p.withSmartDistance(value));
        }
        if (setting == DistanceSetting.REMOTE_DISTANCE) {
            return state.withPlacement(p.withRemoteDistance(value));
        }
        var t = state.transit();
        return state.withTransit(setting == DistanceSetting.PORTAL_DURATION
            ? t.withPortalDuration(value) : t.withTransitCooldown(value));
    }

    private static PortalGunViewState.Transit transitWith(PortalGunViewState.Transit t,
                                                           BooleanSetting setting, boolean value) {
        return switch (setting) {
            case PASSIVE_TRANSIT -> t.withTransitKinds(
                value, t.hostileEnabled(), t.bossEnabled(), t.projectileEnabled());
            case HOSTILE_TRANSIT -> t.withTransitKinds(
                t.passiveEnabled(), value, t.bossEnabled(), t.projectileEnabled());
            case BOSS_TRANSIT -> t.withTransitKinds(
                t.passiveEnabled(), t.hostileEnabled(), value, t.projectileEnabled());
            case PROJECTILE_TRANSIT -> t.withTransitKinds(
                t.passiveEnabled(), t.hostileEnabled(), t.bossEnabled(), value);
            case PLAYER_TARGET -> t.withPlayerTarget(value);
            case EXPANDED_APERTURE -> t.withExpandedAperture(value);
            case FALL_GUARD -> t.withFallGuard(value);
            case FALL_GUARD_ENTITIES -> t.withEntityFallGuard(value);
            case ENTITY_RELOCATION -> t.withEntityRelocation(value);
            case ENTITY_RELOCATION_SMART_ROUTING -> t.withEntityRelocationSmartRouting(value);
            case REMOTE_SCROLL_ADJUSTMENT, REMOTE_RADIAL_SLIDER, REMOTE_PLACEMENT_PREVIEW ->
                throw new IllegalArgumentException("placement setting required");
        };
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
