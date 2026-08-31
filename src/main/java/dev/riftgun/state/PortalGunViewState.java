package dev.riftgun.state;

import dev.riftgun.module.PlayerExcludeMode;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingPendingEndpoint;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Immutable, typed client view of one server-authoritative Portal Gun snapshot. */
public record PortalGunViewState(
    @Nullable UUID instanceId,
    @Nullable PortalPairingPendingEndpoint pendingPairingEndpoint,
    Fuel fuel,
    Navigation navigation,
    Placement placement,
    Transit transit,
    Modules modules
) {
    public PortalGunViewState {
        fuel = fuel == null ? Fuel.EMPTY : fuel;
        navigation = navigation == null ? Navigation.EMPTY : navigation;
        placement = placement == null ? Placement.EMPTY : placement;
        transit = transit == null ? Transit.EMPTY : transit;
        modules = modules == null ? Modules.empty() : modules;
    }

    public static PortalGunViewState empty() {
        return new PortalGunViewState(null, null, Fuel.EMPTY, Navigation.EMPTY,
            Placement.EMPTY, Transit.EMPTY, Modules.empty());
    }

    public PortalGunViewState withNavigation(Navigation next) {
        return new PortalGunViewState(instanceId, pendingPairingEndpoint, fuel, next,
            placement, transit, modules);
    }

    public PortalGunViewState withPlacement(Placement next) {
        return new PortalGunViewState(instanceId, pendingPairingEndpoint, fuel, navigation,
            next, transit, modules);
    }

    public PortalGunViewState withTransit(Transit next) {
        return new PortalGunViewState(instanceId, pendingPairingEndpoint, fuel, navigation,
            placement, next, modules);
    }

    public int moduleCount(PortalModuleKind kind) {
        return modules.counts().getOrDefault(kind, 0);
    }

    public boolean available() { return instanceId != null; }
    public boolean bucketMode() { return fuel.bucketMode(); }
    public int amount() { return fuel.amount(); }
    public int capacity() { return fuel.capacity(); }
    public boolean infiniteFuel() { return fuel.infinite(); }
    public boolean unstableFuel() { return fuel.unstable(); }
    public String fluidId() { return fuel.fluidId(); }
    public int fluidRgb() { return fuel.rgb(); }
    public boolean crossDimensionFuel() { return fuel.crossDimension(); }
    public boolean coordinateOverride() { return navigation.coordinateOverride(); }
    public boolean dimensionalTraversalInstalled() { return navigation.dimensionalTraversalInstalled(); }
    public boolean dimensionalTraversalEnabled() { return navigation.dimensionalTraversalEnabled(); }
    public int maximumSurfaceRange() { return placement.maximumSurfaceRange(); }
    public int remoteDistance() { return placement.remoteDistance(); }
    public int smartDistance() { return placement.smartDistance(); }
    public boolean remoteInstalled() { return placement.remoteInstalled(); }
    public boolean remoteScrollAdjustmentEnabled() { return placement.remoteScrollAdjustmentEnabled(); }
    public boolean remoteRadialSliderEnabled() { return placement.remoteRadialSliderEnabled(); }
    public boolean remotePreviewEnabled() { return placement.remotePreviewEnabled(); }
    public boolean precisionInstalled() { return placement.precisionInstalled(); }
    public boolean pairingInstalled() { return placement.pairingInstalled(); }
    public PortalFunctionMode functionMode() { return placement.functionMode(); }
    public boolean passiveTransitEnabled() { return transit.passiveEnabled(); }
    public boolean hostileTransitEnabled() { return transit.hostileEnabled(); }
    public boolean bossTransitEnabled() { return transit.bossEnabled(); }
    public boolean projectileTransitEnabled() { return transit.projectileEnabled(); }
    public int portalDurationSeconds() { return transit.portalDurationSeconds(); }
    public int maximumPortalDurationSeconds() { return transit.maximumPortalDurationSeconds(); }
    public boolean eternalDurationInstalled() { return transit.eternalDurationInstalled(); }
    public boolean expandedApertureEnabled() { return transit.expandedApertureEnabled(); }
    public int transitCooldownTenths() { return transit.transitCooldownTenths(); }
    public int maximumTransitCooldownTenths() { return transit.maximumTransitCooldownTenths(); }
    public boolean playerTargetEnabled() { return transit.playerTargetEnabled(); }
    public PlayerExcludeMode playerExcludeMode() { return transit.playerExcludeMode(); }
    public boolean fallGuardEnabled() { return transit.fallGuardEnabled(); }
    public boolean entityFallGuardEnabled() { return transit.entityFallGuardEnabled(); }
    public boolean entityRelocationEnabled() { return transit.entityRelocationEnabled(); }
    public boolean entityRelocationSmartRouting() { return transit.entityRelocationSmartRouting(); }

    public record Fuel(boolean bucketMode, int amount, int capacity, boolean overfilled,
                       boolean infinite, boolean unstable, String fluidId, int rgb,
                       boolean crossDimension) {
        static final Fuel EMPTY = new Fuel(false, 0, 1, false, false, false, "", 0, false);

        public Fuel {
            fluidId = fluidId == null ? "" : fluidId;
        }
    }

    public record Navigation(boolean coordinateOverride, boolean dimensionalTraversalInstalled,
                             boolean dimensionalTraversalEnabled, String targetDimension,
                             DimensionalTraversalMode mode) {
        static final Navigation EMPTY = new Navigation(false, false, false, "",
            DimensionalTraversalMode.EXACT_COORDINATES);

        public Navigation {
            targetDimension = targetDimension == null ? "" : targetDimension;
            mode = mode == null ? DimensionalTraversalMode.EXACT_COORDINATES : mode;
        }

        public Navigation withTargetDimension(String dimension) {
            return new Navigation(coordinateOverride, dimensionalTraversalInstalled,
                dimensionalTraversalEnabled, dimension, mode);
        }

        public Navigation withMode(DimensionalTraversalMode next) {
            return new Navigation(coordinateOverride, dimensionalTraversalInstalled,
                dimensionalTraversalEnabled, targetDimension, next);
        }
    }

    public record Placement(int maximumSurfaceRange, int remoteDistance, int smartDistance,
                            boolean remoteInstalled, boolean remoteScrollAdjustmentEnabled,
                            boolean remoteRadialSliderEnabled, boolean remotePreviewEnabled,
                            boolean precisionInstalled, boolean pairingInstalled,
                            PortalFunctionMode functionMode,
                            PortalFloatingFallback coordinateSmartFallback,
                            PortalFloatingFallback pairingSmartFallback) {
        static final Placement EMPTY = new Placement(1, 1, 1, false, false, false, false,
            false, false, PortalFunctionMode.COORDINATE_TRAVEL,
            PortalFloatingFallback.FRONT, PortalFloatingFallback.FRONT);

        public Placement {
            maximumSurfaceRange = Math.max(1, maximumSurfaceRange);
            remoteDistance = Math.clamp(remoteDistance, 1, maximumSurfaceRange);
            smartDistance = Math.clamp(smartDistance, 1, maximumSurfaceRange);
            functionMode = functionMode == null
                ? PortalFunctionMode.COORDINATE_TRAVEL : functionMode;
            coordinateSmartFallback = coordinateSmartFallback == null
                ? PortalFloatingFallback.FRONT : coordinateSmartFallback;
            pairingSmartFallback = pairingSmartFallback == null
                ? PortalFloatingFallback.FRONT : pairingSmartFallback;
        }

        public Placement withRemoteDistance(int distance) {
            return new Placement(maximumSurfaceRange, distance, smartDistance, remoteInstalled,
                remoteScrollAdjustmentEnabled, remoteRadialSliderEnabled, remotePreviewEnabled,
                precisionInstalled, pairingInstalled, functionMode, coordinateSmartFallback,
                pairingSmartFallback);
        }

        public Placement withSmartDistance(int distance) {
            return new Placement(maximumSurfaceRange, remoteDistance, distance, remoteInstalled,
                remoteScrollAdjustmentEnabled, remoteRadialSliderEnabled, remotePreviewEnabled,
                precisionInstalled, pairingInstalled, functionMode, coordinateSmartFallback,
                pairingSmartFallback);
        }

        public Placement withRemoteScrollAdjustment(boolean enabled) {
            return new Placement(maximumSurfaceRange, remoteDistance, smartDistance, remoteInstalled,
                enabled, remoteRadialSliderEnabled, remotePreviewEnabled, precisionInstalled,
                pairingInstalled, functionMode, coordinateSmartFallback, pairingSmartFallback);
        }

        public Placement withRemoteRadialSlider(boolean enabled) {
            return new Placement(maximumSurfaceRange, remoteDistance, smartDistance, remoteInstalled,
                remoteScrollAdjustmentEnabled, enabled, remotePreviewEnabled, precisionInstalled,
                pairingInstalled, functionMode, coordinateSmartFallback, pairingSmartFallback);
        }

        public Placement withRemotePreview(boolean enabled) {
            return new Placement(maximumSurfaceRange, remoteDistance, smartDistance, remoteInstalled,
                remoteScrollAdjustmentEnabled, remoteRadialSliderEnabled, enabled, precisionInstalled,
                pairingInstalled, functionMode, coordinateSmartFallback, pairingSmartFallback);
        }

        public Placement withCoordinateSmartFallback(PortalFloatingFallback fallback) {
            return new Placement(maximumSurfaceRange, remoteDistance, smartDistance, remoteInstalled,
                remoteScrollAdjustmentEnabled, remoteRadialSliderEnabled, remotePreviewEnabled,
                precisionInstalled, pairingInstalled, functionMode, fallback, pairingSmartFallback);
        }

        public Placement withPairingSmartFallback(PortalFloatingFallback fallback) {
            return new Placement(maximumSurfaceRange, remoteDistance, smartDistance, remoteInstalled,
                remoteScrollAdjustmentEnabled, remoteRadialSliderEnabled, remotePreviewEnabled,
                precisionInstalled, pairingInstalled, functionMode, coordinateSmartFallback, fallback);
        }
    }

    public record Transit(int entityAccessMask, boolean passiveEnabled, boolean hostileEnabled,
                          boolean bossEnabled, boolean projectileEnabled,
                          int portalDurationSeconds, int maximumPortalDurationSeconds,
                          boolean eternalDurationInstalled, boolean expandedApertureEnabled,
                          int transitCooldownTenths, int maximumTransitCooldownTenths,
                          boolean playerTargetInstalled, boolean playerTargetEnabled,
                          PlayerExcludeMode playerExcludeMode,
                          boolean fallGuardInstalled, boolean fallGuardEnabled,
                          boolean entityFallGuardEnabled, boolean entityRelocationInstalled,
                          boolean entityRelocationEnabled,
                          boolean entityRelocationSmartRouting) {
        static final Transit EMPTY = new Transit(0, false, false, false, false, 1, 1,
            false, false, 0, 1, false, false, PlayerExcludeMode.OFF, false, false,
            false, false, false, false);

        public Transit {
            playerExcludeMode = playerExcludeMode == null ? PlayerExcludeMode.OFF : playerExcludeMode;
        }

        public Transit withTransitKinds(boolean passive, boolean hostile, boolean boss,
                                        boolean projectile) {
            return copy(passive, hostile, boss, projectile,
                portalDurationSeconds, expandedApertureEnabled, transitCooldownTenths,
                playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withPortalDuration(int seconds) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, seconds, expandedApertureEnabled, transitCooldownTenths,
                playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withExpandedAperture(boolean enabled) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, enabled, transitCooldownTenths,
                playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withTransitCooldown(int tenths) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled, tenths,
                playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withPlayerTarget(boolean enabled) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled,
                transitCooldownTenths, enabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withPlayerExcludeMode(PlayerExcludeMode mode) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled,
                transitCooldownTenths, playerTargetEnabled, mode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withFallGuard(boolean enabled) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled,
                transitCooldownTenths, playerTargetEnabled, playerExcludeMode, enabled,
                entityFallGuardEnabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withEntityFallGuard(boolean enabled) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled,
                transitCooldownTenths, playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                enabled, entityRelocationEnabled, entityRelocationSmartRouting);
        }

        public Transit withEntityRelocation(boolean enabled) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled,
                transitCooldownTenths, playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, enabled, entityRelocationSmartRouting);
        }

        public Transit withEntityRelocationSmartRouting(boolean enabled) {
            return copy(passiveEnabled, hostileEnabled, bossEnabled,
                projectileEnabled, portalDurationSeconds, expandedApertureEnabled,
                transitCooldownTenths, playerTargetEnabled, playerExcludeMode, fallGuardEnabled,
                entityFallGuardEnabled, entityRelocationEnabled, enabled);
        }

        private Transit copy(boolean passive, boolean hostile, boolean boss, boolean projectile,
                             int duration, boolean expandedAperture,
                             int cooldown, boolean playerTarget, PlayerExcludeMode excludeMode,
                             boolean fallGuard, boolean entityFallGuard,
                             boolean entityRelocation, boolean smartRouting) {
            return new Transit(entityAccessMask, passive, hostile, boss, projectile, duration,
                maximumPortalDurationSeconds, eternalDurationInstalled, expandedAperture,
                cooldown, maximumTransitCooldownTenths, playerTargetInstalled, playerTarget,
                excludeMode, fallGuardInstalled, fallGuard, entityFallGuard,
                entityRelocationInstalled, entityRelocation, smartRouting);
        }
    }

    public record Modules(Map<PortalModuleKind, Integer> counts, PortalModuleRules rules) {
        public Modules {
            counts = counts == null ? Map.of() : Map.copyOf(counts);
            rules = rules == null ? PortalModuleRules.defaults() : rules;
        }

        static Modules empty() {
            return new Modules(Map.of(), PortalModuleRules.defaults());
        }
    }
}
