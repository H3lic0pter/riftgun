package dev.riftgun.ui;

/** Pure geometry and scrolling policy shared by both portal configuration screens. */
public final class PortalConfigLayout {
    public static final int HEADER_HEIGHT = 48;
    public static final int FOOTER_HEIGHT = 36;
    public static final int ROW_HEIGHT = 18;
    public static final int ROW_ACTION_SIZE = 14;
    public static final int DETAIL_LINE_HEIGHT = 31;

    public static Box modalBox(PortalConfigPage page, int screenWidth, int screenHeight,
                               int panelWidth, int descriptionExtraHeight) {
        int boxWidth = Math.min(340, panelWidth - 16);
        int desiredHeight = switch (page) {
            case CREATE_COORDINATE, EDIT_DESTINATION -> 214;
            case CREATE_CURRENT -> 164;
            case SETTINGS -> 201;
            case CONFIRM_SETTINGS -> 140;
            case MAP_INTEGRATION_SETTINGS -> 170;
            case GUN_SETTINGS, PORTAL_DURATION_SETTINGS, SMART_DISTANCE_SETTINGS,
                 APERTURE_SETTINGS, PLAYER_TARGET_SETTINGS, FALL_GUARD_SETTINGS,
                 ENTITY_RELOCATION_SETTINGS, PORTAL_PAIRING_SETTINGS -> 132;
            case REMOTE_SETTINGS -> 180;
            case ENTITY_TRANSIT_SETTINGS -> 163;
            case VISUAL_SETTINGS -> 132;
            case SWIRL_ANIMATION_SETTINGS -> 210;
            case SOUND_SETTINGS -> 178;
            case CREATE_GROUP, RENAME_GROUP, CONFIRM_DELETE_DESTINATION, CONFIRM_DELETE_GROUP,
                 CONFIRM_DIRTY, CONFIRM_CLEAR_FLUID -> 112;
            case SHARE_DESTINATION -> 132;
            case NONE -> 0;
        };
        if (page.isGunSettingPage()) desiredHeight += descriptionExtraHeight;
        int boxHeight = Math.min(desiredHeight, screenHeight - 8);
        return new Box((screenWidth - boxWidth) / 2, (screenHeight - boxHeight) / 2,
            boxWidth, boxHeight);
    }

    public static Box dropdownBox(Box modal, int selectorX, int selectorY, int selectorWidth,
                                  int choiceCount, int maximumVisibleChoices) {
        int visible = Math.min(maximumVisibleChoices, choiceCount);
        int height = visible * ROW_HEIGHT + 4;
        int minY = modal.y() + 3;
        int maxY = modal.y() + modal.height() - height - 3;
        int upward = selectorY - height - 2;
        int downward = selectorY + 20;
        int top = upward >= minY ? upward : Math.min(downward, maxY);
        top = clamp(top, minY, Math.max(minY, maxY));
        return new Box(selectorX, top, selectorWidth, height);
    }

    public static Box selectorDropdownBox(Box modal, int selectorX, int selectorY,
                                          int selectorWidth, int choiceCount) {
        int height = choiceCount * ROW_HEIGHT + 4;
        int minY = modal.y() + 3;
        int maxY = modal.y() + modal.height() - height - 3;
        int downward = selectorY + 20;
        int upward = selectorY - height - 2;
        int top = downward <= maxY ? downward : Math.max(minY, upward);
        return new Box(selectorX, top, selectorWidth, height);
    }

    public static int visualOptionsTop(Box box) {
        return box.y() + 34;
    }

    public static int visualOptionsBottom(Box box) {
        return box.y() + box.height() - 31;
    }

    public static int visualOptionsViewportHeight(Box box) {
        return Math.max(1, visualOptionsBottom(box) - visualOptionsTop(box));
    }

    public static int maximumScroll(int contentHeight, int viewportHeight) {
        return Math.max(0, contentHeight - Math.max(1, viewportHeight));
    }

    public static int scrollbarThumbHeight(int top, int bottom, int contentHeight,
                                           int viewportHeight) {
        int track = bottom - top - 4;
        return Math.max(12, track * viewportHeight / Math.max(viewportHeight, contentHeight));
    }

    public static int scrollbarThumbY(int top, int bottom, int scroll, int contentHeight,
                                      int viewportHeight) {
        int maximum = Math.max(1, contentHeight - viewportHeight);
        int thumb = scrollbarThumbHeight(top, bottom, contentHeight, viewportHeight);
        return top + 2 + (bottom - top - 4 - thumb) * scroll / maximum;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.min(Math.max(value, minimum), maximum);
    }

    public record Box(int x, int y, int width, int height) {}

    private PortalConfigLayout() {}
}
