package dev.riftgun.portal;

/** State decision captured by prototype/portal-state-machine. */
public final class PortalLifecycle {
    public static final int CHARGE_TICKS = 6;
    public static final int ANIMATION_TICKS = 5;

    public enum Phase {
        CHARGING,
        OPENING,
        OPEN,
        CLOSING,
        CLOSED;

        public static Phase byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : CLOSED;
        }
    }

    public record Step(Phase phase, int phaseTicks) {}

    public static Step tick(Phase phase, int phaseTicks) {
        return tick(phase, phaseTicks, ANIMATION_TICKS, ANIMATION_TICKS);
    }

    public static Step tick(Phase phase, int phaseTicks,
                            int openingTicks, int closingTicks) {
        int boundedOpeningTicks = Math.max(1, openingTicks);
        int boundedClosingTicks = Math.max(1, closingTicks);
        return switch (phase) {
            case CHARGING -> phaseTicks + 1 >= CHARGE_TICKS
                ? new Step(Phase.OPENING, 0)
                : new Step(phase, phaseTicks + 1);
            case OPENING -> phaseTicks + 1 >= boundedOpeningTicks
                ? new Step(Phase.OPEN, 0)
                : new Step(phase, phaseTicks + 1);
            case CLOSING -> phaseTicks + 1 >= boundedClosingTicks
                ? new Step(Phase.CLOSED, 0)
                : new Step(phase, phaseTicks + 1);
            case OPEN -> new Step(phase, phaseTicks + 1);
            case CLOSED -> new Step(phase, phaseTicks);
        };
    }

    public static float visibleProgress(Phase phase, int phaseTicks, float partialTick) {
        return visibleProgress(phase, phaseTicks, partialTick, ANIMATION_TICKS, ANIMATION_TICKS);
    }

    public static float visibleProgress(Phase phase, int phaseTicks, float partialTick,
                                        int openingTicks, int closingTicks) {
        int boundedOpeningTicks = Math.max(1, openingTicks);
        int boundedClosingTicks = Math.max(1, closingTicks);
        return switch (phase) {
            case CHARGING, CLOSED -> 0.0F;
            case OPENING -> clamp((phaseTicks + partialTick) / boundedOpeningTicks);
            case OPEN -> 1.0F;
            case CLOSING -> clamp(1.0F - (phaseTicks + partialTick) / boundedClosingTicks);
        };
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private PortalLifecycle() {}
}
