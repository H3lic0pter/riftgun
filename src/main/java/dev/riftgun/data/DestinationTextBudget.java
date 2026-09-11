package dev.riftgun.data;

import java.nio.charset.StandardCharsets;

/** Bounds newly saved text while allowing existing saves to load without data loss. */
public final class DestinationTextBudget {
    public static final int MAX_PLAYER_BYTES = 256 * 1024;

    public static boolean canAdd(PortalPlayerData data, String text) {
        long bytes = utf8Bytes(text);
        for (Destination destination : data.destinations()) {
            if (destination.infinityText() != null) bytes += utf8Bytes(destination.infinityText());
            if (bytes > MAX_PLAYER_BYTES) return false;
        }
        return bytes <= MAX_PLAYER_BYTES;
    }

    private static int utf8Bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    private DestinationTextBudget() {}
}
