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

        private Values(ModConfigSpec.Builder builder) {
            builder.push("destinations");
            maxDestinations = builder.defineInRange("maxDestinationsPerPlayer", 256, 1, 4096);
            maxGroups = builder.defineInRange("maxGroupsPerPlayer", 32, 0, 512);
            maxDestinationNameLength = builder.defineInRange("maxDestinationNameLength", 48, 1, 128);
            maxGroupNameLength = builder.defineInRange("maxGroupNameLength", 32, 1, 64);
            builder.pop();
        }
    }

    private ServerConfig() {}
}

