package dev.riftgun.client;

/** Client-session-only memory for the main portal GUI scroll positions. */
public final class PortalGuiScrollMemory {
    private static int listScroll;
    private static int detailScroll;

    public static Position restore(boolean enabled) {
        if (!enabled) clear();
        return new Position(listScroll, detailScroll);
    }

    public static void remember(boolean enabled, int nextListScroll, int nextDetailScroll) {
        if (!enabled) {
            clear();
            return;
        }
        listScroll = Math.max(0, nextListScroll);
        detailScroll = Math.max(0, nextDetailScroll);
    }

    public static void clear() {
        listScroll = 0;
        detailScroll = 0;
    }

    public record Position(int listScroll, int detailScroll) {}

    private PortalGuiScrollMemory() {}
}
