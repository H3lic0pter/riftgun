package dev.riftgun.data;

/** Last safety result produced by a real portal-open attempt. */
public enum DestinationSafetyResult {
    UNKNOWN,
    SAFE,
    UNSAFE;

    public static DestinationSafetyResult parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
