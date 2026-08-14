package dev.riftgun.core.config;

import dev.riftgun.data.TargetPrivacy;
import dev.riftgun.service.PortalShortcutGunMode;
import java.util.List;

/** Loader-neutral, immutable server configuration consumed by common gameplay code. */
public record RiftConfig(
    ShortcutConfig shortcuts,
    DestinationConfig destinations,
    FuelConfig fuel,
    ModuleConfig modules,
    PortalConfig portal,
    RelocationConfig relocation,
    ProjectileConfig projectile,
    PrivacyConfig privacy,
    PredictionConfig prediction,
    CrisisConfig crises,
    DiagnosticsConfig diagnostics
) {
    public record ShortcutConfig(PortalShortcutGunMode gunLookupMode) {}

    public record DestinationConfig(
        int maximumDestinations,
        int maximumGroups,
        int maximumDestinationNameLength,
        int maximumGroupNameLength
    ) {}

    public record FuelConfig(
        boolean randomConsumption,
        int unstableMinimum,
        int unstableMaximum,
        int portalMinimum,
        int portalMaximum,
        int dimensionalMinimum,
        int dimensionalMaximum
    ) {}

    public record ModuleConfig(
        int maximumReservoirModules,
        int reservoirCapacityPerModule,
        int maximumSurfaceRangeModules,
        int surfaceRangePerModule,
        int maximumDurationExtensionModules,
        int durationExtensionSecondsPerModule,
        boolean zeroPointFuelRecipeEnabled,
        boolean matterAnchorPreventsDespawn
    ) {}

    public record PortalConfig(
        int maximumDurationSeconds,
        boolean passengerTreeTransitEnabled,
        double horizontalTriggerExtend
    ) {}

    public record RelocationConfig(
        int maximumConcurrentPerGun,
        int targetCooldownTicks,
        int exitDurationSeconds,
        int destinationReadinessTimeoutTicks,
        int exitPortalImmunityTicks,
        int projectileOpeningTicks,
        double passiveFuelMultiplier,
        double hostileFuelMultiplier,
        double playerFuelMultiplier,
        double bossFuelMultiplier,
        double projectileFuelMultiplier,
        double utilityFuelMultiplier,
        boolean passengerTreeEnabled,
        int maximumPassengerTreeSize
    ) {}

    public record ProjectileConfig(
        int maximumTransits,
        boolean sweptCollisionEnabled,
        int effectCooldownTicks
    ) {}

    public record PrivacyConfig(
        TargetPrivacy defaultTarget,
        TargetPrivacy defaultRelocationDestination,
        TargetPrivacy defaultRelocationSubject,
        boolean foreignExitTransitAllowed,
        int requestTimeoutSeconds,
        int grantTimeoutSeconds,
        int denyOnceCooldownSeconds
    ) {}

    public record PredictionConfig(
        double frontProjectionFactor,
        double downshotProjectionFactor
    ) {}

    public record CrisisConfig(
        List<String> forceUnstableFluids,
        List<String> forceStableFluids,
        List<String> weights,
        int maximumExits,
        int maximumTrackedPlayers,
        int highFallHeight,
        int minimumHighFallDrop,
        int highFallCooldownTicks,
        int guardedHighFallCooldownTicks,
        int lavaSearchRadius,
        int lavaCandidateChecks,
        int lavaMinimumArmor,
        int lavaMinimumFireProtection,
        int spatialTearMinimumHealth,
        double spatialTearMinimumHealthRatio,
        int spatialTearProtectionTicks,
        int spatialTearCooldownTicks,
        int weaknessDurationTicks,
        int weaknessAmplifier,
        int nauseaDurationTicks,
        int nauseaAmplifier,
        double nauseaSoundVolume,
        double nauseaSoundPitch
    ) {
        public CrisisConfig {
            forceUnstableFluids = List.copyOf(forceUnstableFluids);
            forceStableFluids = List.copyOf(forceStableFluids);
            weights = List.copyOf(weights);
        }
    }

    public record DiagnosticsConfig(boolean transitEnabled) {}
}
