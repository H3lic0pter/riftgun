package dev.riftgun.service;

import dev.riftgun.portal.PortalLifecycle;
import dev.riftgun.portal.PortalOpenDuration;

public final class FixedOpenDurationClosePolicy implements PortalClosePolicy {
    public static final int DEFAULT_OPEN_TICKS = PortalOpenDuration.ticks(PortalOpenDuration.DEFAULT_SECONDS);

    @Override
    public boolean shouldClose(PortalLifecycle.Phase phase, int phaseTicks, int openDurationTicks) {
        return phase == PortalLifecycle.Phase.OPEN && phaseTicks >= Math.max(1, openDurationTicks);
    }
}
