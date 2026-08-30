package dev.riftgun.client.screen;

import dev.riftgun.RiftGun;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.portal.PortalOrientation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Editable center artwork for the precision-placement radial. */
final class PrecisionRadialSprites {
    // These sprites intentionally use a 64 x 64 authored canvas centered on the radial origin.
    private static final int SIZE = 64;
    private static final int HALF_SIZE = SIZE / 2;

    private static final Identifier FRONT_IN_FRONT = sprite("front_in_front");
    private static final Identifier FRONT_ABOVE_HEAD = sprite("front_above_head");
    private static final Identifier FRONT_BELOW_FEET = sprite("front_below_feet");
    private static final Identifier REMOTE_SIDEWAYS = sprite("remote_sideways");
    private static final Identifier REMOTE_TOP_DOWN = sprite("remote_top_down");
    private static final Identifier REMOTE_BOTTOM_UP = sprite("remote_bottom_up");

    private PrecisionRadialSprites() {}

    static void draw(GuiGraphicsExtractor graphics, int centerX, int centerY,
                     PortalPlacementMode mode, PortalOrientation orientation) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, select(mode, orientation),
            centerX - HALF_SIZE, centerY - HALF_SIZE, SIZE, SIZE);
    }

    private static Identifier select(PortalPlacementMode mode, PortalOrientation orientation) {
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

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(RiftGun.MOD_ID, "precision_radial/" + name);
    }
}
