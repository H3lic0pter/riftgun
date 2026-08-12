package dev.riftgun.client.screen;

/** Pure layout helper for horizontally scrolling text inside a fixed-width GUI region. */
final class GuiTextMarquee {
    private static final long PAUSE_MILLIS = 900L;
    private static final double PIXELS_PER_SECOND = 24.0;

    static int offset(int textWidth, int availableWidth, long nowMillis) {
        int overflow = Math.max(0, textWidth - Math.max(0, availableWidth));
        if (overflow == 0) return 0;
        long travelMillis = Math.max(1L, Math.round(overflow * 1000.0 / PIXELS_PER_SECOND));
        long cycle = PAUSE_MILLIS * 2L + travelMillis * 2L;
        long phase = Math.floorMod(nowMillis, cycle);
        if (phase < PAUSE_MILLIS) return 0;
        phase -= PAUSE_MILLIS;
        if (phase < travelMillis) return interpolated(phase, travelMillis, overflow);
        phase -= travelMillis;
        if (phase < PAUSE_MILLIS) return overflow;
        phase -= PAUSE_MILLIS;
        return overflow - interpolated(phase, travelMillis, overflow);
    }

    private static int interpolated(long elapsed, long duration, int distance) {
        return (int) Math.round(distance * Math.clamp((double) elapsed / duration, 0.0, 1.0));
    }

    private GuiTextMarquee() {}
}
