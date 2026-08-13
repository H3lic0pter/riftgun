package dev.riftgun.portal;

/** Derives both portal endpoints' lifecycle from one server clock. */
public final class PortalPairClock {
    public static PortalLifecycle.Phase phase(long startedAt, long closeStartedAt, long now) {
        if (closeStartedAt >= 0L) {
            return now - closeStartedAt >= PortalLifecycle.ANIMATION_TICKS
                ? PortalLifecycle.Phase.CLOSED
                : PortalLifecycle.Phase.CLOSING;
        }

        long elapsed = Math.max(0L, now - startedAt);
        if (elapsed < PortalLifecycle.CHARGE_TICKS) {
            return PortalLifecycle.Phase.CHARGING;
        }
        if (elapsed < PortalLifecycle.CHARGE_TICKS + PortalLifecycle.ANIMATION_TICKS) {
            return PortalLifecycle.Phase.OPENING;
        }
        return PortalLifecycle.Phase.OPEN;
    }

    public static int phaseTicks(long startedAt, long closeStartedAt, long now) {
        PortalLifecycle.Phase phase = phase(startedAt, closeStartedAt, now);
        return switch (phase) {
            case CHARGING -> saturatedInt(Math.max(0L, now - startedAt));
            case OPENING -> saturatedInt(Math.max(0L,
                now - startedAt - PortalLifecycle.CHARGE_TICKS));
            case OPEN -> saturatedInt(Math.max(0L,
                now - startedAt - PortalLifecycle.CHARGE_TICKS - PortalLifecycle.ANIMATION_TICKS));
            case CLOSING -> saturatedInt(Math.max(0L, now - closeStartedAt));
            case CLOSED -> 0;
        };
    }

    /** Clock origin that makes an already-open endpoint start with a full open duration. */
    public static long openPhaseStartedAt(long now) {
        return now - PortalLifecycle.CHARGE_TICKS - PortalLifecycle.ANIMATION_TICKS;
    }

    /** Either ticking endpoint may finish synchronizing a deferred pair. */
    public static boolean shouldSynchronize(boolean waitingForLinkedOpen,
                                            boolean synchronizePairOnOpen) {
        return waitingForLinkedOpen || synchronizePairOnOpen;
    }

    private static int saturatedInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private PortalPairClock() {}
}
