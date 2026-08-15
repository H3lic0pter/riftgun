package dev.riftgun.client.screen;

import dev.riftgun.module.PortalModuleMenu;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public final class PortalModuleScreen extends AbstractContainerScreen<PortalModuleMenu> {
    private @Nullable ThemedButton backButton;
    private boolean returning;

    public PortalModuleScreen(PortalModuleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 190);
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 97;
    }

    @Override
    protected void init() {
        super.init();
        backButton = addRenderableWidget(new ThemedButton(leftPos + 150, topPos + 5, 18, 15,
            Component.empty(), false, ignored -> returnToParent()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderInactiveSlots(graphics);
        renderBackIcon(graphics, mouseX, mouseY);
            }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PortalTheme.PANEL);
        outline(graphics, leftPos, topPos, imageWidth, imageHeight, PortalTheme.BORDER_FOCUS);
        graphics.fill(leftPos + 5, topPos + 31, leftPos + imageWidth - 5, topPos + 92, PortalTheme.FIELD);
        outline(graphics, leftPos + 5, topPos + 31, imageWidth - 10, 61, PortalTheme.BORDER);
        for (int slot = 0; slot < menu.unlockedSlots(); slot++) {
            int column = slot % PortalModuleMenu.MODULE_COLUMNS;
            int row = slot / PortalModuleMenu.MODULE_COLUMNS;
            slotFrame(graphics, leftPos + PortalModuleMenu.MODULE_START_X - 1 + column * 18,
                topPos + PortalModuleMenu.MODULE_START_Y - 1 + row * 18);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                slotFrame(graphics, leftPos + 7 + column * 18,
                    topPos + PortalModuleMenu.PLAYER_INVENTORY_Y - 1 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            slotFrame(graphics, leftPos + 7 + column * 18,
                topPos + PortalModuleMenu.HOTBAR_Y - 1);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        graphics.text(font, title, titleLabelX, titleLabelY, PortalTheme.TEXT, false);
        graphics.text(font, Component.translatable("screen.riftgun.modules.slot_count",
            menu.usedSlots(), menu.unlockedSlots()), 8, 19, PortalTheme.TEXT_MUTED, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
            PortalTheme.TEXT_MUTED, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 || minecraft != null && minecraft.options.keyInventory.matches(event)) {
            returnToParent();
            return true;
        }
        return super.keyPressed(event);
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

    private void renderInactiveSlots(GuiGraphicsExtractor graphics) {
        int mask = menu.inactiveSlots();
        for (int slot = 0; slot < menu.unlockedSlots(); slot++) {
            if ((mask & 1 << slot) == 0) continue;
            int column = slot % PortalModuleMenu.MODULE_COLUMNS;
            int row = slot / PortalModuleMenu.MODULE_COLUMNS;
            int x = leftPos + PortalModuleMenu.MODULE_START_X + column * 18;
            int y = topPos + PortalModuleMenu.MODULE_START_Y + row * 18;
            graphics.fill(x, y, x + 16, y + 16, 0x78E18479);
            graphics.fill(x + 3, y + 3, x + 13, y + 5, PortalTheme.DANGER);
            graphics.fill(x + 7, y + 5, x + 9, y + 13, PortalTheme.DANGER);
        }
    }

    private void renderBackIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (backButton == null) return;
        PortalGuiSprites.draw(graphics, PortalGuiSprites.MODULE_BACK,
            backButton.getX() + 1, backButton.getY());
        if (backButton.isHovered()) graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.modules.back")), mouseX, mouseY);
    }

    private static void slotFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, PortalTheme.BORDER);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, PortalTheme.FIELD);
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

}
