package dev.riftgun.portal;

public final class PortalOpenDuration {
    public static final int DEFAULT_SECONDS = 3;
    public static final int MINIMUM_SECONDS = 1;
    public static final int MAXIMUM_CONFIGURABLE_SECONDS = 300;
    public static final int ETERNAL_SECONDS = 100_000;
    public static final int TICKS_PER_SECOND = 20;

    public static int effectiveSeconds(int desiredSeconds, int maximumSeconds) {
        // The eternal sentinel bypasses any configured cap.
        if (desiredSeconds >= ETERNAL_SECONDS) return ETERNAL_SECONDS;
        int maximum = Math.clamp(maximumSeconds, MINIMUM_SECONDS, MAXIMUM_CONFIGURABLE_SECONDS);
        return Math.clamp(desiredSeconds, MINIMUM_SECONDS, maximum);
    }

    /** Applies the installed-module policy to an untrusted duration request. */
    public static int authorizedSeconds(int requestedSeconds, int maximumSeconds,
                                        boolean eternalInstalled) {
        if (requestedSeconds > MAXIMUM_CONFIGURABLE_SECONDS) {
            if (eternalInstalled) return ETERNAL_SECONDS;
            return effectiveSeconds(MAXIMUM_CONFIGURABLE_SECONDS, maximumSeconds);
        }
        return effectiveSeconds(requestedSeconds, maximumSeconds);
    }

    /** Tick count; eternal durations saturate to the int max so the close policy never fires. */
    public static int ticks(int seconds) {
        if (seconds >= ETERNAL_SECONDS) return Integer.MAX_VALUE;
        return Math.clamp(seconds, MINIMUM_SECONDS, MAXIMUM_CONFIGURABLE_SECONDS) * TICKS_PER_SECOND;
    }

    public static boolean isEternal(int seconds) {
        return seconds >= ETERNAL_SECONDS;
    }

    private PortalOpenDuration() {}
}
