package dev.riftgun.portal;

public final class PortalOpenDuration {
    public static final int DEFAULT_SECONDS = 3;
    public static final int MINIMUM_SECONDS = 1;
    public static final int MAXIMUM_CONFIGURABLE_SECONDS = 300;
    public static final int TICKS_PER_SECOND = 20;

    public static int effectiveSeconds(int desiredSeconds, int maximumSeconds) {
        int maximum = Math.clamp(maximumSeconds, MINIMUM_SECONDS, MAXIMUM_CONFIGURABLE_SECONDS);
        return Math.clamp(desiredSeconds, MINIMUM_SECONDS, maximum);
    }

    public static int ticks(int seconds) {
        return Math.clamp(seconds, MINIMUM_SECONDS, MAXIMUM_CONFIGURABLE_SECONDS) * TICKS_PER_SECOND;
    }

    private PortalOpenDuration() {}
}
