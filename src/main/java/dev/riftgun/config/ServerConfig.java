package dev.riftgun.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import dev.riftgun.portal.PortalOpenDuration;

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

        private Values(ModConfigSpec.Builder builder) {
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
                .defineInRange("downshotProjectionFactor", 1.0, 0.0, 8.0);
            builder.pop();
        }
    }

    private ServerConfig() {}
}
