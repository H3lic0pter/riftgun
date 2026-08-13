package dev.riftgun.relocation;

/** Bounded, non-blocking destination preparation with exactly-once cleanup. */
final class EntityRelocationPreparation implements AutoCloseable {
    static final int PREPARING_MESSAGE_DELAY_TICKS = 10;
    private final long startedAt;
    private final long deadline;
    private final Runnable releaseTicket;
    private Outcome terminalOutcome;
    private boolean ticketReleased;
    private boolean preparingMessageShown;

    EntityRelocationPreparation(long startedAt, int timeoutTicks, Runnable releaseTicket) {
        this.startedAt = startedAt;
        long duration = Math.max(1, timeoutTicks);
        deadline = startedAt > Long.MAX_VALUE - duration
            ? Long.MAX_VALUE : startedAt + duration;
        this.releaseTicket = releaseTicket;
    }

    Outcome advance(long now, boolean destinationReady) {
        if (terminalOutcome != null) return terminalOutcome;
        if (destinationReady) return finish(Outcome.READY);
        if (now >= deadline) return finish(Outcome.TIMED_OUT);
        return Outcome.WAITING;
    }

    boolean terminal() {
        return terminalOutcome != null;
    }

    long startedAt() {
        return startedAt;
    }

    boolean shouldShowPreparingMessage(long now) {
        if (terminal() || preparingMessageShown
            || now - startedAt < PREPARING_MESSAGE_DELAY_TICKS) return false;
        preparingMessageShown = true;
        return true;
    }

    boolean preparingMessageShown() {
        return preparingMessageShown;
    }

    @Override
    public void close() {
        releaseTicket();
    }

    private Outcome finish(Outcome outcome) {
        terminalOutcome = outcome;
        releaseTicket();
        return outcome;
    }

    private void releaseTicket() {
        if (ticketReleased) return;
        ticketReleased = true;
        releaseTicket.run();
    }

    enum Outcome {
        WAITING,
        READY,
        TIMED_OUT
    }
}
