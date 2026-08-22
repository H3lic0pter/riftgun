package dev.riftgun.portal;

/** Decides whether a linked portal may receive transit before its chunk starts ticking. */
public enum PortalTransitReadiness {
    READY(true),
    ASYNC_CHUNK_LOADING(true),
    BLOCKED_CHARGING(false),
    BLOCKED_OPENING(false),
    BLOCKED_CLOSING(false),
    BLOCKED_CLOSED(false);

    private final boolean ready;

    PortalTransitReadiness(boolean ready) {
        this.ready = ready;
    }

    public boolean ready() {
        return ready;
    }

    public String diagnosticReason() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static PortalTransitReadiness evaluate(PortalLifecycle.Phase phase,
                                                   boolean targetTicking,
                                                   boolean allowAsyncChunkLoading) {
        if (phase == PortalLifecycle.Phase.OPEN) return READY;
        if (allowAsyncChunkLoading && !targetTicking) {
            if (phase == PortalLifecycle.Phase.CHARGING
                    || phase == PortalLifecycle.Phase.OPENING) {
                return ASYNC_CHUNK_LOADING;
            }
        }
        return switch (phase) {
            case CHARGING -> BLOCKED_CHARGING;
            case OPENING -> BLOCKED_OPENING;
            case CLOSING -> BLOCKED_CLOSING;
            case CLOSED -> BLOCKED_CLOSED;
            case OPEN -> READY;
        };
    }
}
