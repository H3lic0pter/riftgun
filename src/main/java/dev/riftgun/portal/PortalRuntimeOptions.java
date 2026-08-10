package dev.riftgun.portal;

import dev.riftgun.crisis.PortalCrisisConfigurationSnapshot;
import dev.riftgun.module.PortalEntityAccessSnapshot;
import dev.riftgun.sound.PortalSoundSnapshot;

/** Immutable behavior snapshot shared by both ends of one portal pair. */
public record PortalRuntimeOptions(
    PortalEntityAccessSnapshot entityAccess,
    int openDurationTicks,
    PortalAperture aperture,
    int transitCooldownTicks,
    boolean fallGuard,
    double horizontalTriggerExtend,
    PortalSoundSnapshot sounds,
    PortalCrisisConfigurationSnapshot crises
) {
    public PortalRuntimeOptions {
        if (entityAccess == null) entityAccess = PortalEntityAccessSnapshot.NONE;
        openDurationTicks = Math.max(1, openDurationTicks);
        if (aperture == null) aperture = PortalAperture.STANDARD;
        transitCooldownTicks = Math.max(0, transitCooldownTicks);
        horizontalTriggerExtend = Math.max(0.0, horizontalTriggerExtend);
        if (sounds == null) sounds = PortalSoundSnapshot.defaults();
        if (crises == null) crises = PortalCrisisConfigurationSnapshot.stable();
    }
}
