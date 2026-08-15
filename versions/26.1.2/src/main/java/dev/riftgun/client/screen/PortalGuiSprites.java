package dev.riftgun.client.screen;

import dev.riftgun.RiftGun;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Central resource seam for editable GUI artwork.
 *
 * <p>Interaction, layout, and state remain in the screens. This class only maps those states to
 * standard Minecraft GUI sprites, so artists and resource packs can replace the PNGs independently.
 */
final class PortalGuiSprites {
    static final Identifier BUCKET_ON = icon("bucket_on");
    static final Identifier BUCKET_OFF = icon("bucket_off");
    static final Identifier DRAIN_ON = icon("drain_on");
    static final Identifier DRAIN_OFF = icon("drain_off");
    static final Identifier PLACEMENT_SMART = icon("placement_smart");
    static final Identifier PLACEMENT_FRONT = icon("placement_front");
    static final Identifier PLACEMENT_SURFACE = icon("placement_surface");
    static final Identifier PLACEMENT_ENTITY_RELOCATION = icon("placement_entity_relocation");
    static final Identifier PREDICTION_ON = icon("prediction_on");
    static final Identifier PREDICTION_OFF = icon("prediction_off");
    static final Identifier CONFIGURE_GUN = icon("configure_gun");
    static final Identifier MODULE_BAY = icon("module_bay");
    static final Identifier PORTAL_CLOSE = icon("portal_close");
    static final Identifier SMART_DISTANCE = icon("smart_distance");
    static final Identifier PORTAL_DURATION = icon("portal_duration");
    static final Identifier SURFACE_RANGE = icon("surface_range");
    static final Identifier ENTITY_ACCESS = icon("entity_access");
    static final Identifier APERTURE_ON = icon("aperture_on");
    static final Identifier APERTURE_OFF = icon("aperture_off");
    static final Identifier FALL_GUARD_ON = icon("fall_guard_on");
    static final Identifier FALL_GUARD_OFF = icon("fall_guard_off");
    static final Identifier ENTITY_FALL_GUARD_ON = icon("entity_fall_guard_on");
    static final Identifier ENTITY_FALL_GUARD_OFF = icon("entity_fall_guard_off");
    static final Identifier PASSIVE_TRANSIT_ON = icon("passive_transit_on");
    static final Identifier PASSIVE_TRANSIT_OFF = icon("passive_transit_off");
    static final Identifier HOSTILE_TRANSIT_ON = icon("hostile_transit_on");
    static final Identifier HOSTILE_TRANSIT_OFF = icon("hostile_transit_off");
    static final Identifier BOSS_TRANSIT_ON = icon("boss_transit_on");
    static final Identifier BOSS_TRANSIT_OFF = icon("boss_transit_off");
    static final Identifier PROJECTILE_TRANSIT_ON = icon("projectile_transit_on");
    static final Identifier PROJECTILE_TRANSIT_OFF = icon("projectile_transit_off");
    static final Identifier PLAYER_TARGET_ON = icon("player_target_on");
    static final Identifier PLAYER_TARGET_OFF = icon("player_target_off");
    static final Identifier PLAYER_EXCLUDE_ON = icon("player_exclude_on");
    static final Identifier PLAYER_EXCLUDE_OFF = icon("player_exclude_off");
    static final Identifier PLAYER_REFRESH = icon("player_refresh");
    static final Identifier ENTITY_RELOCATION_ON = icon("entity_relocation_on");
    static final Identifier ENTITY_RELOCATION_OFF = icon("entity_relocation_off");
    static final Identifier ENTITY_RELOCATION_SMART_ON = icon("entity_relocation_smart_on");
    static final Identifier ENTITY_RELOCATION_SMART_OFF = icon("entity_relocation_smart_off");
    static final Identifier VISUALS = icon("visuals");
    static final Identifier SOUNDS = icon("sounds");
    static final Identifier DROPDOWN = icon("dropdown");
    static final Identifier BACK = icon("back");
    static final Identifier MODULE_BACK = icon("module_back");
    static final Identifier RESET_ON = icon("reset_on");
    static final Identifier RESET_OFF = icon("reset_off");
    static final Identifier SWIRL = icon("swirl");
    static final Identifier GROUP_EXPANDED = icon("group_expanded");
    static final Identifier GROUP_COLLAPSED = icon("group_collapsed");
    static final Identifier DRAG_HANDLE = icon("drag_handle");
    static final Identifier DESTINATION_DOT_ON = icon("destination_dot_on");
    static final Identifier DESTINATION_DOT_OFF = icon("destination_dot_off");
    static final Identifier STAR_ON = icon("star_on");
    static final Identifier STAR_OFF = icon("star_off");
    static final Identifier DELETE = icon("delete");
    static final Identifier EDIT = icon("edit");

    private static final int SIZE = 16;

    private PortalGuiSprites() {}

    static void draw(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, SIZE, SIZE);
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "icons/" + name);
    }
}
