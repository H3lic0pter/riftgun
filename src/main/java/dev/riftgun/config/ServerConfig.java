package dev.riftgun.config;

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

    public static final class Values {
        public final ModConfigSpec.IntValue maxDestinations;
        public final ModConfigSpec.IntValue maxGroups;
        public final ModConfigSpec.IntValue maxDestinationNameLength;
        public final ModConfigSpec.IntValue maxGroupNameLength;
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
        public final ModConfigSpec.IntValue maximumPortalDurationSeconds;
        public final ModConfigSpec.DoubleValue horizontalTriggerExtend;
        public final ModConfigSpec.EnumValue<dev.riftgun.data.TargetPrivacy> defaultTargetPrivacy;
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
            builder.pop();

            builder.push("portal");
            maximumPortalDurationSeconds = builder.comment(
                    "Maximum fully-open duration selectable on a Portal Gun, in seconds.")
                .defineInRange("maximumDurationSeconds", 15,
                    PortalOpenDuration.MINIMUM_SECONDS, PortalOpenDuration.MAXIMUM_CONFIGURABLE_SECONDS);
            horizontalTriggerExtend = builder.comment(
                    "Extra trigger reach along the normal for flat top/bottom portals, in blocks. "
                        + "Catches falling bodies before their feet touch the ground so fall damage "
                        + "is resolved at the exit.")
                .defineInRange("horizontalTriggerExtend", 0.35, 0.0, 2.0);
            builder.pop();

            builder.push("privacy");
            defaultTargetPrivacy = builder.comment(
                    "Default Target privacy for players who have not configured the Privacy Terminal: "
                        + "PUBLIC, REQUEST, or PRIVATE.")
                .defineEnum("defaultTargetPrivacy", dev.riftgun.data.TargetPrivacy.PUBLIC);
            privacyRequestTimeoutSeconds = builder.comment(
                    "How long a Player Portal request waits for a response, in seconds.")
                .defineInRange("privacyRequestTimeoutSeconds", 30, 5, 300);
            privacyGrantTimeoutSeconds = builder.comment(
                    "How long an Allow Once grant waits for a successful portal opening, in seconds.")
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
            lavaMinimumArmor = builder.defineInRange("lavaMinimumArmor", 16, 0, 100);
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
        }
    }

    private static boolean validId(Object value) {
        return value instanceof String text && net.minecraft.resources.ResourceLocation.tryParse(text) != null;
    }

    private static boolean validWeightEntry(Object value) {
        if (!(value instanceof String text)) return false;
        int separator = text.lastIndexOf('=');
        if (separator <= 0 || net.minecraft.resources.ResourceLocation.tryParse(text.substring(0, separator)) == null) {
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
