package dev.riftgun.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import dev.riftgun.ui.PortalColorPickerState;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
//?}

/** Hue ring and saturation/value square, inspired by the Photoshop HUD picker. */
final class PortalColorPickerScreen extends Screen {
    private static final int TEXTURE_SIZE = 240;
    private static final float INNER_RADIUS = 0.80F;
    //? if >=1.21.11 {
    /*private static final Identifier WHEEL = Identifier.fromNamespaceAndPath("riftgun", "dynamic/color_wheel");
    *///?} else {
    private static final ResourceLocation WHEEL = ResourceLocation.fromNamespaceAndPath("riftgun", "dynamic/color_wheel");
    //?}
    private final Screen parent;
    private final IntConsumer result;
    private final int original;
    private final PortalColorPickerState color;
    private EditBox hex;
    private ThemedButton accept;
    private boolean updatingHex;
    private boolean textureReady;
    private int drag; // 0: none, 1: hue ring, 2: saturation/value square.
    private int panelX, panelY, panelWidth, panelHeight, wheelX, wheelY, size, sideX;
    private int squareX, squareY, squareSize;

    /** Result is opaque RGB on acceptance, or -1 on cancellation. */
    PortalColorPickerScreen(Screen parent, int initial, IntConsumer result) {
        super(Component.translatable("screen.riftgun.color_picker"));
        this.parent = parent;
        this.original = initial & 0xFFFFFF;
        this.color = new PortalColorPickerState(original);
        this.result = result;
    }

    @Override protected void init() {
        panelWidth = Math.min(336, width - 12);
        panelHeight = Math.min(228, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        size = Math.min(160, Math.min(panelHeight - 64, panelWidth - 142));
        wheelX = panelX + 12;
        wheelY = panelY + 32;
        squareSize = (int) (size * 0.53F);
        squareX = wheelX + (size - squareSize) / 2;
        squareY = wheelY + (size - squareSize) / 2;
        sideX = wheelX + size + 16;
        int fieldWidth = panelX + panelWidth - 12 - sideX;
        hex = new EditBox(font, sideX, panelY + 113, fieldWidth, 18, Component.literal("HEX"));
        hex.setMaxLength(7);
        hex.setValue(color.hex());
        addRenderableWidget(hex);
        accept = addRenderableWidget(new ThemedButton(panelX + 12, panelY + panelHeight - 28,
            (panelWidth - 30) / 2, 19, Component.translatable("screen.riftgun.color_accept"), false,
            ignored -> finish(true)));
        addRenderableWidget(new ThemedButton(panelX + 18 + (panelWidth - 30) / 2,
            panelY + panelHeight - 28, (panelWidth - 30) / 2, 19,
            Component.translatable("screen.riftgun.cancel"), false, ignored -> finish(false)));
        hex.setResponder(text -> {
            if (updatingHex) return;
            int rgb = PortalColorPickerState.parseHex(text);
            accept.active = rgb >= 0;
            if (rgb >= 0) color.setRgb(rgb);
        });
        drag = 0;
        if (!textureReady) createWheelTexture();
    }

    private void createWheelTexture() {
        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
        float radius = TEXTURE_SIZE / 2F;
        for (int y = 0; y < TEXTURE_SIZE; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                double dx = x + 0.5 - radius, dy = y + 0.5 - radius;
                double distance = Math.sqrt(dx * dx + dy * dy);
                // A one-pixel alpha edge avoids a jagged ring at large GUI scales.
                double coverage = Math.clamp(Math.min(radius - distance,
                    distance - radius * INNER_RADIUS), 0, 1);
                int rgb = PortalColorPickerState.rgb((float) (Math.atan2(dy, dx) / (Math.PI * 2)), 1, 1);
                int alpha = (int) Math.round(coverage * 255);
                //? if >=1.21.11 {
                /*image.setPixel(x, y, alpha << 24 | rgb);
                *///?} else {
                image.setPixelRGBA(x, y, alpha << 24 | (rgb & 255) << 16 | (rgb & 0xFF00) | (rgb >> 16));
                //?}
            }
        }
        //? if >=1.21.11 {
        /*minecraft.getTextureManager().register(WHEEL, new DynamicTexture(() -> "RiftGun color wheel", image));
        *///?} else {
        minecraft.getTextureManager().register(WHEEL, new DynamicTexture(image));
        //?}
        textureReady = true;
    }

    //? if >=1.21.11 {
    /*@Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    *///?} else {
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    //?}
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        //? if >=1.21.11 {
        /*graphics.outline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.text(font, title, panelX + 12, panelY + 12, PortalTheme.TEXT, false);
        graphics.blit(WHEEL, wheelX, wheelY, wheelX + size, wheelY + size, 0, 1, 0, 1);
        *///?} else {
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.drawString(font, title, panelX + 12, panelY + 12, PortalTheme.TEXT, false);
        graphics.blit(WHEEL, wheelX, wheelY, size, size, 0F, 0F,
            TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        //?}
        for (int x = 0; x < squareSize; x++) {
            int top = 0xFF000000 | PortalColorPickerState.rgb(color.hue(), x / (float) (squareSize - 1), 1);
            graphics.fillGradient(squareX + x, squareY, squareX + x + 1, squareY + squareSize, top, 0xFF000000);
        }
        double angle = color.hue() * Math.PI * 2;
        float markerRadius = size * (1 + INNER_RADIUS) / 4;
        marker(graphics, wheelX + size / 2 + (int) Math.round(Math.cos(angle) * markerRadius),
            wheelY + size / 2 + (int) Math.round(Math.sin(angle) * markerRadius));
        marker(graphics, squareX + Math.round(color.saturation() * (squareSize - 1)),
            squareY + Math.round((1 - color.value()) * (squareSize - 1)));
        label(graphics, "screen.riftgun.color_new", sideX, panelY + 33);
        label(graphics, "screen.riftgun.color_old", sideX + 58, panelY + 33);
        graphics.fill(sideX, panelY + 46, sideX + 50, panelY + 82, 0xFF000000 | color.rgb());
        graphics.fill(sideX + 58, panelY + 46, sideX + 108, panelY + 82, 0xFF000000 | original);
        label(graphics, "screen.riftgun.color_hex", sideX, panelY + 98);
        //? if >=1.21.11 {
        /*for (var widget : renderables) widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        *///?} else {
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
        //?}
    }

    //? if >=1.21.11 {
    /*private void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
        graphics.text(font, Component.translatable(key), x, y, PortalTheme.TEXT_MUTED, false);
    }
    private static void marker(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.outline(x - 3, y - 3, 7, 7, 0xFF000000);
        graphics.outline(x - 2, y - 2, 5, 5, 0xFFFFFFFF);
    }
    *///?} else {
    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, PortalTheme.TEXT_MUTED, false);
    }
    private static void marker(GuiGraphics graphics, int x, int y) {
        graphics.renderOutline(x - 3, y - 3, 7, 7, 0xFF000000);
        graphics.renderOutline(x - 2, y - 2, 5, 5, 0xFFFFFFFF);
    }
    //?}

    private boolean beginDrag(double x, double y, int button) {
        if (button != 0) return false;
        double distance = Math.hypot(x - wheelX - size / 2.0, y - wheelY - size / 2.0);
        if (distance >= size * INNER_RADIUS / 2 && distance <= size / 2.0) drag = 1;
        else if (x >= squareX && x < squareX + squareSize && y >= squareY && y < squareY + squareSize) drag = 2;
        else return false;
        setFocused(null);
        updateDrag(x, y);
        return true;
    }

    private void updateDrag(double x, double y) {
        if (drag == 1) color.hue((float) (Math.atan2(y - wheelY - size / 2.0,
            x - wheelX - size / 2.0) / (Math.PI * 2)));
        else color.shade((float) (x - squareX) / (squareSize - 1),
            1 - (float) (y - squareY) / (squareSize - 1));
        updatingHex = true;
        hex.setValue(color.hex());
        updatingHex = false;
        accept.active = true;
    }

    //? if >=1.21.11 {
    /*@Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return beginDrag(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (drag != 0 && event.button() == 0) { updateDrag(event.x(), event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && drag != 0) { drag = 0; return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if ((event.key() == 257 || event.key() == 335) && (hex.isFocused() || getFocused() == null)) { finish(true); return true; }
        return super.keyPressed(event);
    }
    *///?} else {
    @Override public boolean mouseClicked(double x, double y, int button) {
        return beginDrag(x, y, button) || super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (drag != 0 && button == 0) { updateDrag(x, y); return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && drag != 0) { drag = 0; return true; }
        return super.mouseReleased(x, y, button);
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((key == 257 || key == 335) && (hex.isFocused() || getFocused() == null)) { finish(true); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    //?}

    private void finish(boolean apply) {
        if (apply && !accept.active) return;
        minecraft.setScreen(parent);
        result.accept(apply ? color.rgb() : -1);
    }
    @Override public void onClose() { finish(false); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void removed() {
        if (textureReady) minecraft.getTextureManager().release(WHEEL);
        textureReady = false;
    }
}
