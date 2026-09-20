package dev.riftgun.client.screen;

import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalOrientation;

/**
 * Shared catalog for all editable GUI sprites, including the precision-placement radial.
 *
 * <p>Paths are relative to assets/riftgun/textures/gui/sprites (without the PNG extension).
 * Each entry resolves its native resource ID once. State selectors return existing entries;
 * drawing stays in the version-specific GuiSprite adapter and uses Minecraft's GUI atlas.
 * Module item artwork continues to use the vanilla item model JSON loading path.
 */
final class PortalGuiSprites {
    static final GuiSprite BUCKET_ON = icon("bucket_on");
    static final GuiSprite BUCKET_OFF = icon("bucket_off");
    static final GuiSprite DRAIN_ON = icon("drain_on");
    static final GuiSprite DRAIN_OFF = icon("drain_off");
    static final GuiSprite PLACEMENT_SMART = icon("placement_smart");
    static final GuiSprite PLACEMENT_FRONT = icon("placement_front");
    static final GuiSprite PLACEMENT_REMOTE = icon("placement_remote");
    static final GuiSprite PLACEMENT_SURFACE = icon("placement_surface");
    static final GuiSprite PLACEMENT_ENTITY_RELOCATION = icon("placement_entity_relocation");
    static final GuiSprite PREDICTION_ON = icon("prediction_on");
    static final GuiSprite PREDICTION_OFF = icon("prediction_off");
    static final GuiSprite FUNCTION_COORDINATE = icon("function_coordinate");
    static final GuiSprite FUNCTION_PAIRING = icon("function_pairing");
    static final GuiSprite CONFIGURE_GUN = icon("configure_gun");
    static final GuiSprite MODULE_BAY = icon("module_bay");
    static final GuiSprite PORTAL_CLOSE = icon("portal_close");
    static final GuiSprite DIMENSIONAL_TRAVERSAL = icon("dimensional_traversal");
    static final GuiSprite RANDOM_RIFT_ON = icon("random_rift_on");
    static final GuiSprite RANDOM_RIFT_OFF = icon("random_rift_off");
    static final GuiSprite SMART_DISTANCE = icon("smart_distance");
    static final GuiSprite PORTAL_DURATION = icon("portal_duration");
    static final GuiSprite SURFACE_RANGE = icon("surface_range");
    static final GuiSprite ENTITY_ACCESS = icon("entity_access");
    static final GuiSprite APERTURE_ON = icon("aperture_on");
    static final GuiSprite APERTURE_OFF = icon("aperture_off");
    static final GuiSprite SURFACE_SIZE_FULL_SUPPORT = icon("surface_size_full_support");
    static final GuiSprite SURFACE_SIZE_ADAPTIVE = icon("surface_size_adaptive");
    static final GuiSprite SURFACE_SIZE_PREFER_LARGE = icon("surface_size_prefer_large");
    static final GuiSprite COLOR = icon("color");
    static final GuiSprite FALL_GUARD_ON = icon("fall_guard_on");
    static final GuiSprite FALL_GUARD_OFF = icon("fall_guard_off");
    static final GuiSprite ENTITY_FALL_GUARD_ON = icon("entity_fall_guard_on");
    static final GuiSprite ENTITY_FALL_GUARD_OFF = icon("entity_fall_guard_off");
    static final GuiSprite PASSIVE_TRANSIT_ON = icon("passive_transit_on");
    static final GuiSprite PASSIVE_TRANSIT_OFF = icon("passive_transit_off");
    static final GuiSprite HOSTILE_TRANSIT_ON = icon("hostile_transit_on");
    static final GuiSprite HOSTILE_TRANSIT_OFF = icon("hostile_transit_off");
    static final GuiSprite BOSS_TRANSIT_ON = icon("boss_transit_on");
    static final GuiSprite BOSS_TRANSIT_OFF = icon("boss_transit_off");
    static final GuiSprite PROJECTILE_TRANSIT_ON = icon("projectile_transit_on");
    static final GuiSprite PROJECTILE_TRANSIT_OFF = icon("projectile_transit_off");
    static final GuiSprite PLAYER_TARGET_ON = icon("player_target_on");
    static final GuiSprite PLAYER_TARGET_OFF = icon("player_target_off");
    static final GuiSprite PLAYER_EXCLUDE_ON = icon("player_exclude_on");
    static final GuiSprite PLAYER_EXCLUDE_OFF = icon("player_exclude_off");
    static final GuiSprite PLAYER_REFRESH = icon("player_refresh");
    static final GuiSprite ENTITY_RELOCATION_ON = icon("entity_relocation_on");
    static final GuiSprite ENTITY_RELOCATION_OFF = icon("entity_relocation_off");
    static final GuiSprite ENTITY_RELOCATION_SMART_ON = icon("entity_relocation_smart_on");
    static final GuiSprite ENTITY_RELOCATION_SMART_OFF = icon("entity_relocation_smart_off");
    static final GuiSprite VISUALS = icon("visuals");
    static final GuiSprite SOUNDS = icon("sounds");
    static final GuiSprite SHOT_ANIMATION = icon("shot_animation");
    static final GuiSprite RECOMMEND_APPLY = icon("recommend_apply");
    static final GuiSprite DROPDOWN = icon("dropdown");
    static final GuiSprite BACK = icon("back");
    static final GuiSprite MODULE_BACK = icon("module_back");
    static final GuiSprite RESET_ON = icon("reset_on");
    static final GuiSprite RESET_OFF = icon("reset_off");
    static final GuiSprite SWIRL = icon("swirl");
    static final GuiSprite GROUP_EXPANDED = icon("group_expanded");
    static final GuiSprite GROUP_COLLAPSED = icon("group_collapsed");
    static final GuiSprite DRAG_HANDLE = icon("drag_handle");
    static final GuiSprite DESTINATION_DOT_ON = icon("destination_dot_on");
    static final GuiSprite DESTINATION_DOT_OFF = icon("destination_dot_off");
    static final GuiSprite STAR_ON = icon("star_on");
    static final GuiSprite STAR_OFF = icon("star_off");
    static final GuiSprite DELETE = icon("delete");
    static final GuiSprite EDIT = icon("edit");

    private static final GuiSprite RECOMMEND_SOUNDS_ON = icon("recommend_sounds_on");
    private static final GuiSprite RECOMMEND_SOUNDS_OFF = icon("recommend_sounds_off");
    private static final GuiSprite RECOMMEND_PORTAL_VISUAL_ON = icon("recommend_portal_visual_on");
    private static final GuiSprite RECOMMEND_PORTAL_VISUAL_OFF = icon("recommend_portal_visual_off");
    private static final GuiSprite RECOMMEND_SHOT_ANIMATION_ON = icon("recommend_shot_animation_on");
    private static final GuiSprite RECOMMEND_SHOT_ANIMATION_OFF = icon("recommend_shot_animation_off");

    // Precision-placement center artwork uses a 64 x 64 canvas.
    private static final GuiSprite FRONT_IN_FRONT = precisionIcon("front_in_front");
    private static final GuiSprite FRONT_ABOVE_HEAD = precisionIcon("front_above_head");
    private static final GuiSprite FRONT_BELOW_FEET = precisionIcon("front_below_feet");
    private static final GuiSprite REMOTE_SIDEWAYS = precisionIcon("remote_sideways");
    private static final GuiSprite REMOTE_TOP_DOWN = precisionIcon("remote_top_down");
    private static final GuiSprite REMOTE_BOTTOM_UP = precisionIcon("remote_bottom_up");

    static GuiSprite recommendation(dev.riftgun.config.SkinRecommendationConfig.Category category, boolean enabled) {
        return switch (category) {
            case SOUNDS -> enabled ? RECOMMEND_SOUNDS_ON : RECOMMEND_SOUNDS_OFF;
            case PORTAL_VISUAL -> enabled ? RECOMMEND_PORTAL_VISUAL_ON : RECOMMEND_PORTAL_VISUAL_OFF;
            case SHOT_ANIMATION -> enabled ? RECOMMEND_SHOT_ANIMATION_ON : RECOMMEND_SHOT_ANIMATION_OFF;
        };
    }

    static GuiSprite precision(PortalPlacementMode mode, PortalOrientation orientation) {
        if (mode == PortalPlacementMode.REMOTE) {
            return switch (orientation) {
                case TOP -> REMOTE_TOP_DOWN;
                case BOTTOM -> REMOTE_BOTTOM_UP;
                default -> REMOTE_SIDEWAYS;
            };
        }
        return switch (orientation) {
            case TOP -> FRONT_BELOW_FEET;
            case BOTTOM -> FRONT_ABOVE_HEAD;
            default -> FRONT_IN_FRONT;
        };
    }

    private static GuiSprite icon(String name) {
        return new GuiSprite("icons/" + name, 16);
    }

    private static GuiSprite precisionIcon(String name) {
        return new GuiSprite("precision_radial/" + name, 64);
    }

    private PortalGuiSprites() {}
}
