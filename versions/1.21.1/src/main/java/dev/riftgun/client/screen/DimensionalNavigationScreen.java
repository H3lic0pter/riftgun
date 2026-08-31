package dev.riftgun.client.screen;

import dev.riftgun.client.DimensionLabelState;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.navigation.DimensionalTraversalMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Fixed-size, per-gun cross-dimensional navigation editor. */
public final class DimensionalNavigationScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 230;
    private static final int FIELD_HEIGHT = 18;
    private final PortalConfigScreen parent;
    private final UUID group;
    private String dimension;
    private DimensionalTraversalMode mode;
    private String name = "";
    private String x = "";
    private String y = "";
    private String z = "";
    private String yaw = "";
    private boolean coordinatesEdited;
    private boolean coordinateDefaultsInitialized;
    private boolean dropdownOpen;
    private int dropdownIndex;
    private int dropdownScroll;
    private boolean saving;
    private UUID selectedBeforeSave;
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
        this.group = group == null ? PortalPlayerData.DEFAULT_GROUP_ID : group;
        dimension = Nbt.getString(PortalClientState.gun(), "DimensionalTraversalDimension");
        mode = DimensionalTraversalMode.parse(
            Nbt.getString(PortalClientState.gun(), "DimensionalTraversalMode"));
    }

    @Override
    protected void init() {
        if (!knownDimension(dimension)) dimension = currentDimension();
        if (!coordinateDefaultsInitialized) {
            coordinateDefaultsInitialized = resetCoordinateDefaults();
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
        backButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("screen.riftgun.back")));
        dimensionSelector = button(left, panelY + 39, contentWidth - 22, 18,
            Component.literal(displayDimension(dimension)), false, ignored -> {});
        dimensionSelector.horizontalMarquee();
        dimensionSelector.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.literal(dimension)));
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
        if (!dropdownOpen && mode == DimensionalTraversalMode.EXACT_COORDINATES) {
            exact.accented(0xFF31506B, 0xFF3F698C, PortalTheme.TEXT);
        } else {
            automatic.accented(0xFF31506B, 0xFF3F698C, PortalTheme.TEXT);
        }
        automatic.active = PortalClientState.randomRift().getBoolean("Enabled");
        if (!automatic.active) automatic.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
            Component.translatable("screen.riftgun.dimensional_navigation.random_disabled")));
        if (mode == DimensionalTraversalMode.EXACT_COORDINATES) initExactFields(left, contentWidth);
        ThemedButton action = button(left, panelY + panelHeight - 31, contentWidth, 20,
            Component.translatable(mode == DimensionalTraversalMode.EXACT_COORDINATES
                ? "screen.riftgun.dimensional_navigation.save"
                : "screen.riftgun.dimensional_navigation.open"), true,
            ignored -> performAction());
        action.active = mode != DimensionalTraversalMode.AUTOMATIC_SEARCH
            || PortalClientState.randomRift().getBoolean("Enabled");
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

    private EditBox field(int x, int y, int width, String value, int maxLength,
                          java.util.function.Consumer<String> responder, boolean coordinate) {
        EditBox field = new EditBox(font, x, y, width, FIELD_HEIGHT, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            if (coordinate) coordinatesEdited = true;
        });
        return addRenderableWidget(field);
    }

    void selectDimension(String id) {
        if (!knownDimension(id)) return;
        dimension = id;
        dropdownOpen = false;
        if (!coordinatesEdited) coordinateDefaultsInitialized = resetCoordinateDefaults();
        PortalClientState.gun().putString("DimensionalTraversalDimension", id);
        sendSetting("DimensionalTraversalDimension", id);
        if (dimensionSelector != null) {
            dimensionSelector.setMessage(Component.literal(displayDimension(id)));
            dimensionSelector.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal(id)));
        }
    }

    private void selectMode(DimensionalTraversalMode selected) {
        if (selected == DimensionalTraversalMode.AUTOMATIC_SEARCH
            && !PortalClientState.randomRift().getBoolean("Enabled")) return;
        mode = selected;
        PortalClientState.gun().putString("DimensionalTraversalMode", selected.name());
        sendSetting("DimensionalTraversalMode", selected.name());
        clearWidgets();
        init();
    }

    private void performAction() {
        if (mode == DimensionalTraversalMode.AUTOMATIC_SEARCH) {
            PortalNetworking.sendRequest(PortalAction.OPEN_DIMENSIONAL_RIFT,
                tag -> tag.putString("Dimension", dimension));
            minecraft.setScreen(null);
            return;
        }
        saving = true;
        selectedBeforeSave = PortalClientState.data().selectedDestinationId();
        PortalNetworking.sendRequest(PortalAction.CREATE_DIMENSIONAL_COORDINATE, tag -> {
            tag.putString("Dimension", dimension);
            tag.putString("Name", nameField.getValue());
            tag.putString("X", xField.getValue());
            tag.putString("Y", yField.getValue());
            tag.putString("Z", zField.getValue());
            tag.putString("Yaw", yawField.getValue());
            Nbt.putUUID(tag, "Group", group);
        });
        clearWidgets();
        init();
    }

    public void onServerSnapshot() {
        if (!saving) return;
        UUID selected = PortalClientState.data().selectedDestinationId();
        if (selected != null && !selected.equals(selectedBeforeSave)) {
            parent.refreshFromServer(Set.of());
            minecraft.setScreen(parent);
        }
    }

    private boolean resetCoordinateDefaults() {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return false;
        double sourceScale = minecraft.level.dimensionType().coordinateScale();
        double targetScale = DimensionLabelState.dimensions().stream()
            .filter(info -> info.id().equals(dimension)).mapToDouble(
                DimensionLabelState.DimensionInfo::coordinateScale).findFirst().orElse(sourceScale);
        x = coordinate(minecraft.player.getX() * sourceScale / targetScale);
        y = coordinate(minecraft.player.getY());
        z = coordinate(minecraft.player.getZ() * sourceScale / targetScale);
        yaw = coordinate(minecraft.player.getYRot());
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.drawString(font, title, panelX + 12, panelY + 12, PortalTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftgun.dimensional_navigation.dimension"),
            panelX + 18, panelY + 29, PortalTheme.TEXT_MUTED, false);
        if (mode == DimensionalTraversalMode.EXACT_COORDINATES) {
            graphics.drawString(font, Component.translatable("screen.riftgun.name"),
                panelX + 18, panelY + 131, PortalTheme.TEXT_MUTED, false);
            int left = panelX + 18;
            int contentWidth = panelWidth - 36;
            int gap = 4;
            int coordinateWidth = (contentWidth - gap * 3) / 4;
            String[] keys = {"screen.riftgun.x", "screen.riftgun.y", "screen.riftgun.z", "screen.riftgun.yaw"};
            for (int index = 0; index < 4; index++) {
                graphics.drawString(font, Component.translatable(keys[index]),
                    left + index * (coordinateWidth + gap), panelY + 153, PortalTheme.TEXT_MUTED, false);
            }
        }
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
        }
        if (backButton != null) PortalGuiIcons.drawCompactBackButtonIcon(
            graphics, backButton.getX(), backButton.getY());
        if (dimensionDropdownButton != null) PortalGuiIcons.drawDownIcon(graphics,
            dimensionDropdownButton.getX() + 6, dimensionDropdownButton.getY() + 7);
        if (dropdownOpen) renderDimensionDropdown(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dropdownOpen) {
            if (button == 0) clickDimensionDropdown(mouseX, mouseY);
            dropdownOpen = false;
            return true;
        }
        if (dimensionSelector != null && (button == 0 || button == 1)
            && inside(mouseX, mouseY, dimensionSelector.getX(), dimensionSelector.getY(),
                dimensionSelector.getWidth(), dimensionSelector.getHeight())) {
            String before = dimension;
            shiftDimension(button == 0 ? 1 : -1);
            if (!before.equals(dimension) && minecraft != null) {
                dimensionSelector.playDownSound(minecraft.getSoundManager());
            }
            setFocused(dimensionSelector);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (dropdownOpen) {
            int visible = Math.min(7, DimensionLabelState.dimensions().size());
            dropdownScroll = Mth.clamp(dropdownScroll - (int) Math.signum(verticalAmount),
                0, Math.max(0, DimensionLabelState.dimensions().size() - visible));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (dropdownOpen) return dropdownKeyPressed(keyCode);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openDimensionDropdown() {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        dropdownOpen = true;
        dropdownIndex = Math.max(0, indexOfDimension(dimensions, dimension));
        dropdownScroll = Mth.clamp(dropdownIndex - 3, 0, Math.max(0, dimensions.size() - 7));
        setFocused(dimensionSelector);
    }

    private void shiftDimension(int delta) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        if (dimensions.isEmpty()) return;
        int current = Math.max(0, indexOfDimension(dimensions, dimension));
        int next = Math.floorMod(current + delta, dimensions.size());
        selectDimension(dimensions.get(next).id());
    }

    private void renderDimensionDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        DropdownBox box = dropdownBox(dimensions.size());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.renderOutline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        int visible = Math.min(7, dimensions.size());
        dropdownScroll = Mth.clamp(dropdownScroll, 0, Math.max(0, dimensions.size() - visible));
        for (int index = 0; index < visible; index++) {
            int dimensionIndex = dropdownScroll + index;
            String id = dimensions.get(dimensionIndex).id();
            int rowY = box.y() + 2 + index * 18;
            boolean hover = inside(mouseX, mouseY, box.x() + 2, rowY, box.width() - 4, 18);
            if (hover || dimensionIndex == dropdownIndex) {
                graphics.fill(box.x() + 2, rowY, box.x() + box.width() - 2, rowY + 18,
                    id.equals(dimension) ? 0x773F7180 : 0x5530333A);
            }
            graphics.drawString(font, trim(dropdownLabel(id), box.width() - 12), box.x() + 6,
                rowY + 5, id.equals(dimension) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
        graphics.pose().popPose();
    }

    private boolean clickDimensionDropdown(double mouseX, double mouseY) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        DropdownBox box = dropdownBox(dimensions.size());
        if (!inside(mouseX, mouseY, box.x(), box.y(), box.width(), box.height())) return false;
        int visible = Math.min(7, dimensions.size());
        if (mouseY < box.y() + 2 || mouseY >= box.y() + 2 + visible * 18) return true;
        int index = (int) ((mouseY - box.y() - 2) / 18) + dropdownScroll;
        if (index >= 0 && index < dimensions.size()) selectDimension(dimensions.get(index).id());
        return true;
    }

    private boolean dropdownKeyPressed(int keyCode) {
        List<DimensionLabelState.DimensionInfo> dimensions = DimensionLabelState.dimensions();
        if (keyCode == 256) {
            dropdownOpen = false;
            return true;
        }
        if (dimensions.isEmpty()) return true;
        if (keyCode == 265 || keyCode == 264) {
            dropdownIndex = Mth.clamp(dropdownIndex + (keyCode == 265 ? -1 : 1),
                0, dimensions.size() - 1);
            if (dropdownIndex < dropdownScroll) dropdownScroll = dropdownIndex;
            if (dropdownIndex >= dropdownScroll + 7) dropdownScroll = dropdownIndex - 6;
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            selectDimension(dimensions.get(dropdownIndex).id());
            dropdownOpen = false;
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

    private static int indexOfDimension(List<DimensionLabelState.DimensionInfo> dimensions, String id) {
        for (int index = 0; index < dimensions.size(); index++) {
            if (dimensions.get(index).id().equals(id)) return index;
        }
        return -1;
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
        return dimension;
    }

    private ThemedButton button(int x, int y, int width, int height, Component label,
                                boolean portalAction, java.util.function.Consumer<ThemedButton> action) {
        return addRenderableWidget(new ThemedButton(x, y, width, height, label, portalAction, action));
    }

    private void sendSetting(String setting, String value) {
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting);
            tag.putString("Value", value);
        });
    }

    private static String coordinate(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean knownDimension(String id) {
        return DimensionLabelState.dimensions().stream().anyMatch(info -> info.id().equals(id));
    }

    private String currentDimension() {
        return minecraft == null || minecraft.level == null ? "minecraft:overworld"
            : minecraft.level.dimension().location().toString();
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
