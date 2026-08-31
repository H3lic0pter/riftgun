package dev.riftgun.client.screen;

import dev.riftgun.client.DimensionLabelState;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Searchable full-ID dimension picker used only by Dimensional Navigation. */
public final class DimensionSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 230;
    private static final int ROW_HEIGHT = 20;
    private final DimensionalNavigationScreen parent;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;
    private int scroll;
    private String query = "";
    private String filteredNeedle = "";
    private List<DimensionLabelState.DimensionInfo> filteredDimensions = List.of();
    private List<DimensionLabelState.DimensionInfo> filteredSource = List.of();
    private EditBox search;
    private ThemedButton backButton;

    DimensionSelectionScreen(DimensionalNavigationScreen parent) {
        super(Component.translatable("screen.riftgun.dimensional_navigation.choose_dimension"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(PANEL_WIDTH, width - 16);
        panelHeight = Math.min(PANEL_HEIGHT, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listTop = panelY + 70;
        listBottom = panelY + panelHeight - 16;
        backButton = addRenderableWidget(new ThemedButton(
            panelX + panelWidth - 27, panelY + 7, 19, 18,
            Component.empty(), false, ignored -> onClose()));
        backButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("screen.riftgun.back")));
        search = new EditBox(font, panelX + 18, panelY + 39, panelWidth - 36, 20,
            Component.translatable("screen.riftgun.dimensional_navigation.search"));
        search.setValue(query);
        search.setHint(Component.translatable("screen.riftgun.dimensional_navigation.search"));
        search.setMaxLength(128);
        search.setResponder(value -> {
            query = value;
            scroll = 0;
            rebuildFilter();
        });
        addRenderableWidget(search);
        rebuildFilter();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.text(font, title, panelX + 12, panelY + 12, PortalTheme.TEXT, false);
        List<DimensionLabelState.DimensionInfo> dimensions = filtered();
        clampScroll(dimensions.size());
        graphics.enableScissor(panelX + 12, listTop, panelX + panelWidth - 12, listBottom);
        for (int index = 0; index < dimensions.size(); index++) {
            int rowY = listTop + index * ROW_HEIGHT - scroll;
            if (rowY + ROW_HEIGHT <= listTop || rowY >= listBottom) continue;
            String id = dimensions.get(index).id();
            boolean selected = id.equals(parent.selectedDimension());
            boolean hovered = inside(mouseX, mouseY, panelX + 18, rowY, panelWidth - 36, ROW_HEIGHT - 2);
            graphics.fill(panelX + 18, rowY, panelX + panelWidth - 18, rowY + ROW_HEIGHT - 2,
                selected ? 0xFF31506B : hovered ? PortalTheme.PANEL_HOVER : PortalTheme.PANEL_RAISED);
            graphics.text(font, id, panelX + 25, rowY + 6,
                selected ? PortalTheme.TEXT : PortalTheme.TEXT_MUTED, false);
        }
        graphics.disableScissor();
        for (Renderable renderable : renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (backButton != null) PortalGuiIcons.drawCompactBackButtonIcon(
            graphics, backButton.getX(), backButton.getY());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && inside(event.x(), event.y(), panelX + 18, listTop,
            panelWidth - 36, listBottom - listTop)) {
            int index = (int) (event.y() - listTop + scroll) / ROW_HEIGHT;
            List<DimensionLabelState.DimensionInfo> dimensions = filtered();
            if (index >= 0 && index < dimensions.size()) {
                parent.selectDimension(dimensions.get(index).id());
                onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (inside(mouseX, mouseY, panelX + 12, listTop, panelWidth - 24, listBottom - listTop)) {
            scroll -= (int) Math.round(verticalAmount) * ROW_HEIGHT;
            clampScroll(filtered().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private List<DimensionLabelState.DimensionInfo> filtered() {
        if (filteredSource != DimensionLabelState.dimensions()) rebuildFilter();
        return filteredDimensions;
    }

    private void rebuildFilter() {
        filteredNeedle = query.strip().toLowerCase(Locale.ROOT);
        filteredSource = DimensionLabelState.dimensions();
        filteredDimensions = filteredNeedle.isEmpty() ? filteredSource : filteredSource.stream()
            .filter(info -> info.id().toLowerCase(Locale.ROOT).contains(filteredNeedle))
            .toList();
    }

    private void clampScroll(int rows) {
        scroll = Math.clamp(scroll, 0, Math.max(0, rows * ROW_HEIGHT - (listBottom - listTop)));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
