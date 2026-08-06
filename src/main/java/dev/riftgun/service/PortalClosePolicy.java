package dev.riftgun.service;

import dev.riftgun.portal.PortalEntity;
import dev.riftgun.portal.PortalLifecycle;

@FunctionalInterface
public interface PortalClosePolicy {
    boolean shouldClose(PortalLifecycle.Phase phase, int phaseTicks, int openDurationTicks);

    default boolean shouldClose(PortalEntity portal) {
        return shouldClose(portal.phase(), portal.phaseTicks(), portal.openDurationTicks());
    }
}
