package dev.riftgun.module;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import dev.riftgun.portal.PortalAperture;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.data.PortalPlacementMode;

public record PortalGunCapabilities(
    boolean coordinateOverride,
    boolean dimensionalTraversal,
    int nominalCapacity,
    int maximumSurfaceRange,
    int remoteDistance,
    int smartDistance,
    PortalEntityAccessSnapshot entityAccess,
    int openDurationTicks,
    PortalAperture aperture,
    boolean playerTarget,
    PlayerExcludeMode playerExcludeMode,
    int transitCooldownTicks,
    boolean entityRelocation,
    boolean entityRelocationSmartRouting,
    boolean remote,
    boolean remoteScrollAdjustment,
    boolean precisionPlacement,
    boolean portalPairing,
    PortalFunctionMode functionMode,
    PortalFloatingFallback coordinateSmartFallback,
    PortalFloatingFallback pairingSmartFallback,
    boolean fallGuard,
    boolean entityFallGuard
) {
    public static PortalGunCapabilities resolve(ItemStack gun, int legacySmartDistance) {
        return resolve(gun, legacySmartDistance, PortalModuleRules.current());
    }

    public static PortalGunCapabilities resolve(ItemStack gun, int legacySmartDistance,
                                                 PortalModuleRules rules) {
        PortalGunModuleSettings settings = PortalGunModuleSettings.get(gun, legacySmartDistance);
        return resolve(gun, settings, rules);
    }

    public static PortalGunCapabilities resolve(ItemStack gun, PortalGunModuleSettings settings,
                                                 PortalModuleRules rules) {
        PortalGunModules.ActiveCounts modules = PortalGunModules.activeCounts(gun, rules);
        int reservoirCount = modules.count(PortalModuleKind.RESERVOIR_EXPANSION);
        int rangeCount = modules.count(PortalModuleKind.SURFACE_RANGE);
        int maximumRange = rules.maximumSurfaceRangeFor(rangeCount);
        int remoteDistance = configuredRemoteDistance(settings.desiredRemoteDistance(), maximumRange);
        int durationSeconds = configuredDurationSeconds(
            modules, settings.portalDurationSeconds(), rules);
        boolean apertureInstalled = modules.count(PortalModuleKind.APERTURE_EXPANSION) > 0;
        boolean playerTargetInstalled = modules.count(PortalModuleKind.PLAYER_TARGET) > 0;
        boolean relocationInstalled = modules.count(PortalModuleKind.ENTITY_RELOCATION) > 0;
        boolean pairingInstalled = modules.count(PortalModuleKind.PORTAL_PAIRING) > 0;
        boolean remoteInstalled = modules.count(PortalModuleKind.REMOTE) > 0;
        boolean fallGuardInstalled = modules.count(PortalModuleKind.FALL_GUARD) > 0;
        return new PortalGunCapabilities(
            modules.count(PortalModuleKind.COORDINATE_OVERRIDE) > 0,
            modules.count(PortalModuleKind.DIMENSIONAL_TRAVERSAL) > 0,
            rules.capacityFor(reservoirCount),
            maximumRange,
            remoteDistance,
            configuredSmartDistance(settings.smartDistance(), maximumRange),
            new PortalEntityAccessSnapshot(
                modules.count(PortalModuleKind.PASSIVE_TRANSIT) > 0
                    && settings.passiveTransitEnabled(),
                modules.count(PortalModuleKind.HOSTILE_TRANSIT) > 0
                    && settings.hostileTransitEnabled(),
                modules.count(PortalModuleKind.BOSS_TRANSIT) > 0
                    && settings.bossTransitEnabled(),
                modules.count(PortalModuleKind.PROJECTILE_TRANSIT) > 0
                    && settings.projectileTransitEnabled()
            ),
            PortalOpenDuration.ticks(durationSeconds),
            apertureInstalled && settings.expandedApertureEnabled()
                ? PortalAperture.EXPANDED : PortalAperture.STANDARD,
            playerTargetInstalled && settings.playerTargetEnabled(),
            settings.playerExcludeMode(),
            settings.transitCooldownTenths() * 2,
            relocationInstalled && settings.entityRelocation().enabled(),
            relocationInstalled && settings.entityRelocation().enabled()
                && settings.entityRelocation().smartRouting(),
            remoteInstalled,
            remoteInstalled && settings.remote().scrollAdjustmentEnabled(),
            modules.count(PortalModuleKind.PRECISION_PLACEMENT) > 0,
            pairingInstalled,
            pairingInstalled ? settings.portalPairing().functionMode() : PortalFunctionMode.COORDINATE_TRAVEL,
            remoteInstalled ? settings.remote().coordinateSmartFallback() : PortalFloatingFallback.FRONT,
            configuredPairingSmartFallback(pairingInstalled, remoteInstalled,
                settings.portalPairing().smartFallback()),
            fallGuardInstalled && settings.fallGuardEnabled(),
            fallGuardInstalled && settings.fallGuardEntitiesEnabled()
        );
    }

    public PortalFloatingFallback activeSmartFallback() {
        return functionMode == PortalFunctionMode.PORTAL_PAIRING
            ? pairingSmartFallback : coordinateSmartFallback;
    }

    /** Applies availability without overwriting the player's persisted placement preference. */
    public PortalPlacementMode effectivePlacementMode(PortalPlacementMode preferred) {
        return effectivePlacementMode(preferred, remote);
    }

    public boolean usesRemoteDistanceControls(PortalPlacementMode placementMode) {
        return usesRemoteDistanceControls(placementMode, functionMode, activeSmartFallback());
    }

    public static boolean usesRemoteDistanceControls(PortalPlacementMode placementMode,
                                                     PortalFunctionMode functionMode,
                                                     PortalFloatingFallback smartFallback) {
        return placementMode == PortalPlacementMode.REMOTE
            || placementMode == PortalPlacementMode.SMART
                && smartFallback == PortalFloatingFallback.REMOTE
            || placementMode == PortalPlacementMode.ENTITY_RELOCATION
                && functionMode == PortalFunctionMode.PORTAL_PAIRING;
    }

    static PortalPlacementMode effectivePlacementMode(PortalPlacementMode preferred,
                                                        boolean remoteAvailable) {
        return preferred == PortalPlacementMode.REMOTE && !remoteAvailable
            ? PortalPlacementMode.FRONT : preferred;
    }

    static int configuredRemoteDistance(int desiredRange, int maximumRange) {
        return Mth.clamp(desiredRange, 1, Math.max(1, maximumRange));
    }

    static int configuredSmartDistance(int desiredDistance, int maximumRange) {
        return Mth.clamp(desiredDistance, 1, Math.max(1, maximumRange));
    }

    static PortalFloatingFallback configuredPairingSmartFallback(boolean pairingInstalled,
                                                                  boolean remoteInstalled,
                                                                  PortalFloatingFallback saved) {
        return pairingInstalled && remoteInstalled ? saved : PortalFloatingFallback.FRONT;
    }

    public static int configuredDurationSeconds(ItemStack gun, int requestedSeconds) {
        return configuredDurationSeconds(gun, requestedSeconds, PortalModuleRules.current());
    }

    public static int maximumDurationSeconds(ItemStack gun, PortalModuleRules rules) {
        return maximumDurationSeconds(PortalGunModules.activeCounts(gun, rules), rules);
    }

    private static int maximumDurationSeconds(PortalGunModules.ActiveCounts modules,
                                              PortalModuleRules rules) {
        if (hasEternalDuration(modules, rules)) {
            return PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS;
        }
        int extensionCount = modules.count(PortalModuleKind.DURATION_EXTENSION);
        return rules.maximumPortalDurationSeconds(extensionCount);
    }

    public static boolean hasEternalDuration(ItemStack gun, PortalModuleRules rules) {
        return hasEternalDuration(PortalGunModules.activeCounts(gun, rules), rules);
    }

    private static boolean hasEternalDuration(PortalGunModules.ActiveCounts modules,
                                              PortalModuleRules rules) {
        return modules.count(PortalModuleKind.DURATION_ETERNAL) > 0;
    }

    private static int configuredDurationSeconds(ItemStack gun, int requestedSeconds,
                                                  PortalModuleRules rules) {
        return configuredDurationSeconds(
            PortalGunModules.activeCounts(gun, rules), requestedSeconds, rules);
    }

    private static int configuredDurationSeconds(PortalGunModules.ActiveCounts modules,
                                                  int requestedSeconds, PortalModuleRules rules) {
        boolean eternalInstalled = hasEternalDuration(modules, rules);
        return PortalOpenDuration.authorizedSeconds(requestedSeconds,
            maximumDurationSeconds(modules, rules), eternalInstalled);
    }
}
