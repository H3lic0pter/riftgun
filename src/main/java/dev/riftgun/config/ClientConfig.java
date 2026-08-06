package dev.riftgun.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    static {
        var configured = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = configured.getLeft();
        SPEC = configured.getRight();
    }

    public static final class Values {
        public final ModConfigSpec.ConfigValue<String> portalVisualType;
        public final ModConfigSpec.BooleanValue swirlAnimationEnabled;
        public final ModConfigSpec.DoubleValue swirlOuterPeriod;
        public final ModConfigSpec.DoubleValue swirlInnerPeriod;
        public final ModConfigSpec.DoubleValue swirlInwardPeriod;

        private Values(ModConfigSpec.Builder builder) {
            portalVisualType = builder.comment("Client-local portal visual type ID")
                .define("portalVisualType", "riftgun:classic");

            builder.push("visuals").push("swirl");
            swirlAnimationEnabled = builder.comment("Animate the swirl portal texture")
                .define("animationEnabled", true);
            swirlOuterPeriod = builder.comment("Outer rotation period in seconds")
                .defineInRange("outerPeriod", 5.0, 1.5, 20.0);
            swirlInnerPeriod = builder.comment("Inner rotation period in seconds")
                .defineInRange("innerPeriod", 10.0, 2.0, 30.0);
            swirlInwardPeriod = builder.comment("Inward highlight period in seconds")
                .defineInRange("inwardPeriod", 2.5, 0.8, 10.0);
            builder.pop(2);
        }
    }

    private ClientConfig() {}
}
