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
        int reservoirCount = PortalGunModules.activeCount(gun, PortalModuleKind.RESERVOIR_EXPANSION, rules);
        int rangeCount = PortalGunModules.activeCount(gun, PortalModuleKind.SURFACE_RANGE, rules);
        int maximumRange = rules.maximumSurfaceRangeFor(rangeCount);
        int remoteDistance = configuredRemoteDistance(settings.desiredRemoteDistance(), maximumRange);
        int durationSeconds = configuredDurationSeconds(gun, settings.portalDurationSeconds(), rules);
        boolean apertureInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.APERTURE_EXPANSION, rules) > 0;
        boolean playerTargetInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.PLAYER_TARGET, rules) > 0;
        boolean relocationInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.ENTITY_RELOCATION, rules) > 0;
        boolean pairingInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.PORTAL_PAIRING, rules) > 0;
        boolean remoteInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.REMOTE, rules) > 0;
        boolean fallGuardInstalled = PortalGunModules.activeCount(
            gun, PortalModuleKind.FALL_GUARD, rules) > 0;
        return new PortalGunCapabilities(
            PortalGunModules.activeCount(gun, PortalModuleKind.COORDINATE_OVERRIDE, rules) > 0,
            PortalGunModules.activeCount(gun, PortalModuleKind.DIMENSIONAL_TRAVERSAL, rules) > 0,
            rules.capacityFor(reservoirCount),
            maximumRange,
            remoteDistance,
            configuredSmartDistance(settings.smartDistance(), maximumRange),
            new PortalEntityAccessSnapshot(
                PortalGunModules.activeCount(gun, PortalModuleKind.PASSIVE_TRANSIT, rules) > 0
                    && settings.passiveTransitEnabled(),
                PortalGunModules.activeCount(gun, PortalModuleKind.HOSTILE_TRANSIT, rules) > 0
                    && settings.hostileTransitEnabled(),
                PortalGunModules.activeCount(gun, PortalModuleKind.BOSS_TRANSIT, rules) > 0
                    && settings.bossTransitEnabled(),
                PortalGunModules.activeCount(gun, PortalModuleKind.PROJECTILE_TRANSIT, rules) > 0
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
            PortalGunModules.activeCount(gun, PortalModuleKind.PRECISION_PLACEMENT, rules) > 0,
            pairingInstalled,
            pairingInstalled ? settings.portalPairing().functionMode() : PortalFunctionMode.COORDINATE_TRAVEL,
            remoteInstalled ? settings.remote().coordinateSmartFallback() : PortalFloatingFallback.FRONT,
            pairingInstalled && remoteInstalled
                ? settings.portalPairing().smartFallback() : PortalFloatingFallback.FRONT,
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

    public static boolean usesRemoteDistanceControls(PortalPlacementMode placementMode,
                                                     PortalFunctionMode functionMode) {
        return placementMode == PortalPlacementMode.REMOTE
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

    public static int configuredDurationSeconds(ItemStack gun, int requestedSeconds) {
        return configuredDurationSeconds(gun, requestedSeconds, PortalModuleRules.current());
    }

    public static int maximumDurationSeconds(ItemStack gun, PortalModuleRules rules) {
        if (hasEternalDuration(gun, rules)) return PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS;
        int extensionCount = PortalGunModules.activeCount(gun, PortalModuleKind.DURATION_EXTENSION, rules);
        return rules.maximumPortalDurationSeconds(extensionCount);
    }

    public static boolean hasEternalDuration(ItemStack gun, PortalModuleRules rules) {
        return PortalGunModules.activeCount(gun, PortalModuleKind.DURATION_ETERNAL, rules) > 0;
    }

    private static int configuredDurationSeconds(ItemStack gun, int requestedSeconds,
                                                 PortalModuleRules rules) {
        boolean eternalInstalled = hasEternalDuration(gun, rules);
        return PortalOpenDuration.authorizedSeconds(requestedSeconds,
            maximumDurationSeconds(gun, rules), eternalInstalled);
    }
}
