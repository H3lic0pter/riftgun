package dev.riftgun.core.config;

/** Loader-neutral, immutable client visual preferences. */
public record ClientVisualConfig(
    String portalVisualType,
    boolean swirlAnimationEnabled,
    double swirlOuterPeriod,
    double swirlInnerPeriod,
    double swirlInwardPeriod,
    boolean swirlInwardDirection,
    int portalDynamicLightLevel
) {
    public static ClientVisualConfig defaults() {
        return new ClientVisualConfig("riftgun:swirl", true,
            20.0, 20.0, 2.5, true, 9);
    }
}
