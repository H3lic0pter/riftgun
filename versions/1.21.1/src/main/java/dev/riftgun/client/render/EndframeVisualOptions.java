package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import dev.riftgun.core.config.RiftConfigs;
import java.util.List;

/** GUI options for the end-frame portal visual, mirroring the swirl's layout. */
public final class EndframeVisualOptions {
    public static final double DEFAULT_ROTATION_PERIOD = 20.0;

    public static final PortalVisualOptions DESCRIPTOR = new PortalVisualOptions(
        "screen.riftgun.visual.endframe_rotation_settings",
        "screen.riftgun.visual.endframe_reset_tooltip",
        List.of(
            new PortalVisualOption.Toggle(
                "screen.riftgun.visual.endframe_rotation",
                EndframeVisualOptions::rotationEnabled,
                EndframeVisualOptions::setRotationEnabled,
                true,
                () -> true
            ),
            new PortalVisualOption.Range(
                "screen.riftgun.visual.endframe_rotation_period",
                EndframeVisualOptions::rotationPeriod,
                EndframeVisualOptions::setRotationPeriod,
                EndframeVisualOptions::rotationEnabled,
                2.0, 60.0, 0.5, DEFAULT_ROTATION_PERIOD
            ),
            new PortalVisualOption.Toggle(
                "screen.riftgun.visual.endframe_rotation_reverse",
                EndframeVisualOptions::rotationReverse,
                EndframeVisualOptions::setRotationReverse,
                false,
                EndframeVisualOptions::rotationEnabled
            )
        )
    );

    public static boolean rotationEnabled() {
        return RiftConfigs.client().endframeRotationEnabled();
    }

    public static double rotationPeriod() {
        return RiftConfigs.client().endframeRotationPeriod();
    }

    public static boolean rotationReverse() {
        return RiftConfigs.client().endframeRotationReverse();
    }

    public static void setRotationEnabled(boolean enabled) {
        ClientConfig.VALUES.endframeRotationEnabled.set(enabled);
        ClientConfig.publishSnapshot();
    }

    public static void setRotationPeriod(double seconds) {
        ClientConfig.VALUES.endframeRotationPeriod.set(seconds);
        ClientConfig.publishSnapshot();
    }

    public static void setRotationReverse(boolean reverse) {
        ClientConfig.VALUES.endframeRotationReverse.set(reverse);
        ClientConfig.publishSnapshot();
    }

    private EndframeVisualOptions() {}
}
