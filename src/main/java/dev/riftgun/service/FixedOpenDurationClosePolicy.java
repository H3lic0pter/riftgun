package dev.riftgun.service;

import dev.riftgun.portal.PortalLifecycle;

public final class FixedOpenDurationClosePolicy implements PortalClosePolicy {
    public static final int OPEN_TICKS = 60;

    @Override
    public boolean shouldClose(PortalLifecycle.Phase phase, int phaseTicks) {
        return phase == PortalLifecycle.Phase.OPEN && phaseTicks >= OPEN_TICKS;
    }
}
