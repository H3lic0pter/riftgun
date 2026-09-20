package dev.riftgun.client.screen;

import dev.riftgun.core.RiftConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Native GUI atlas adapter. Resource IDs and canvas sizes are cached by the shared catalog. */
record GuiSprite(Identifier id, int size) {
    GuiSprite(String path, int size) {
        this(Identifier.fromNamespaceAndPath(RiftConstants.MOD_ID, path), size);
    }

    /** Accepts the sprite canvas top-left. One native atlas draw, with no per-draw allocation. */
    static void draw(GuiGraphicsExtractor graphics, GuiSprite sprite, int x, int y) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.id, x, y, sprite.size, sprite.size);
    }

    /** Accepts button bounds; a 16 px sprite in a 26 px button has a 5 px inset. */
    static void drawCentered(GuiGraphicsExtractor graphics, GuiSprite sprite,
                             int buttonX, int buttonY, int buttonWidth, int buttonHeight) {
        draw(graphics, sprite, buttonX + (buttonWidth - sprite.size) / 2,
            buttonY + (buttonHeight - sprite.size) / 2);
    }

    /** Accepts the radial center, preserving the authored canvas size. */
    static void drawAtCenter(GuiGraphicsExtractor graphics, GuiSprite sprite, int centerX, int centerY) {
        draw(graphics, sprite, centerX - sprite.size / 2, centerY - sprite.size / 2);
    }
}
