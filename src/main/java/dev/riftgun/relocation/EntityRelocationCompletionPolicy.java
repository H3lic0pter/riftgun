package dev.riftgun.relocation;

/** Transaction-clock completion policy, independent of whether the exit visual gets entity ticks. */
final class EntityRelocationCompletionPolicy {
    static Decision decide(long elapsedTicks, int openingTicks, int timeoutTicks,
                           boolean exitPresent, boolean destinationTicking) {
        if (!exitPresent) return Decision.FAILED;
        if (elapsedTicks >= Math.max(1, timeoutTicks)) return Decision.TIMED_OUT;
        if (elapsedTicks < Math.max(1, openingTicks) || !destinationTicking) {
            return Decision.WAITING;
        }
        return Decision.READY;
    }

    enum Decision {
        WAITING,
        READY,
        FAILED,
        TIMED_OUT
    }

    private EntityRelocationCompletionPolicy() {}
}
