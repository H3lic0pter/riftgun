package dev.riftgun.client.screen;

import dev.riftgun.portal.SurfacePortalSize;
//? if >=1.21.11 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

/** Editable 16 x 16 PNG icon. Coordinates are the top-left of a 26 x 26 button. */
final class SurfacePortalSizeIcon {
    private static final int BUTTON_SIZE = 26;

    //? if >=1.21.11 {
    /*static void draw(GuiGraphicsExtractor graphics, int buttonX, int buttonY, SurfacePortalSize mode) {
    *///?} else {
    static void draw(GuiGraphics graphics, int buttonX, int buttonY, SurfacePortalSize mode) {
    //?}
        var sprite = switch (mode) {
            case FULL_SUPPORT -> PortalGuiSprites.SURFACE_SIZE_FULL_SUPPORT;
            case ADAPTIVE -> PortalGuiSprites.SURFACE_SIZE_ADAPTIVE;
            case PREFER_LARGE -> PortalGuiSprites.SURFACE_SIZE_PREFER_LARGE;
        };
        PortalGuiSprites.drawCentered(graphics, sprite, buttonX, buttonY, BUTTON_SIZE, BUTTON_SIZE);
    }

    private SurfacePortalSizeIcon() {}
}
