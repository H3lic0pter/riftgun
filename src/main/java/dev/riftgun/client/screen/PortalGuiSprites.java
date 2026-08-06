package dev.riftgun.client.screen;

import dev.riftgun.RiftGun;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Central resource seam for editable GUI artwork.
 *
 * <p>Interaction, layout, and state remain in the screens. This class only maps those states to
 * standard Minecraft GUI sprites, so artists and resource packs can replace the PNGs independently.
 */
final class PortalGuiSprites {
    static final ResourceLocation BUCKET_ON = icon("bucket_on");
    static final ResourceLocation BUCKET_OFF = icon("bucket_off");
    static final ResourceLocation DRAIN_ON = icon("drain_on");
    static final ResourceLocation DRAIN_OFF = icon("drain_off");
    static final ResourceLocation PLACEMENT_SMART = icon("placement_smart");
    static final ResourceLocation PLACEMENT_FRONT = icon("placement_front");
    static final ResourceLocation PLACEMENT_SURFACE = icon("placement_surface");
    static final ResourceLocation PREDICTION_ON = icon("prediction_on");
    static final ResourceLocation PREDICTION_OFF = icon("prediction_off");
    static final ResourceLocation CONFIGURE_GUN = icon("configure_gun");
    static final ResourceLocation MODULE_BAY = icon("module_bay");
    static final ResourceLocation SMART_DISTANCE = icon("smart_distance");
    static final ResourceLocation SURFACE_RANGE = icon("surface_range");
    static final ResourceLocation ENTITY_ACCESS = icon("entity_access");
    static final ResourceLocation PASSIVE_TRANSIT_ON = icon("passive_transit_on");
    static final ResourceLocation PASSIVE_TRANSIT_OFF = icon("passive_transit_off");
    static final ResourceLocation HOSTILE_TRANSIT_ON = icon("hostile_transit_on");
    static final ResourceLocation HOSTILE_TRANSIT_OFF = icon("hostile_transit_off");
    static final ResourceLocation BOSS_TRANSIT_ON = icon("boss_transit_on");
    static final ResourceLocation BOSS_TRANSIT_OFF = icon("boss_transit_off");
    static final ResourceLocation VISUALS = icon("visuals");
    static final ResourceLocation DROPDOWN = icon("dropdown");
    static final ResourceLocation BACK = icon("back");
    static final ResourceLocation MODULE_BACK = icon("module_back");
    static final ResourceLocation RESET_ON = icon("reset_on");
    static final ResourceLocation RESET_OFF = icon("reset_off");
    static final ResourceLocation SWIRL = icon("swirl");
    static final ResourceLocation GROUP_EXPANDED = icon("group_expanded");
    static final ResourceLocation GROUP_COLLAPSED = icon("group_collapsed");
    static final ResourceLocation DRAG_HANDLE = icon("drag_handle");
    static final ResourceLocation DESTINATION_DOT_ON = icon("destination_dot_on");
    static final ResourceLocation DESTINATION_DOT_OFF = icon("destination_dot_off");
    static final ResourceLocation STAR_ON = icon("star_on");
    static final ResourceLocation STAR_OFF = icon("star_off");
    static final ResourceLocation DELETE = icon("delete");
    static final ResourceLocation EDIT = icon("edit");
    static final ResourceLocation STATUS_CAPACITY = icon("status_capacity");
    static final ResourceLocation STATUS_RANGE = icon("status_range");
    static final ResourceLocation STATUS_ENTITY = icon("status_entity");
    static final ResourceLocation STATUS_COORDINATE_ON = icon("status_coordinate_on");
    static final ResourceLocation STATUS_COORDINATE_OFF = icon("status_coordinate_off");

    private static final int SIZE = 16;

    private PortalGuiSprites() {}

    static void draw(GuiGraphics graphics, ResourceLocation sprite, int x, int y) {
        graphics.blitSprite(sprite, x, y, SIZE, SIZE);
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "icons/" + name);
    }
}
