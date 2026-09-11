package dev.riftgun.service;

import java.util.function.BooleanSupplier;

/** Polls installation without repeating generation or charging a key twice. */
public final class DeferredDimensionPreparation {
    private static final int TIMEOUT_TICKS = 200;
    private int waitingTicks;
    private boolean started;
    private boolean complete;

    public boolean tick(BooleanSupplier ready, BooleanSupplier pending,
                        DimensionGenerationBudget budget, Runnable generate) {
        if (complete) return true;
        if (ready.getAsBoolean()) {
            complete = true;
            return true;
        }
        if (++waitingTicks > TIMEOUT_TICKS) {
            throw new IllegalArgumentException("message.riftgun.dimension_unavailable");
        }
        if (pending.getAsBoolean()) started = true;
        if (!started && budget.tryAcquire()) {
            // Even a failed upstream operation must not be repeated for this request.
            started = true;
            generate.run();
        }
        return false;
    }
}
