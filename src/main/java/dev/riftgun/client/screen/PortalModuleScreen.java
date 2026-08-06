package dev.riftgun.client.screen;

import dev.riftgun.module.PortalModuleMenu;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public final class PortalModuleScreen extends AbstractContainerScreen<PortalModuleMenu> {
    private static final int STATUS_Y = 17;
    private static final int STATUS_SIZE = 13;
    private @Nullable ThemedButton backButton;
    private boolean returning;

    public PortalModuleScreen(PortalModuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        backButton = addRenderableWidget(new ThemedButton(leftPos + 150, topPos + 5, 18, 15,
            Component.empty(), false, ignored -> returnToParent()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderInactiveSlots(graphics);
        renderBackIcon(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        renderStatusTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PortalTheme.PANEL);
        outline(graphics, leftPos, topPos, imageWidth, imageHeight, PortalTheme.BORDER_FOCUS);
        graphics.fill(leftPos + 5, topPos + 31, leftPos + imageWidth - 5, topPos + 55, PortalTheme.FIELD);
        outline(graphics, leftPos + 5, topPos + 31, imageWidth - 10, 24, PortalTheme.BORDER);
        for (int slot = 0; slot < PortalModuleMenu.MODULE_SLOT_COUNT; slot++) {
            slotFrame(graphics, leftPos + 7 + slot * 18, topPos + 34);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotFrame(graphics, leftPos + 7 + column * 18, topPos + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotFrame(graphics, leftPos + 7 + column * 18, topPos + 141);
        }
        renderStatusBar(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, PortalTheme.TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
            PortalTheme.TEXT_MUTED, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            returnToParent();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private void returnToParent() {
        if (returning || minecraft == null || minecraft.player == null) return;
        returning = true;
        minecraft.player.closeContainer();
        PortalNetworking.sendRequest(PortalAction.OPEN_GUI,
            tag -> tag.put("GunReference", menu.gunReference()));
    }

    private void renderStatusBar(GuiGraphics graphics) {
        int x = leftPos + 8;
        int y = topPos + STATUS_Y;
        for (int index = 0; index < 4; index++) {
            int bx = x + index * 17;
            graphics.fill(bx, y, bx + STATUS_SIZE, y + STATUS_SIZE, PortalTheme.PANEL_RAISED);
            outline(graphics, bx, y, STATUS_SIZE, STATUS_SIZE, PortalTheme.BORDER);
        }
        drawTankIcon(graphics, x + 3, y + 3, PortalTheme.ICE);
        drawRangeIcon(graphics, x + 20, y + 3, PortalTheme.WARNING);
        drawEntityIcon(graphics, x + 37, y + 3, PortalTheme.PORTAL);
        drawCoordinateIcon(graphics, x + 54, y + 3,
            menu.coordinateUnlocked() ? PortalTheme.ICE : PortalTheme.TEXT_MUTED);
        graphics.drawString(font, menu.usedSlots() + "/" + PortalModuleMenu.MODULE_SLOT_COUNT,
            leftPos + 82, topPos + 19, PortalTheme.TEXT_MUTED, false);
    }

    private void renderInactiveSlots(GuiGraphics graphics) {
        int mask = menu.inactiveSlots();
        for (int slot = 0; slot < PortalModuleMenu.MODULE_SLOT_COUNT; slot++) {
            if ((mask & 1 << slot) == 0) continue;
            int x = leftPos + 8 + slot * 18;
            int y = topPos + 35;
            graphics.fill(x, y, x + 16, y + 16, 0x78E18479);
            graphics.fill(x + 3, y + 3, x + 13, y + 5, PortalTheme.DANGER);
            graphics.fill(x + 7, y + 5, x + 9, y + 13, PortalTheme.DANGER);
        }
    }

    private void renderBackIcon(GuiGraphics graphics, int mouseX, int mouseY) {
        if (backButton == null) return;
        PortalGuiSprites.draw(graphics, PortalGuiSprites.MODULE_BACK,
            backButton.getX() + 1, backButton.getY());
        if (backButton.isHovered()) graphics.renderTooltip(font,
            Component.translatable("screen.riftgun.modules.back"), mouseX, mouseY);
    }

    private void renderStatusTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int firstX = leftPos + 8;
        int y = topPos + STATUS_Y;
        if (mouseY < y || mouseY >= y + STATUS_SIZE) return;
        int index = (mouseX - firstX) / 17;
        if (index < 0 || index > 3 || mouseX >= firstX + index * 17 + STATUS_SIZE) return;
        List<Component> tooltip = switch (index) {
            case 0 -> List.of(Component.translatable("screen.riftgun.modules.capacity",
                menu.capacity()));
            case 1 -> List.of(Component.translatable("screen.riftgun.modules.surface_range",
                menu.configuredRange(), menu.maximumRange()));
            case 2 -> List.of(Component.translatable("screen.riftgun.modules.entity_access",
                Integer.bitCount(menu.entityMask())));
            default -> List.of(Component.translatable(menu.coordinateUnlocked()
                ? "screen.riftgun.modules.coordinate_unlocked"
                : "screen.riftgun.modules.coordinate_locked"));
        };
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private static void slotFrame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, PortalTheme.BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, PortalTheme.FIELD);
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static void drawTankIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.STATUS_CAPACITY, x - 4, y - 4);
    }

    private static void drawRangeIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.STATUS_RANGE, x - 4, y - 4);
    }

    private static void drawEntityIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.STATUS_ENTITY, x - 4, y - 4);
    }

    private static void drawCoordinateIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.STATUS_COORDINATE_OFF : PortalGuiSprites.STATUS_COORDINATE_ON,
            x - 4, y - 4);
    }
}
