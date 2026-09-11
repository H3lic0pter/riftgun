package dev.riftgun.service;

/** One instance per server tick, shared by every queued search in that tick. */
public final class DimensionGenerationBudget {
    private boolean claimed;

    public boolean tryAcquire() {
        if (claimed) return false;
        claimed = true;
        return true;
    }
}
