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
            VALUES.endframeRotationEnabled.get(), VALUES.endframeRotationPeriod.get(),
            VALUES.endframeRotationReverse.get(),
            VALUES.portalDynamicLightLevel.get()));
    }

    public static final class Values {
        public final ModConfigSpec.ConfigValue<String> portalVisualType;
        public final ModConfigSpec.BooleanValue swirlAnimationEnabled;
        public final ModConfigSpec.DoubleValue swirlOuterPeriod;
        public final ModConfigSpec.DoubleValue swirlInnerPeriod;
        public final ModConfigSpec.DoubleValue swirlInwardPeriod;
        public final ModConfigSpec.BooleanValue swirlInwardDirection;
        public final ModConfigSpec.BooleanValue endframeRotationEnabled;
        public final ModConfigSpec.DoubleValue endframeRotationPeriod;
        public final ModConfigSpec.BooleanValue endframeRotationReverse;
        public final ModConfigSpec.IntValue portalDynamicLightLevel;
        public final ModConfigSpec.BooleanValue rememberGuiScrollPosition;
        public final ModConfigSpec.BooleanValue journeyMapWaypointsEnabled;
        public final ModConfigSpec.BooleanValue xaeroWaypointsEnabled;
        public final ModConfigSpec.IntValue maximumMapWaypoints;

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

            builder.push("visuals").push("endframe");
            endframeRotationEnabled = builder.comment("Rotate the endframe portal frame texture like a vortex")
                .define("rotationEnabled", true);
            endframeRotationPeriod = builder.comment("Endframe frame rotation period in seconds per full turn")
                .defineInRange("rotationPeriod", 20.0, 2.0, 60.0);
            endframeRotationReverse = builder.comment("Reverse the endframe frame rotation direction")
                .define("rotationReverse", false);
            builder.pop(2);

            builder.push("visuals").push("dynamicLighting");
            portalDynamicLightLevel = builder.comment(
                    "Maximum portal luminance used by optional dynamic-light integrations")
                .defineInRange("level", 9, 0, 15);
            builder.pop(2);

            builder.push("gui");
            rememberGuiScrollPosition = builder.comment(
                    "Remember the main portal GUI list and detail scroll positions for this client session")
                .define("rememberScrollPosition", true);
            builder.pop();

            builder.push("mapIntegration");
            journeyMapWaypointsEnabled = builder.comment("Show read-only JourneyMap waypoints")
                .define("journeyMapEnabled", true);
            xaeroWaypointsEnabled = builder.comment("Show experimental read-only Xaero's Minimap waypoints")
                .define("xaeroMinimapEnabled", true);
            maximumMapWaypoints = builder.comment("Maximum displayed waypoints per installed map mod")
                .defineInRange("maximumWaypointsPerSource", 100, 1, 1000);
            builder.pop();
        }
    }

    private ClientConfig() {}
}
