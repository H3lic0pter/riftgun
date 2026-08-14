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
) {}
