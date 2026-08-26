package dev.riftgun.config;

import dev.riftgun.core.config.RiftConfig;
import dev.riftgun.core.config.RiftConfigs;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import dev.riftgun.portal.PortalOpenDuration;
import dev.riftgun.service.PortalShortcutGunMode;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    static {
        var configured = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = configured.getLeft();
        SPEC = configured.getRight();
    }

    /** Rebuilds and atomically publishes a complete loader-neutral snapshot. */
    public static void publishSnapshot() {
        Values value = VALUES;
        RiftConfigs.publishServer(new RiftConfig(
            new RiftConfig.ShortcutConfig(value.shortcutGunMode.get()),
            new RiftConfig.DestinationConfig(
                value.maxDestinations.get(), value.maxGroups.get(),
                value.maxDestinationNameLength.get(), value.maxGroupNameLength.get()),
            new RiftConfig.CoordinateSharingConfig(
                value.coordinateSharingEnabled.get(), value.chatShareExpirySeconds.get(),
                value.chatShareCooldownSeconds.get()),
            new RiftConfig.MapWaypointIntegrationConfig(value.mapWaypointIntegrationEnabled.get()),
            new RiftConfig.RandomRiftConfig(
                value.randomRiftEnabled.get(), value.randomRiftCooldownTicks.get(),
                value.randomRiftMinimumRadius.get(), value.randomRiftMaximumRadius.get(),
                value.randomRiftMaximumAttempts.get(), value.maxConcurrentRandomRiftSearches.get()),
            new RiftConfig.FuelConfig(
                value.randomConsumption.get(), value.unstableFuelMin.get(), value.unstableFuelMax.get(),
                value.portalFuelMin.get(), value.portalFuelMax.get(),
                value.dimensionalFuelMin.get(), value.dimensionalFuelMax.get()),
            new RiftConfig.ModuleConfig(
                value.maxReservoirModules.get(), value.reservoirModuleCapacity.get(),
                value.maxSurfaceRangeModules.get(), value.surfaceRangePerModule.get(),
                value.maxDurationExtensionModules.get(), value.durationExtensionSecondsPerModule.get(),
                value.enableZeroPointFuelRecipe.get(), value.matterAnchorPreventsDespawn.get()),
            new RiftConfig.PortalConfig(
                value.maximumPortalDurationSeconds.get(), value.enablePassengerTreeTransit.get(),
                value.horizontalTriggerExtend.get()),
            new RiftConfig.SpecialEntityTransitConfig(
                value.enableSpecialEntitySweptCollision.get()),
            new RiftConfig.RelocationConfig(
                value.maximumConcurrentEntityRelocations.get(),
                value.entityRelocationTargetCooldownTicks.get(),
                value.entityRelocationExitDurationSeconds.get(),
                value.destinationReadinessTimeoutTicks.get(),
                value.entityRelocationExitPortalImmunityTicks.get(),
                value.projectileRelocationOpeningTicks.get(),
                value.passiveRelocationFuelMultiplier.get(),
                value.hostileRelocationFuelMultiplier.get(),
                value.playerRelocationFuelMultiplier.get(),
                value.bossRelocationFuelMultiplier.get(),
                value.projectileRelocationFuelMultiplier.get(),
                value.utilityRelocationFuelMultiplier.get(),
                value.enablePassengerTreeRelocation.get(), value.maximumPassengerTreeSize.get()),
            new RiftConfig.ProjectileConfig(
                value.maximumProjectileTransits.get(), value.enableProjectileSweptCollision.get(),
                value.projectileEffectCooldownTicks.get()),
            new RiftConfig.PrivacyConfig(
                value.defaultTargetPrivacy.get(),
                value.defaultEntityRelocationDestinationPrivacy.get(),
                value.defaultEntityRelocationSubjectPrivacy.get(),
                value.defaultForeignExitTransitAllowed.get(),
                value.privacyRequestTimeoutSeconds.get(), value.privacyGrantTimeoutSeconds.get(),
                value.privacyDenyOnceCooldownSeconds.get()),
            new RiftConfig.PredictionConfig(
                value.frontProjectionFactor.get(), value.downshotProjectionFactor.get()),
            new RiftConfig.CrisisConfig(
                strings(value.forceUnstableFluids.get()), strings(value.forceStableFluids.get()),
                strings(value.crisisWeights.get()), value.maximumCrisisExits.get(),
                value.maximumTrackedCrisisPlayers.get(), value.highFallHeight.get(),
                value.minimumHighFallDrop.get(), value.highFallCooldownTicks.get(),
                value.guardedHighFallCooldownTicks.get(), value.lavaSearchRadius.get(),
                value.lavaCandidateChecks.get(), value.lavaMinimumArmor.get(),
                value.lavaMinimumFireProtection.get(), value.spatialTearMinimumHealth.get(),
                value.spatialTearMinimumHealthRatio.get(), value.spatialTearProtectionTicks.get(),
                value.spatialTearCooldownTicks.get(), value.weaknessDurationTicks.get(),
                value.weaknessAmplifier.get(), value.nauseaDurationTicks.get(),
                value.nauseaAmplifier.get(), value.nauseaSoundVolume.get(),
                value.nauseaSoundPitch.get()),
            new RiftConfig.DiagnosticsConfig(value.enableTransitDiagnostics.get())
        ));
    }

    private static List<String> strings(List<? extends String> values) {
        return List.copyOf(values);
    }

    public static final class Values {
        public final ModConfigSpec.IntValue maxDestinations;
        public final ModConfigSpec.IntValue maxGroups;
        public final ModConfigSpec.IntValue maxDestinationNameLength;
        public final ModConfigSpec.IntValue maxGroupNameLength;
        public final ModConfigSpec.BooleanValue coordinateSharingEnabled;
        public final ModConfigSpec.BooleanValue mapWaypointIntegrationEnabled;
        public final ModConfigSpec.IntValue chatShareExpirySeconds;
        public final ModConfigSpec.IntValue chatShareCooldownSeconds;
        public final ModConfigSpec.BooleanValue randomRiftEnabled;
        public final ModConfigSpec.IntValue randomRiftCooldownTicks;
        public final ModConfigSpec.IntValue randomRiftMinimumRadius;
        public final ModConfigSpec.IntValue randomRiftMaximumRadius;
        public final ModConfigSpec.IntValue randomRiftMaximumAttempts;
        public final ModConfigSpec.IntValue maxConcurrentRandomRiftSearches;
        public final ModConfigSpec.BooleanValue randomConsumption;
        public final ModConfigSpec.IntValue unstableFuelMin;
        public final ModConfigSpec.IntValue unstableFuelMax;
        public final ModConfigSpec.IntValue portalFuelMin;
        public final ModConfigSpec.IntValue portalFuelMax;
        public final ModConfigSpec.IntValue dimensionalFuelMin;
        public final ModConfigSpec.IntValue dimensionalFuelMax;
        public final ModConfigSpec.IntValue maxReservoirModules;
        public final ModConfigSpec.IntValue reservoirModuleCapacity;
        public final ModConfigSpec.IntValue maxSurfaceRangeModules;
        public final ModConfigSpec.IntValue surfaceRangePerModule;
        public final ModConfigSpec.IntValue maxDurationExtensionModules;
        public final ModConfigSpec.IntValue durationExtensionSecondsPerModule;
        public final ModConfigSpec.BooleanValue enableZeroPointFuelRecipe;
        public final ModConfigSpec.BooleanValue matterAnchorPreventsDespawn;
        public final ModConfigSpec.IntValue maximumPortalDurationSeconds;
        public final ModConfigSpec.BooleanValue enablePassengerTreeTransit;
        public final ModConfigSpec.IntValue maximumConcurrentEntityRelocations;
        public final ModConfigSpec.IntValue entityRelocationTargetCooldownTicks;
        public final ModConfigSpec.IntValue entityRelocationExitDurationSeconds;
        public final ModConfigSpec.IntValue destinationReadinessTimeoutTicks;
        public final ModConfigSpec.IntValue entityRelocationExitPortalImmunityTicks;
        public final ModConfigSpec.BooleanValue enableTransitDiagnostics;
        public final ModConfigSpec.IntValue projectileRelocationOpeningTicks;
        public final ModConfigSpec.DoubleValue passiveRelocationFuelMultiplier;
        public final ModConfigSpec.DoubleValue hostileRelocationFuelMultiplier;
        public final ModConfigSpec.DoubleValue playerRelocationFuelMultiplier;
        public final ModConfigSpec.DoubleValue bossRelocationFuelMultiplier;
        public final ModConfigSpec.DoubleValue projectileRelocationFuelMultiplier;
        public final ModConfigSpec.DoubleValue utilityRelocationFuelMultiplier;
        public final ModConfigSpec.BooleanValue enablePassengerTreeRelocation;
        public final ModConfigSpec.IntValue maximumPassengerTreeSize;
        public final ModConfigSpec.IntValue maximumProjectileTransits;
        public final ModConfigSpec.BooleanValue enableProjectileSweptCollision;
        public final ModConfigSpec.IntValue projectileEffectCooldownTicks;
        public final ModConfigSpec.DoubleValue horizontalTriggerExtend;
        public final ModConfigSpec.BooleanValue enableSpecialEntitySweptCollision;
        public final ModConfigSpec.EnumValue<dev.riftgun.data.TargetPrivacy> defaultTargetPrivacy;
        public final ModConfigSpec.EnumValue<dev.riftgun.data.TargetPrivacy> defaultEntityRelocationDestinationPrivacy;
        public final ModConfigSpec.EnumValue<dev.riftgun.data.TargetPrivacy> defaultEntityRelocationSubjectPrivacy;
        public final ModConfigSpec.BooleanValue defaultForeignExitTransitAllowed;
        public final ModConfigSpec.IntValue privacyRequestTimeoutSeconds;
        public final ModConfigSpec.IntValue privacyGrantTimeoutSeconds;
        public final ModConfigSpec.IntValue privacyDenyOnceCooldownSeconds;
        public final ModConfigSpec.DoubleValue frontProjectionFactor;
        public final ModConfigSpec.DoubleValue downshotProjectionFactor;
        public final ModConfigSpec.ConfigValue<List<? extends String>> forceUnstableFluids;
        public final ModConfigSpec.ConfigValue<List<? extends String>> forceStableFluids;
        public final ModConfigSpec.ConfigValue<List<? extends String>> crisisWeights;
        public final ModConfigSpec.IntValue maximumCrisisExits;
        public final ModConfigSpec.IntValue maximumTrackedCrisisPlayers;
        public final ModConfigSpec.IntValue highFallHeight;
        public final ModConfigSpec.IntValue minimumHighFallDrop;
        public final ModConfigSpec.IntValue highFallCooldownTicks;
        public final ModConfigSpec.IntValue guardedHighFallCooldownTicks;
        public final ModConfigSpec.IntValue lavaSearchRadius;
        public final ModConfigSpec.IntValue lavaCandidateChecks;
        public final ModConfigSpec.IntValue lavaMinimumArmor;
        public final ModConfigSpec.IntValue lavaMinimumFireProtection;
        public final ModConfigSpec.IntValue spatialTearMinimumHealth;
        public final ModConfigSpec.DoubleValue spatialTearMinimumHealthRatio;
        public final ModConfigSpec.IntValue spatialTearProtectionTicks;
        public final ModConfigSpec.IntValue spatialTearCooldownTicks;
        public final ModConfigSpec.IntValue weaknessDurationTicks;
        public final ModConfigSpec.IntValue weaknessAmplifier;
        public final ModConfigSpec.IntValue nauseaDurationTicks;
        public final ModConfigSpec.IntValue nauseaAmplifier;
        public final ModConfigSpec.DoubleValue nauseaSoundVolume;
        public final ModConfigSpec.DoubleValue nauseaSoundPitch;
        public final ModConfigSpec.EnumValue<PortalShortcutGunMode> shortcutGunMode;

        private Values(ModConfigSpec.Builder builder) {
            builder.push("shortcuts");
            shortcutGunMode = builder.comment(
                    "Where keyboard shortcuts look for a Portal Gun. HELD_HANDS checks the main hand "
                        + "first and then the offhand. REGISTERED_LOCATORS also searches inventory and "
                        + "third-party locator extensions.")
                .defineEnum("gunLookupMode", PortalShortcutGunMode.HELD_HANDS);
            builder.pop();

            builder.push("destinations");
            maxDestinations = builder.defineInRange("maxDestinationsPerPlayer", 256, 1, 4096);
            maxGroups = builder.defineInRange("maxGroupsPerPlayer", 32, 0, 512);
            maxDestinationNameLength = builder.defineInRange("maxDestinationNameLength", 48, 1, 128);
            maxGroupNameLength = builder.defineInRange("maxGroupNameLength", 32, 1, 64);
            builder.pop();

            builder.push("coordinateSharing");
            coordinateSharingEnabled = builder.define("enabled", true);
            chatShareExpirySeconds = builder.comment("Lifetime of clickable coordinate shares in chat.")
                .defineInRange("chatShareExpirySeconds", 300, 10, 3600);
            chatShareCooldownSeconds = builder.comment("Per-player chat sharing cooldown. Zero disables it.")
                .defineInRange("chatShareCooldownSeconds", 5, 0, 60);
            builder.pop();

            builder.push("mapWaypointIntegration");
            mapWaypointIntegrationEnabled = builder.comment(
                    "Allow clients to use JourneyMap/Xaero waypoints as portal targets. The server",
                    "revalidates dimensions and coordinates but cannot verify that client-supplied",
                    "waypoints came from an unmodified map mod; treat this as arbitrary coordinates.")
                .define("enabled", true);
            builder.pop();

            builder.push("randomRift");
            randomRiftEnabled = builder.comment("Enable random same-dimension rifts from the Portal Gun GUI.")
                .define("enabled", true);
            randomRiftCooldownTicks = builder.comment("Per-player cooldown after a random rift opens. Zero disables it.")
                .defineInRange("cooldownTicks", 60, 0, 72000);
            randomRiftMinimumRadius = builder.defineInRange("minimumRadius", 256, 0, 30000000);
            randomRiftMaximumRadius = builder.defineInRange("maximumRadius", 4096, 1, 30000000);
            randomRiftMaximumAttempts = builder.defineInRange("maximumAttempts", 16, 1, 1024);
            maxConcurrentRandomRiftSearches = builder.comment(
                    "Maximum random rift searches running at once on this server. Existing searches "
                        + "are not canceled if this value is lowered while the server is running.")
                .defineInRange("maxConcurrentRandomRiftSearches", 8, 1, 64);
            builder.pop();

            builder.push("fuel");
            randomConsumption = builder.comment("Use a uniformly random amount between each fluid's min and max.")
                .define("randomConsumption", true);
            unstableFuelMin = builder.defineInRange("unstableMinimum", 50, 1, 8000);
            unstableFuelMax = builder.defineInRange("unstableMaximum", 100, 1, 8000);
            portalFuelMin = builder.defineInRange("portalMinimum", 5, 1, 8000);
            portalFuelMax = builder.defineInRange("portalMaximum", 8, 1, 8000);
            dimensionalFuelMin = builder.defineInRange("dimensionalMinimum", 5, 1, 8000);
            dimensionalFuelMax = builder.defineInRange("dimensionalMaximum", 8, 1, 8000);
            builder.pop();

            builder.push("modules");
            maxReservoirModules = builder.defineInRange("maximumReservoirModules", 2, 0, 9);
            reservoirModuleCapacity = builder.defineInRange("reservoirCapacityPerModule", 8000, 1, 1_000_000);
            maxSurfaceRangeModules = builder.defineInRange("maximumSurfaceRangeModules", 3, 0, 9);
            surfaceRangePerModule = builder.defineInRange("surfaceRangePerModule", 16, 1, 1024);
            maxDurationExtensionModules = builder.defineInRange("maximumDurationExtensionModules", 1, 0, 9);
            durationExtensionSecondsPerModule = builder.defineInRange("durationExtensionSecondsPerModule", 45, 1, 300);
            matterAnchorPreventsDespawn = builder.comment(
                    "Allow the Matter Anchor Module to prevent a dropped Portal Gun from despawning. "
                        + "Fire, lava, and explosion immunity remain active when disabled.")
                .define("matterAnchorPreventsDespawn", true);
            enableZeroPointFuelRecipe = builder.comment(
                    "Enable crafting the Zero-Point Fuel Module. Creative/admin acquisition is unaffected.")
                .define("enableZeroPointFuelRecipe", true);
            builder.pop();

            builder.push("portal");
            maximumPortalDurationSeconds = builder.comment(
                    "Maximum fully-open duration selectable on a Portal Gun, in seconds.")
                .defineInRange("maximumDurationSeconds", 15,
                    PortalOpenDuration.MINIMUM_SECONDS, PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS);
            enablePassengerTreeTransit = builder.comment(
                    "Allow a vehicle or other entity with passengers to transit as one passenger tree. "
                        + "Disable to leave mounted trees untouched; empty vehicles and ordinary single "
                        + "entities can still transit.")
                .define("enablePassengerTreeTransit", true);
            horizontalTriggerExtend = builder.comment(
                    "Extra trigger reach along the normal for flat top/bottom portals, in blocks. "
                        + "Catches falling bodies before their feet touch the ground so fall damage "
                        + "is resolved at the exit.")
                .defineInRange("horizontalTriggerExtend", 0.35, 0.0, 2.0);
            builder.pop();

            builder.push("specialEntityTransit");
            enableSpecialEntitySweptCollision = builder.comment(
                    "Detect fast entities listed in #riftgun:portal_transit_swept by intersecting "
                        + "their swept path with indexed portal faces. Disable to remove this runtime work; "
                        + "ordinary trigger-box transit and tag permissions remain available.")
                .define("enableSweptCollision", true);
            builder.pop();

            builder.push("entityRelocation");
            maximumConcurrentEntityRelocations = builder.comment(
                    "Maximum simultaneous Entity Relocation transactions owned by one Portal Gun.")
                .defineInRange("maximumConcurrentPerGun", 8, 1, 16);
            entityRelocationTargetCooldownTicks = builder.comment(
                    "Cooldown after a successful relocation before the same entity can be targeted again.")
                .defineInRange("targetCooldownTicks", 10, 0, 1200);
            entityRelocationExitDurationSeconds = builder.comment(
                    "Fully-open hold time for visual Entity Relocation exits, in seconds. "
                        + "Opening and closing animations are not included.")
                .defineInRange("exitDurationSeconds", 3, 1, 30);
            destinationReadinessTimeoutTicks = builder.comment(
                    "Maximum time to wait for an Entity Relocation destination to become ready, "
                        + "during both chunk preparation and active transit.")
                .defineInRange("destinationReadinessTimeoutTicks", 100, 20, 600);
            entityRelocationExitPortalImmunityTicks = builder.comment(
                    "Time after a successful Entity Relocation during which non-player members "
                        + "of the relocated entity tree cannot trigger normal portal exits. "
                        + "Set to zero to disable.")
                .defineInRange("exitPortalImmunityTicks", 100, 0, 1200);
            enablePassengerTreeRelocation = builder.comment(
                    "Allow Entity Relocation to move a vehicle and its full passenger tree.")
                .define("enablePassengerTreeRelocation", true);
            maximumPassengerTreeSize = builder.comment(
                    "Maximum members in one Entity Relocation passenger tree, including the root.")
                .defineInRange("maximumPassengerTreeSize", 16, 1, 64);
            projectileRelocationOpeningTicks = builder.comment(
                    "Opening animation duration for newly created Projectile Entity Relocation "
                        + "entrances and exits. Existing shared exits keep their current duration.")
                .defineInRange("projectileOpeningTicks", 2, 1, 5);
            builder.push("fuelMultipliers");
            passiveRelocationFuelMultiplier = relocationFuelMultiplier(builder,
                "passive", 1.5, "passive, friendly, and neutral living entities");
            hostileRelocationFuelMultiplier = relocationFuelMultiplier(builder,
                "hostile", 3.0, "hostile living entities");
            playerRelocationFuelMultiplier = relocationFuelMultiplier(builder,
                "player", 3.0, "players");
            bossRelocationFuelMultiplier = relocationFuelMultiplier(builder,
                "boss", 10.0, "entities in NeoForge's boss tag");
            projectileRelocationFuelMultiplier = relocationFuelMultiplier(builder,
                "projectile", 1.0, "projectiles");
            utilityRelocationFuelMultiplier = relocationFuelMultiplier(builder,
                "utility", 1.0, "vehicles and dropped item entities");
            builder.pop();
            builder.pop();

            builder.push("projectileTransit");
            maximumProjectileTransits = builder.comment(
                    "Maximum successful RiftGun portal transits during one projectile's lifetime.")
                .defineInRange("maximumTransits", 32, 1, 1024);
            enableProjectileSweptCollision = builder.comment(
                    "Detect fast projectiles by intersecting their swept path with indexed portal faces. "
                        + "Disable to reduce overhead; ordinary trigger-box transit remains available.")
                .define("enableSweptCollision", true);
            projectileEffectCooldownTicks = builder.comment(
                    "Minimum ticks between projectile transit effects on one portal side. "
                        + "Zero disables effect coalescing.")
                .defineInRange("effectCooldownTicks", 2, 0, 20);
            builder.pop();

            builder.push("privacy");
            defaultTargetPrivacy = builder.comment(
                    "Default Target privacy for players who have not configured the Privacy Terminal: "
                        + "PUBLIC, REQUEST, or PRIVATE.")
                .defineEnum("defaultTargetPrivacy", dev.riftgun.data.TargetPrivacy.PUBLIC);
            defaultEntityRelocationDestinationPrivacy = builder.comment(
                    "Default policy for opening an Entity Relocation exit above another player.")
                .defineEnum("defaultEntityRelocationDestinationPrivacy",
                    dev.riftgun.data.TargetPrivacy.REQUEST);
            defaultEntityRelocationSubjectPrivacy = builder.comment(
                    "Default policy for including another player as an Entity Relocation subject.")
                .defineEnum("defaultEntityRelocationSubjectPrivacy",
                    dev.riftgun.data.TargetPrivacy.REQUEST);
            defaultForeignExitTransitAllowed = builder.comment(
                    "Whether another player's exit portal may carry a new player's entity tree by default.")
                .define("defaultForeignExitTransitAllowed", true);
            privacyRequestTimeoutSeconds = builder.comment(
                    "How long a privacy request waits for a response, in seconds.")
                .defineInRange("privacyRequestTimeoutSeconds", 10, 5, 300);
            privacyGrantTimeoutSeconds = builder.comment(
                    "How long an Allow Once grant waits for a successful authorized action, in seconds.")
                .defineInRange("privacyGrantTimeoutSeconds", 60, 5, 600);
            privacyDenyOnceCooldownSeconds = builder.comment(
                    "How long Deny Once blocks the same requester from asking again, in seconds. "
                        + "Set to zero to disable the cooldown.")
                .defineInRange("privacyDenyOnceCooldownSeconds", 10, 0, 300);
            builder.pop();

            builder.push("prediction");
            frontProjectionFactor = builder.comment(
                    "Blocks of extra door distance per block/second of velocity projected onto the "
                        + "view axis, for front doors opened in Projection mode.")
                .defineInRange("frontProjectionFactor", 0.7, 0.0, 8.0);
            downshotProjectionFactor = builder.comment(
                    "Blocks of extra door distance per block/second of velocity projected onto the "
                        + "downward axis, for downshot doors opened in Projection mode.")
                .defineInRange("downshotProjectionFactor", 2.5, 0.0, 8.0);
            builder.pop();

            builder.push("crises");
            forceUnstableFluids = builder.comment(
                    "Fluid IDs forced to behave as unstable, in addition to #riftgun:unstable_portal_fluids.")
                .defineListAllowEmpty("forceUnstableFluids", List.of(),
                    () -> "minecraft:water", ServerConfig::validId);
            forceStableFluids = builder.comment(
                    "Fluid IDs forced stable. This list overrides both the tag and forceUnstableFluids.")
                .defineListAllowEmpty("forceStableFluids", List.of(),
                    () -> "minecraft:water", ServerConfig::validId);
            crisisWeights = builder.comment(
                    "Absolute weights out of 1000, formatted as namespace:id=weight. The full set must total at most 1000.")
                .defineListAllowEmpty("weights", List.of(
                    "riftgun:high_altitude_fall=8",
                    "riftgun:lava_hazard=5",
                    "riftgun:spatial_tear=2",
                    "riftgun:weakness=30",
                    "riftgun:nausea=55"
                ), () -> "riftgun:nausea=0", ServerConfig::validWeightEntry);
            maximumCrisisExits = builder.defineInRange("maximumCrisisExitsPerPortal", 4, 0, 32);
            maximumTrackedCrisisPlayers = builder.comment(
                    "Maximum players remembered by an unstable portal pair. The least recently used "
                        + "entry is evicted when full, so an evicted player may be evaluated again.")
                .defineInRange("maximumTrackedPlayersPerPortalPair", 1024, 1, 65536);
            highFallHeight = builder.defineInRange("highFallHeight", 192, 48, 256);
            minimumHighFallDrop = builder.defineInRange("minimumHighFallDrop", 128, 16, 256);
            highFallCooldownTicks = builder.defineInRange("highFallCooldownTicks", 30, 0, 1200);
            guardedHighFallCooldownTicks = builder.defineInRange("guardedHighFallCooldownTicks", 15, 0, 1200);
            lavaSearchRadius = builder.defineInRange("lavaSearchRadius", 24, 1, 64);
            lavaCandidateChecks = builder.defineInRange("lavaCandidateChecks", 96, 1, 1024);
            lavaMinimumArmor = builder.defineInRange("lavaMinimumArmor", 20, 0, 100);
            lavaMinimumFireProtection = builder.defineInRange("lavaMinimumFireProtection", 4, 0, 100);
            spatialTearMinimumHealth = builder.defineInRange("spatialTearMinimumHealth", 16, 1, 2048);
            spatialTearMinimumHealthRatio = builder.defineInRange("spatialTearMinimumHealthRatio", 0.8, 0.0, 1.0);
            spatialTearProtectionTicks = builder.defineInRange("spatialTearProtectionTicks", 30, 0, 1200);
            spatialTearCooldownTicks = builder.defineInRange("spatialTearCooldownTicks", 40, 0, 1200);
            weaknessDurationTicks = builder.defineInRange("weaknessDurationTicks", 1000, 1, 72000);
            weaknessAmplifier = builder.defineInRange("weaknessAmplifier", 0, 0, 255);
            nauseaDurationTicks = builder.defineInRange("nauseaDurationTicks", 160, 1, 72000);
            nauseaAmplifier = builder.defineInRange("nauseaAmplifier", 0, 0, 255);
            nauseaSoundVolume = builder.defineInRange("nauseaSoundVolume", 0.45, 0.0, 4.0);
            nauseaSoundPitch = builder.defineInRange("nauseaSoundPitch", 1.35, 0.01, 4.0);
            builder.pop();

            builder.push("debug");
            enableTransitDiagnostics = builder.comment(
                    "Log detailed portal transit, chunk ticket, and post-teleport diagnostics. "
                        + "Disabled by default; changes take effect when the server config reloads.")
                .define("enableTransitDiagnostics", false);
            builder.pop();
        }

        private static ModConfigSpec.DoubleValue relocationFuelMultiplier(
                ModConfigSpec.Builder builder, String key, double defaultValue, String targets) {
            return builder.comment(
                    "Entity Relocation fuel multiplier for " + targets
                        + ". Zero makes this target category free, but valid portal fluid is still required.")
                .defineInRange(key, defaultValue, 0.0, 100.0);
        }
    }

    private static boolean validId(Object value) {
        return value instanceof String text && net.minecraft.resources.Identifier.tryParse(text) != null;
    }

    private static boolean validWeightEntry(Object value) {
        if (!(value instanceof String text)) return false;
        int separator = text.lastIndexOf('=');
        if (separator <= 0 || net.minecraft.resources.Identifier.tryParse(text.substring(0, separator)) == null) {
            return false;
        }
        try {
            int weight = Integer.parseInt(text.substring(separator + 1));
            return weight >= 0 && weight <= 1000;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private ServerConfig() {}
}
