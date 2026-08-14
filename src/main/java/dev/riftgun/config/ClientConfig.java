package dev.riftgun.config;

import dev.riftgun.core.config.ClientVisualConfig;
import dev.riftgun.core.config.RiftConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final Values VALUES;

    static {
        var configured = new ModConfigSpec.Builder().configure(Values::new);
        VALUES = configured.getLeft();
        SPEC = configured.getRight();
    }

    /** Rebuilds and atomically publishes a complete loader-neutral snapshot. */
    public static void publishSnapshot() {
        RiftConfigs.publishClient(new ClientVisualConfig(
            VALUES.portalVisualType.get(), VALUES.swirlAnimationEnabled.get(),
            VALUES.swirlOuterPeriod.get(), VALUES.swirlInnerPeriod.get(),
            VALUES.swirlInwardPeriod.get(), VALUES.swirlInwardDirection.get(),
            VALUES.portalDynamicLightLevel.get()));
    }

    public static final class Values {
        public final ModConfigSpec.ConfigValue<String> portalVisualType;
        public final ModConfigSpec.BooleanValue swirlAnimationEnabled;
        public final ModConfigSpec.DoubleValue swirlOuterPeriod;
        public final ModConfigSpec.DoubleValue swirlInnerPeriod;
        public final ModConfigSpec.DoubleValue swirlInwardPeriod;
        public final ModConfigSpec.BooleanValue swirlInwardDirection;
        public final ModConfigSpec.IntValue portalDynamicLightLevel;

        private Values(ModConfigSpec.Builder builder) {
            portalVisualType = builder.comment("Client-local portal visual type ID")
                .define("portalVisualType", "riftgun:swirl");

            builder.push("visuals").push("swirl");
            swirlAnimationEnabled = builder.comment("Animate the swirl portal texture")
                .define("animationEnabled", true);
            swirlOuterPeriod = builder.comment("Outer rotation period in seconds")
                .defineInRange("outerPeriod", 20.0, 1.5, 40.0);
            swirlInnerPeriod = builder.comment("Inner rotation period in seconds")
                .defineInRange("innerPeriod", 20.0, 2.0, 45.0);
            swirlInwardPeriod = builder.comment("Inward highlight period in seconds")
                .defineInRange("inwardPeriod", 2.5, 0.8, 10.0);
            swirlInwardDirection = builder.comment("Inward flow direction of the swirl animation; true flows toward the center")
                .define("inwardDirection", true);
            builder.pop(2);

            builder.push("visuals").push("dynamicLighting");
            portalDynamicLightLevel = builder.comment(
                    "Maximum portal luminance used by optional dynamic-light integrations")
                .defineInRange("level", 9, 0, 15);
            builder.pop(2);
        }
    }

    private ClientConfig() {}
}
