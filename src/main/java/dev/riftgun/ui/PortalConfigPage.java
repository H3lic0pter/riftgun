package dev.riftgun.ui;

/** Semantic pages in the portal configuration workflow, independent of GUI toolkit versions. */
public enum PortalConfigPage {
    NONE("", "", false, false),
    CREATE_CURRENT("screen.riftgun.create_current", "", true, false),
    CREATE_COORDINATE("screen.riftgun.create_coordinate", "", true, true),
    EDIT_DESTINATION("screen.riftgun.edit_destination", "", true, true),
    CREATE_GROUP("screen.riftgun.create_group", "", true, false),
    RENAME_GROUP("screen.riftgun.rename_group", "", true, false),
    SHARE_DESTINATION("screen.riftgun.share", "", false, false),
    SETTINGS("screen.riftgun.settings", "", false, false),
    CONFIRM_SETTINGS("screen.riftgun.confirm_settings", "", false, false),
    MAP_INTEGRATION_SETTINGS("screen.riftgun.map_integration_settings", "", false, false),
    GUN_SETTINGS("screen.riftgun.configure_gun", "", false, false),
    PORTAL_DURATION_SETTINGS("screen.riftgun.portal_duration", "", false, false),
    SMART_DISTANCE_SETTINGS("screen.riftgun.smart_range", "", false, false),
    REMOTE_SETTINGS("screen.riftgun.remote.settings", "", false, false),
    ENTITY_TRANSIT_SETTINGS("screen.riftgun.entity_transit", "", false, false),
    APERTURE_SETTINGS("screen.riftgun.aperture", "", false, false),
    FALL_GUARD_SETTINGS("screen.riftgun.fall_guard", "", false, false),
    ENTITY_RELOCATION_SETTINGS("screen.riftgun.entity_relocation", "", false, false),
    PORTAL_PAIRING_SETTINGS("screen.riftgun.pairing.settings", "", false, false),
    PLAYER_TARGET_SETTINGS("screen.riftgun.player_target", "", false, false),
    VISUAL_SETTINGS("screen.riftgun.visual_settings", "", false, false),
    SWIRL_ANIMATION_SETTINGS("screen.riftgun.visual.swirl_animation_settings", "", false, false),
    SOUND_SETTINGS("screen.riftgun.sound_settings", "", false, false),
    CONFIRM_DELETE_DESTINATION(
        "screen.riftgun.delete", "screen.riftgun.delete_destination_body", false, false),
    CONFIRM_DELETE_GROUP(
        "screen.riftgun.delete", "screen.riftgun.delete_group_body", false, false),
    CONFIRM_DIRTY("screen.riftgun.unsaved", "screen.riftgun.unsaved_body", false, false),
    CONFIRM_CLEAR_FLUID(
        "screen.riftgun.clear_fluid", "screen.riftgun.clear_fluid_body", false, false);

    private final String titleKey;
    private final String bodyKey;
    private final boolean hasName;
    private final boolean hasCoordinates;

    PortalConfigPage(String titleKey, String bodyKey, boolean hasName, boolean hasCoordinates) {
        this.titleKey = titleKey;
        this.bodyKey = bodyKey;
        this.hasName = hasName;
        this.hasCoordinates = hasCoordinates;
    }

    public String titleKey() {
        return titleKey;
    }

    public String bodyKey() {
        return bodyKey;
    }

    public boolean isConfirmation() {
        return name().startsWith("CONFIRM_");
    }

    public boolean isGunSettingPage() {
        return this == PORTAL_DURATION_SETTINGS || this == SMART_DISTANCE_SETTINGS
            || this == REMOTE_SETTINGS || this == ENTITY_TRANSIT_SETTINGS
            || this == APERTURE_SETTINGS || this == PLAYER_TARGET_SETTINGS
            || this == FALL_GUARD_SETTINGS || this == ENTITY_RELOCATION_SETTINGS
            || this == PORTAL_PAIRING_SETTINGS;
    }

    public boolean hasInputs() {
        return hasName || hasCoordinates;
    }

    public boolean isDestinationForm() {
        return this == CREATE_CURRENT || this == CREATE_COORDINATE || this == EDIT_DESTINATION;
    }
}
