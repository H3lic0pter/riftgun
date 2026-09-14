package dev.riftgun.client.render;

import dev.riftgun.config.ClientConfig;
import dev.riftgun.core.config.RiftConfigs;
import java.util.List;

/** Reuses the visual settings screen and its debounced config persistence. */
public final class MagicCircleVisualOptions {
    public static final PortalVisualOptions DESCRIPTOR = new PortalVisualOptions(
        "screen.riftgun.visual.magic_circle_settings", "screen.riftgun.visual.magic_circle_reset",
        List.of(
            new PortalVisualOption.Range("screen.riftgun.visual.magic_outer_period",
                () -> RiftConfigs.client().magicOuterPeriod(), value -> {
                    ClientConfig.VALUES.magicOuterPeriod.set(value);
                    ClientConfig.publishSnapshot();
                }, () -> true, 2.0, 60.0, 0.5, 20.0),
            new PortalVisualOption.Range("screen.riftgun.visual.magic_inner_period",
                () -> RiftConfigs.client().magicInnerPeriod(), value -> {
                    ClientConfig.VALUES.magicInnerPeriod.set(value);
                    ClientConfig.publishSnapshot();
                }, () -> true, 2.0, 60.0, 0.5, 15.0),
            new PortalVisualOption.Toggle("screen.riftgun.visual.magic_outer_counterclockwise",
                () -> RiftConfigs.client().magicOuterCounterclockwise(), value -> {
                    ClientConfig.VALUES.magicOuterCounterclockwise.set(value);
                    ClientConfig.publishSnapshot();
                }, false, () -> true),
            new PortalVisualOption.Toggle("screen.riftgun.visual.magic_inner_counterclockwise",
                () -> RiftConfigs.client().magicInnerCounterclockwise(), value -> {
                    ClientConfig.VALUES.magicInnerCounterclockwise.set(value);
                    ClientConfig.publishSnapshot();
                }, true, () -> true)
        ));

    private MagicCircleVisualOptions() {}
}
