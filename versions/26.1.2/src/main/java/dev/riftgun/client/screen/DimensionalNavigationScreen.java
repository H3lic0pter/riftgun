package dev.riftgun.client.screen;

import dev.riftgun.client.DimensionLabelState;
import dev.riftgun.client.DimensionalNavigationController;
import dev.riftgun.client.DimensionalNavigationWorkflow;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Fixed-size, per-gun cross-dimensional navigation editor. */
public final class DimensionalNavigationScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 230;
    private static final int FIELD_HEIGHT = 18;
    private final PortalConfigScreen parent;
    private final DimensionalNavigationController controller;


    private String name = "";
    private String x = "";
    private String y = "";
    private String z = "";
    private String yaw = "";







    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private EditBox nameField;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private EditBox yawField;
    private ThemedButton backButton;
    private ThemedButton dimensionSelector;
    private ThemedButton dimensionDropdownButton;
    private Map<String, String> dropdownLabels = Map.of();

    public DimensionalNavigationScreen(PortalConfigScreen parent, UUID group) {
        super(Component.translatable("screen.riftgun.dimensional_navigation"));
        this.parent = parent;
        controller = new DimensionalNavigationController(PortalClientState.gun(), group);


    }

    @Override
    protected void init() {
        controller.ensureKnownDimension(DimensionLabelState.dimensions(), currentDimension());
        if (!controller.coordinateDefaultsInitialized()) {
            controller.coordinateDefaultsInitialized(resetCoordinateDefaults());
        }
        rebuildDropdownLabels();
        panelWidth = Math.min(PANEL_WIDTH, width - 16);
        panelHeight = Math.min(PANEL_HEIGHT, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int left = panelX + 18;
        int contentWidth = panelWidth - 36;
        backButton = button(panelX + panelWidth - 27, panelY + 7, 19, 18,
            Component.empty(), false, ignored -> onClose());
        backButton.setTooltip(Tooltip.create(Component.translatable("screen.riftgun.back")));
        dimensionSelector = button(left, panelY + 39, contentWidth - 22, 18,
            Component.literal(displayDimension(controller.dimension())), false, ignored -> {});
        dimensionSelector.horizontalMarquee();
        dimensionSelector.setTooltip(Tooltip.create(Component.literal(controller.dimension())));
        dimensionDropdownButton = button(left + contentWidth - 20, panelY + 39, 20, 18,
            Component.empty(), false, ignored -> openDimensionDropdown());
        button(left, panelY + 64, contentWidth, 19,
            Component.translatable("screen.riftgun.dimensional_navigation.choose_dimension"), false,
            ignored -> minecraft.setScreen(new DimensionSelectionScreen(this)));
        int segmentWidth = (contentWidth - 3) / 2;
        ThemedButton exact = button(left, panelY + 91, segmentWidth, 20,
            Component.translatable("screen.riftgun.dimensional_navigation.exact"), false,
            ignored -> selectMode(DimensionalTraversalMode.EXACT_COORDINATES));
        ThemedButton automatic = button(left + segmentWidth + 3, panelY + 91,
            contentWidth - segmentWidth - 3, 20,
            Component.translatable("screen.riftgun.dimensional_navigation.automatic"), false,
            ignored -> selectMode(DimensionalTraversalMode.AUTOMATIC_SEARCH));
        if (!controller.dropdownOpen() && controller.mode() == DimensionalTraversalMode.EXACT_COORDINATES) {
            exact.accented(0xFF31506B, 0xFF3F698C, PortalTheme.TEXT);
        } else {
            automatic.accented(0xFF31506B, 0xFF3F698C, PortalTheme.TEXT);
        }
        automatic.active = PortalClientState.randomRift().getBoolean("Enabled").orElse(false);
        if (!automatic.active) automatic.setTooltip(Tooltip.create(
            Component.translatable("screen.riftgun.dimensional_navigation.random_disabled")));
        if (controller.mode() == DimensionalTraversalMode.EXACT_COORDINATES) initExactFields(left, contentWidth);
        ThemedButton action = button(left, panelY + panelHeight - 31, contentWidth, 20,
            Component.translatable(controller.mode() == DimensionalTraversalMode.EXACT_COORDINATES
                ? "screen.riftgun.dimensional_navigation.save"
                : "screen.riftgun.dimensional_navigation.open"), true,
            ignored -> performAction());
        action.active = !controller.saving() && (controller.mode() != DimensionalTraversalMode.AUTOMATIC_SEARCH
            || PortalClientState.randomRift().getBoolean("Enabled").orElse(false));
    }

    private void initExactFields(int left, int contentWidth) {
        nameField = field(left + 54, panelY + 126, contentWidth - 54, name, 48, value -> name = value, false);
        int gap = 4;
        int coordinateWidth = (contentWidth - gap * 3) / 4;
        xField = field(left, panelY + 165, coordinateWidth, x, 64, value -> x = value, true);
        yField = field(left + coordinateWidth + gap, panelY + 165, coordinateWidth, y, 64,
            value -> y = value, true);
        zField = field(left + (coordinateWidth + gap) * 2, panelY + 165, coordinateWidth, z, 64,
            value -> z = value, true);
        yawField = field(left + (coordinateWidth + gap) * 3, panelY + 165,
            contentWidth - (coordinateWidth + gap) * 3, yaw, 64, value -> yaw = value, true);
    }

    private EditBox field(int fieldX, int fieldY, int fieldWidth, String value, int maxLength,
                          java.util.function.Consumer<String> responder, boolean coordinate) {
        EditBox field = new EditBox(font, fieldX, fieldY, fieldWidth, FIELD_HEIGHT, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            if (coordinate) controller.coordinatesEdited(true);
        });
        return addRenderableWidget(field);
    }

    void selectDimension(String id) {
        if (!controller.selectDimension(DimensionLabelState.dimensions(), id)) return;

        controller.closeDropdown();
        if (!controller.coordinatesEdited()) controller.coordinateDefaultsInitialized(resetCoordinateDefaults());
        PortalClientState.updateGun(state -> state.withNavigation(
            state.navigation().withTargetDimension(id)));
        sendSetting("DimensionalTraversalDimension", id);
        if (dimensionSelector != null) {
            dimensionSelector.setMessage(Component.literal(displayDimension(id)));
            dimensionSelector.setTooltip(Tooltip.create(Component.literal(id)));
        }
    }

    private void selectMode(DimensionalTraversalMode selected) {
        if (!controller.selectMode(selected,
            PortalClientState.randomRift().getBoolean("Enabled").orElse(false))) return;
        PortalClientState.updateGun(state -> state.withNavigation(
            state.navigation().withMode(selected)));
        sendSetting("DimensionalTraversalMode", selected.name());
        clearWidgets();
        init();
    }

    private void performAction() {
        DimensionalNavigationWorkflow.Command command = DimensionalNavigationWorkflow.begin(
            controller, exactFields(), PortalClientState.data().selectedDestinationId());
        if (command == null) return;
        PortalNetworking.sendRequest(command.action(), command::writeTo);
        if (command.closesScreen()) {
            minecraft.setScreen(null);
            return;
        }
        clearWidgets();
        init();
    }

    public void onServerSnapshot() {
        UUID selected = PortalClientState.data().selectedDestinationId();
        DimensionalNavigationController.SaveOutcome outcome = controller.acceptSnapshot(selected);
        if (outcome == DimensionalNavigationController.SaveOutcome.SAVED) {
            parent.refreshFromServer(Set.of());
            minecraft.setScreen(parent);
            return;
        }
        refreshGunState();
    }

    public void onGunSnapshot() {
        refreshGunState();
    }

    private void refreshGunState() {
        controller.refresh(PortalClientState.gun());
        controller.ensureKnownDimension(DimensionLabelState.dimensions(), currentDimension());
        clearWidgets();
        init();
    }

    private boolean resetCoordinateDefaults() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return false;
        DimensionalNavigationWorkflow.Coordinates defaults =
            DimensionalNavigationWorkflow.coordinateDefaults(
                DimensionLabelState.dimensions(), controller.dimension(),
                minecraft.level.dimensionType().coordinateScale(), minecraft.player.getX(),
                minecraft.player.getY(), minecraft.player.getZ(), minecraft.player.getYRot());
        x = defaults.x();
        y = defaults.y();
        z = defaults.z();
        yaw = defaults.yaw();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.text(font, title, panelX + 12, panelY + 12, PortalTheme.TEXT, false);
        graphics.text(font, Component.translatable("screen.riftgun.dimensional_navigation.dimension"),
            panelX + 18, panelY + 29, PortalTheme.TEXT_MUTED, false);
        if (controller.mode() == DimensionalTraversalMode.EXACT_COORDINATES) {
            graphics.text(font, Component.translatable("screen.riftgun.name"),
                panelX + 18, panelY + 131, PortalTheme.TEXT_MUTED, false);
            int left = panelX + 18;
            int contentWidth = panelWidth - 36;
            int gap = 4;
            int coordinateWidth = (contentWidth - gap * 3) / 4;
            String[] keys = {"screen.riftgun.x", "screen.riftgun.y", "screen.riftgun.z", "screen.riftgun.yaw"};
            for (int index = 0; index < 4; index++) {
                graphics.text(font, Component.translatable(keys[index]),
                    left + index * (coordinateWidth + gap), panelY + 153, PortalTheme.TEXT_MUTED, false);
            }
        }
        for (Renderable renderable : renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (backButton != null) PortalGuiIcons.drawCompactBackButtonIcon(
            graphics, backButton.getX(), backButton.getY());
        if (dimensionDropdownButton != null) PortalGuiIcons.drawDownIcon(graphics,
            dimensionDropdownButton.getX() + 6, dimensionDropdownButton.getY() + 7);
        if (controller.dropdownOpen()) renderDimensionDropdown(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (controller.dropdownOpen()) {
            if (event.button() == 0) clickDimensionDropdown(event.x(), event.y());
            controller.closeDropdown();
            return true;
        }
        if (dimensionSelector != null && (event.button() == 0 || event.button() == 1)
            && inside(event.x(), event.y(), dimensionSelector.getX(), dimensionSelector.getY(),
                dimensionSelector.getWidth(), dimensionSelector.getHeight())) {
            String before = controller.dimension();
            shiftDimension(event.button() == 0 ? 1 : -1);
            if (!before.equals(controller.dimension()) && minecraft != null) {
                dimensionSelector.playDownSound(minecraft.getSoundManager());
            }
            setFocused(dimensionSelector);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (controller.dropdownOpen()) {
            controller.scrollDropdown(-(int) Math.signum(verticalAmount),
                DimensionLabelState.dimensions().size());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (controller.dropdownOpen()) return dropdownKeyPressed(event.key());
        return super.keyPressed(event);
    }

    private void openDimensionDropdown() {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        controller.openDropdown(dimensions);
        setFocused(dimensionSelector);
    }

    private void shiftDimension(int delta) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        selectDimension(controller.shiftedDimension(dimensions, delta));
    }

    private void renderDimensionDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        DropdownBox box = dropdownBox(dimensions.size());
        graphics.nextStratum();
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.outline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        int visible = Math.min(7, dimensions.size());
        controller.dropdownScroll(Mth.clamp(controller.dropdownScroll(), 0, Math.max(0, dimensions.size() - visible)));
        for (int index = 0; index < visible; index++) {
            int dimensionIndex = controller.dropdownScroll() + index;
            String id = dimensions.get(dimensionIndex).id();
            int rowY = box.y() + 2 + index * 18;
            boolean hover = inside(mouseX, mouseY, box.x() + 2, rowY, box.width() - 4, 18);
            if (hover || dimensionIndex == controller.dropdownIndex()) {
                graphics.fill(box.x() + 2, rowY, box.x() + box.width() - 2, rowY + 18,
                    id.equals(controller.dimension()) ? 0x773F7180 : 0x5530333A);
            }
            graphics.text(font, trim(dropdownLabel(id), box.width() - 12), box.x() + 6,
                rowY + 5, id.equals(controller.dimension()) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
    }

    private boolean clickDimensionDropdown(double mouseX, double mouseY) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        DropdownBox box = dropdownBox(dimensions.size());
        if (!inside(mouseX, mouseY, box.x(), box.y(), box.width(), box.height())) return false;
        int visible = Math.min(7, dimensions.size());
        if (mouseY < box.y() + 2 || mouseY >= box.y() + 2 + visible * 18) return true;
        int index = (int) ((mouseY - box.y() - 2) / 18) + controller.dropdownScroll();
        if (index >= 0 && index < dimensions.size()) selectDimension(dimensions.get(index).id());
        return true;
    }

    private boolean dropdownKeyPressed(int keyCode) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        if (keyCode == 256) {
            controller.closeDropdown();
            return true;
        }
        if (dimensions.isEmpty()) return true;
        if (keyCode == 265 || keyCode == 264) {
            controller.moveDropdownSelection(keyCode == 265 ? -1 : 1, dimensions.size());
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            selectDimension(dimensions.get(controller.dropdownIndex()).id());
            controller.closeDropdown();
            return true;
        }
        return true;
    }

    private DropdownBox dropdownBox(int count) {
        int visible = Math.min(7, count);
        int selectorX = dimensionSelector == null ? panelX + 18 : dimensionSelector.getX();
        int selectorY = dimensionSelector == null ? panelY + 39 : dimensionSelector.getY();
        return new DropdownBox(selectorX, selectorY + 20, panelWidth - 36, visible * 18 + 4);
    }

    private String trim(String value, int maxWidth) {
        if (maxWidth <= 8) return "";
        return font.width(value) <= maxWidth ? value
            : font.plainSubstrByWidth(value, maxWidth - 8) + "…";
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private record DropdownBox(int x, int y, int width, int height) {}

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    String selectedDimension() {
        return controller.dimension();
    }

    private ThemedButton button(int buttonX, int buttonY, int buttonWidth, int buttonHeight,
                                Component label, boolean portalAction,
                                java.util.function.Consumer<ThemedButton> action) {
        return addRenderableWidget(new ThemedButton(
            buttonX, buttonY, buttonWidth, buttonHeight, label, portalAction, action));
    }

    private void sendSetting(String setting, String value) {
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting);
            tag.putString("Value", value);
        });
    }

    private DimensionalNavigationWorkflow.ExactFields exactFields() {
        return new DimensionalNavigationWorkflow.ExactFields(
            value(nameField, name), value(xField, x), value(yField, y),
            value(zField, z), value(yawField, yaw));
    }

    private static String value(EditBox field, String fallback) {
        return field == null ? fallback : field.getValue();
    }

    private String currentDimension() {
        return minecraft == null || minecraft.level == null ? "minecraft:overworld"
            : minecraft.level.dimension().identifier().toString();
    }

    private static String displayDimension(String id) {
        return DimensionLabelState.label(id).orElseGet(() -> friendlyDimension(
            id.substring(id.lastIndexOf(':') + 1)));
    }

    private String dropdownLabel(String id) {
        return dropdownLabels.getOrDefault(id, displayDimension(id));
    }

    private void rebuildDropdownLabels() {
        Map<String, Integer> counts = new HashMap<>();
        for (DimensionLabelState.DimensionInfo info : DimensionLabelState.dimensions()) {
            counts.merge(displayDimension(info.id()), 1, Integer::sum);
        }
        Map<String, String> rebuilt = new HashMap<>();
        for (DimensionLabelState.DimensionInfo info : DimensionLabelState.dimensions()) {
            String id = info.id();
            String display = displayDimension(id);
            rebuilt.put(id, counts.getOrDefault(display, 0) > 1
                ? display + " - " + id.substring(0, id.indexOf(':')) : display);
        }
        dropdownLabels = Map.copyOf(rebuilt);
    }

    private static String friendlyDimension(String path) {
        StringBuilder result = new StringBuilder();
        for (String word : path.replace('_', ' ').split(" ")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word.isEmpty() ? word
                : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return result.toString();
    }
}
