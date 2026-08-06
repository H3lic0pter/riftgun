package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import java.util.List;

public final class SwirlVisualOptions {
    public static final double DEFAULT_OUTER_PERIOD = 5.0;
    public static final double DEFAULT_INNER_PERIOD = 10.0;
    public static final double DEFAULT_INWARD_PERIOD = 2.5;

    public static final PortalVisualOptions DESCRIPTOR = new PortalVisualOptions(
        "screen.riftgun.visual.swirl_animation_settings",
        "screen.riftgun.visual.swirl_reset_tooltip",
        List.of(
            new PortalVisualOption.Toggle(
                "screen.riftgun.visual.swirl_animation",
                SwirlVisualOptions::animationEnabled,
                SwirlVisualOptions::setAnimationEnabled,
                true
            ),
            new PortalVisualOption.Range(
                "screen.riftgun.visual.swirl_outer_period",
                SwirlVisualOptions::outerPeriod,
                SwirlVisualOptions::setOuterPeriod,
                SwirlVisualOptions::animationEnabled,
                1.5, 20.0, 0.1, DEFAULT_OUTER_PERIOD
            ),
            new PortalVisualOption.Range(
                "screen.riftgun.visual.swirl_inner_period",
                SwirlVisualOptions::innerPeriod,
                SwirlVisualOptions::setInnerPeriod,
                SwirlVisualOptions::animationEnabled,
                2.0, 30.0, 0.1, DEFAULT_INNER_PERIOD
            ),
            new PortalVisualOption.Range(
                "screen.riftgun.visual.swirl_inward_period",
                SwirlVisualOptions::inwardPeriod,
                SwirlVisualOptions::setInwardPeriod,
                SwirlVisualOptions::animationEnabled,
                0.8, 10.0, 0.1, DEFAULT_INWARD_PERIOD
            )
        )
    );

    public static boolean animationEnabled() {
        return ClientConfig.VALUES.swirlAnimationEnabled.get();
    }

    public static double outerPeriod() {
        return ClientConfig.VALUES.swirlOuterPeriod.get();
    }

    public static double innerPeriod() {
        return ClientConfig.VALUES.swirlInnerPeriod.get();
    }

    public static double inwardPeriod() {
        return ClientConfig.VALUES.swirlInwardPeriod.get();
    }

    public static Snapshot snapshot() {
        return new Snapshot(animationEnabled(), (float) outerPeriod(),
            (float) innerPeriod(), (float) inwardPeriod());
    }

    private static void setAnimationEnabled(boolean enabled) {
        ClientConfig.VALUES.swirlAnimationEnabled.set(enabled);
    }

    private static void setOuterPeriod(double seconds) {
        ClientConfig.VALUES.swirlOuterPeriod.set(seconds);
    }

    private static void setInnerPeriod(double seconds) {
        ClientConfig.VALUES.swirlInnerPeriod.set(seconds);
    }

    private static void setInwardPeriod(double seconds) {
        ClientConfig.VALUES.swirlInwardPeriod.set(seconds);
    }

    public record Snapshot(boolean animationEnabled, float outerPeriod,
                           float innerPeriod, float inwardPeriod) {}

    private SwirlVisualOptions() {}
}
