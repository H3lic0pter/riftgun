package dev.riftgun.config;

import net.neoforged.neoforge.common.ModConfigSpec;

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
            builder.pop();
        }
    }

    private ServerConfig() {}
}
