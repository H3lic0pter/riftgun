package dev.riftgun.relocation;

import dev.riftgun.portal.PortalLifecycle;

/** Entity-relocation policy layered on the normal portal opening/open/closing phases. */
public final class EntityRelocationLifecycle {
    public static final int OPENING_TICKS = PortalLifecycle.ANIMATION_TICKS;
    public static final int CLOSING_TICKS = PortalLifecycle.ANIMATION_TICKS;

    public static boolean shouldTransit(int ageTicks) {
        return ageTicks == OPENING_TICKS;
    }

    public static boolean shouldTransit(int ageTicks, int openingTicks) {
        return ageTicks == Math.max(1, openingTicks);
    }

    public static boolean shouldDeferExit(boolean playerDestination, boolean destinationTicking) {
        return !playerDestination && !destinationTicking;
    }

    public static boolean shouldBeginClosing(PortalLifecycle.Phase phase, int phaseTicks,
                                             int openDurationTicks, int reservations) {
        return phase == PortalLifecycle.Phase.OPEN && reservations <= 0
            && phaseTicks >= Math.max(1, openDurationTicks);
    }

    public static int remainingOpenTicks(PortalLifecycle.Phase phase, int phaseTicks,
                                         int openDurationTicks) {
        return phase == PortalLifecycle.Phase.OPEN
            ? Math.max(0, Math.max(1, openDurationTicks) - phaseTicks) : 0;
    }

    public static float visibleProgress(PortalLifecycle.Phase phase, int phaseTicks,
                                        float partialTick) {
        return PortalLifecycle.visibleProgress(phase, phaseTicks, partialTick);
    }

    public static float visibleProgress(PortalLifecycle.Phase phase, int phaseTicks,
                                        float partialTick, int openingTicks) {
        return PortalLifecycle.visibleProgress(phase, phaseTicks, partialTick,
            openingTicks, CLOSING_TICKS);
    }

    private EntityRelocationLifecycle() {}
}
