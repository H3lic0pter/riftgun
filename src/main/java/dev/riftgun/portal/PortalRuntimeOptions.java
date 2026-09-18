package dev.riftgun.portal;

import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.api.PortalTransitAuthorization;
import dev.riftgun.module.PortalEntityAccessSnapshot;
import dev.riftgun.sound.PortalSoundSnapshot;
import java.util.Optional;

/** Immutable behavior snapshot shared by both ends of one portal pair. */
public record PortalRuntimeOptions(
    PortalEntityAccessSnapshot entityAccess,
    int openDurationTicks,
    PortalAperture aperture,
    int transitCooldownTicks,
    boolean fallGuard,
    boolean entityFallGuard,
    double horizontalTriggerExtend,
    PortalSoundSnapshot sounds,
    PortalCrisisConfigurationSnapshot crises,
    Optional<PortalTransitAuthorization> transitAuthorization,
    String visualType,
    int displayRgb
) {
    public PortalRuntimeOptions(PortalEntityAccessSnapshot entityAccess, int openDurationTicks,
            PortalAperture aperture, int transitCooldownTicks, boolean fallGuard, boolean entityFallGuard,
            double horizontalTriggerExtend, PortalSoundSnapshot sounds,
            PortalCrisisConfigurationSnapshot crises, Optional<PortalTransitAuthorization> transitAuthorization,
            String visualType) {
        this(entityAccess, openDurationTicks, aperture, transitCooldownTicks, fallGuard, entityFallGuard,
            horizontalTriggerExtend, sounds, crises, transitAuthorization, visualType, -1);
    }

    public PortalRuntimeOptions withDisplayRgb(int rgb) {
        return new PortalRuntimeOptions(entityAccess, openDurationTicks, aperture, transitCooldownTicks,
            fallGuard, entityFallGuard, horizontalTriggerExtend, sounds, crises, transitAuthorization,
            visualType, rgb & 0xFFFFFF);
    }

    public PortalRuntimeOptions {
        if (entityAccess == null) entityAccess = PortalEntityAccessSnapshot.NONE;
        openDurationTicks = Math.max(1, openDurationTicks);
        if (aperture == null) aperture = PortalAperture.STANDARD;
        transitCooldownTicks = Math.max(0, transitCooldownTicks);
        horizontalTriggerExtend = Math.max(0.0, horizontalTriggerExtend);
        if (sounds == null) sounds = PortalSoundSnapshot.defaults();
        if (crises == null) crises = PortalCrisisConfigurationSnapshot.stable();
        if (transitAuthorization == null) transitAuthorization = Optional.empty();
        if (!dev.riftgun.appearance.PortalGunSkin.validId(visualType)) {
            visualType = dev.riftgun.appearance.GunPresentation.DEFAULT_VISUAL;
        }
    }

    public PortalRuntimeOptions(PortalEntityAccessSnapshot entityAccess, int openDurationTicks,
            PortalAperture aperture, int transitCooldownTicks, boolean fallGuard, boolean entityFallGuard,
            double horizontalTriggerExtend, PortalSoundSnapshot sounds,
            PortalCrisisConfigurationSnapshot crises, Optional<PortalTransitAuthorization> transitAuthorization) {
        this(entityAccess, openDurationTicks, aperture, transitCooldownTicks, fallGuard, entityFallGuard,
            horizontalTriggerExtend, sounds, crises, transitAuthorization,
            dev.riftgun.appearance.GunPresentation.DEFAULT_VISUAL);
    }
}
