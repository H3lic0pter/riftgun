package dev.riftgun.core.config;

import dev.riftgun.data.TargetPrivacy;
import dev.riftgun.service.PortalShortcutGunMode;
import java.util.List;

/** Loader-neutral, immutable server configuration consumed by common gameplay code. */
public record RiftConfig(
    ShortcutConfig shortcuts,
    DestinationConfig destinations,
    CoordinateSharingConfig coordinateSharing,
    RandomRiftConfig randomRift,
    FuelConfig fuel,
    ModuleConfig modules,
    PortalConfig portal,
    SpecialEntityTransitConfig specialEntityTransit,
    RelocationConfig relocation,
    ProjectileConfig projectile,
    PrivacyConfig privacy,
    PredictionConfig prediction,
    CrisisConfig crises,
    DiagnosticsConfig diagnostics
) {
    public static RiftConfig defaults() {
        return new RiftConfig(
            new ShortcutConfig(PortalShortcutGunMode.HELD_HANDS),
            new DestinationConfig(256, 32, 48, 32),
            new CoordinateSharingConfig(true, 300, 5),
            new RandomRiftConfig(true, 60, 256, 4096, 16, 8),
            new FuelConfig(true, 50, 100, 5, 8, 5, 8),
            new ModuleConfig(2, 8000, 3, 16, 1, 45, true, true),
            new PortalConfig(15, true, 0.35),
            new SpecialEntityTransitConfig(true),
            new RelocationConfig(8, 10, 3, 100, 100, 2,
                1.5, 3.0, 3.0, 10.0, 1.0, 1.0, true, 16),
            new ProjectileConfig(32, true, 2),
            new PrivacyConfig(TargetPrivacy.PUBLIC, TargetPrivacy.REQUEST, TargetPrivacy.REQUEST,
                true, 10, 60, 10),
            new PredictionConfig(0.7, 2.5),
            new CrisisConfig(List.of(), List.of(), List.of(
                "riftgun:high_altitude_fall=8",
                "riftgun:lava_hazard=5",
                "riftgun:spatial_tear=2",
                "riftgun:weakness=30",
                "riftgun:nausea=55"
            ), 4, 1024, 192, 128, 30, 15, 24, 96, 20, 4,
                16, 0.8, 30, 40, 1000, 0, 160, 0, 0.45, 1.35),
            new DiagnosticsConfig(false)
        );
    }

    public record ShortcutConfig(PortalShortcutGunMode gunLookupMode) {}

    public record DestinationConfig(
        int maximumDestinations,
        int maximumGroups,
        int maximumDestinationNameLength,
        int maximumGroupNameLength
    ) {}

    public record CoordinateSharingConfig(
        boolean enabled,
        int chatExpirySeconds,
        int chatCooldownSeconds
    ) {}

    public record RandomRiftConfig(
        boolean enabled,
        int cooldownTicks,
        int minimumRadius,
        int maximumRadius,
        int maximumAttempts,
        int maximumConcurrentSearches
    ) {
        public int innerRadius() {
            return Math.min(minimumRadius, maximumRadius);
        }

        public int outerRadius() {
            return Math.max(minimumRadius, maximumRadius);
        }
    }

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

    public record SpecialEntityTransitConfig(boolean sweptCollisionEnabled) {}

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
