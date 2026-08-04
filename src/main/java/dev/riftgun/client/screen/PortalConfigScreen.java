package dev.riftgun.client.screen;

import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.service.SafetyReport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class PortalConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 48;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ROW_HEIGHT = 18;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int listTop;
    private int listBottom;
    private boolean compactLayout;
    private int scroll;
    private int contentHeight;
    private String query = "";
    private @Nullable UUID viewedDestination;
    private @Nullable UUID selectedGroup;
    private @Nullable UUID draggingGroup;
    private double dragStartY;
    private final List<Row> hitRows = new ArrayList<>();

    private Modal modal = Modal.NONE;
    private Modal returnModal = Modal.NONE;
    private @Nullable UUID modalTarget;
    private @Nullable UUID unsafeDestination;
    private boolean dirty;
    private String formName = "";
    private String formX = "";
    private String formY = "";
    private String formZ = "";
    private String formYaw = "";
    private UUID formGroup = PortalPlayerData.DEFAULT_GROUP_ID;
    private @Nullable EditBox searchBox;

    public PortalConfigScreen() {
        super(Component.translatable("screen.riftgun.config"));
        PortalPlayerData data = PortalClientState.data();
        viewedDestination = data.selectedDestinationId() != null
            ? data.selectedDestinationId() : data.lastViewedDestinationId();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 12);
        panelHeight = Math.min(320, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        compactLayout = panelWidth < 360 || panelHeight < 210;
        listWidth = compactLayout ? Math.max(132, panelWidth * 54 / 100)
            : Math.max(156, panelWidth * 57 / 100);
        listTop = panelY + HEADER_HEIGHT;
        listBottom = panelY + panelHeight - FOOTER_HEIGHT;

        if (modal != Modal.NONE) {
            initModal();
            return;
        }

        searchBox = new EditBox(font, panelX + 10, panelY + 25, listWidth - 20, 17,
            Component.translatable("screen.riftgun.search"));
        searchBox.setValue(query);
        searchBox.setHint(Component.translatable("screen.riftgun.search_hint"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(value -> query = value);
        addRenderableWidget(searchBox);

        int rightX = panelX + listWidth + 8;
        int available = panelWidth - listWidth - 16;
        int compactButtonWidth = Math.max(26, (available - 6) / 3);
        button(rightX, panelY + 24, compactButtonWidth, 18, "screen.riftgun.save_here", false,
            ignored -> openForm(Modal.CREATE_CURRENT, null));
        button(rightX + compactButtonWidth + 3, panelY + 24, compactButtonWidth, 18, "screen.riftgun.add_coordinate", false,
            ignored -> openForm(Modal.CREATE_COORDINATE, null));
        button(rightX + (compactButtonWidth + 3) * 2, panelY + 24, compactButtonWidth, 18, "screen.riftgun.add_group", false,
            ignored -> openForm(Modal.CREATE_GROUP, null));

        int detailButtonY = listBottom - 22;
        ThemedButton edit = button(rightX, detailButtonY, 44, 18, "screen.riftgun.edit", false,
            ignored -> editSelection());
        ThemedButton pin = button(rightX + 47, detailButtonY, 44, 18, "screen.riftgun.pin", false,
            ignored -> togglePin());
        ThemedButton remove = button(rightX + 94, detailButtonY, Math.max(44, available - 94), 18,
            "screen.riftgun.delete", false, ignored -> confirmDelete());
        edit.active = viewed() != null || editableGroup();
        pin.active = viewed() != null;
        remove.active = viewed() != null || editableGroup();

        int footerY = panelY + panelHeight - 28;
        button(panelX + 10, footerY, 54, 19, "screen.riftgun.settings", false,
            ignored -> openForm(Modal.SETTINGS, null));
        button(panelX + 67, footerY, 82, 19,
            Component.translatable("screen.riftgun.sort_mode", Component.translatable(
                "screen.riftgun.sort." + PortalClientState.data().settings().sort().name().toLowerCase(Locale.ROOT))), false,
            ignored -> cycleSort());
        button(panelX + listWidth + 8, footerY, Math.max(34, available / 2 - 2), 19,
            "screen.riftgun.select", false, ignored -> selectViewed()).active = viewed() != null;
        button(panelX + listWidth + 8 + available / 2 + 2, footerY,
            Math.max(34, available - available / 2 - 2), 19,
            "screen.riftgun.generate", true, ignored -> generatePortal()).active = viewed() != null;

        if (editableGroup()) {
            button(rightX, listTop + 52, 32, 18, Component.literal("↑"), false,
                ignored -> moveSelectedGroup(-1));
            button(rightX + 35, listTop + 52, 32, 18, Component.literal("↓"), false,
                ignored -> moveSelectedGroup(1));
        }
    }

    private void initModal() {
        int boxWidth = Math.min(310, panelWidth - (compactLayout ? 12 : 36));
        int x = panelX + (panelWidth - boxWidth) / 2;
        int y = modalY();
        if (modal.hasName) {
            addField(x + 18, y + (compactLayout ? 27 : 32), boxWidth - 36, formName, 48,
                value -> formName = value);
        }
        if (modal.hasCoordinates) {
            int fieldWidth = (boxWidth - 45) / 2;
            int firstRow = y + (compactLayout ? 51 : 57);
            int secondRow = y + (compactLayout ? 74 : 82);
            addField(x + 18, firstRow, fieldWidth, formX, 64, value -> formX = value);
            addField(x + 27 + fieldWidth, firstRow, fieldWidth, formY, 64, value -> formY = value);
            addField(x + 18, secondRow, fieldWidth, formZ, 64, value -> formZ = value);
            addField(x + 27 + fieldWidth, secondRow, fieldWidth, formYaw, 64, value -> formYaw = value);
        }
        if (modal.isDestinationForm()) {
            int groupY = y + (modal.hasCoordinates ? compactLayout ? 97 : 107 : 76);
            button(x + 18, groupY, boxWidth - 36, 18,
                Component.translatable("screen.riftgun.group_value", groupName(formGroup)), false,
                ignored -> cycleFormGroup());
        }
        int buttonY = y + (modal.hasCoordinates ? compactLayout ? 122 : 135
            : modal.isDestinationForm() ? 104 : modal.hasName ? 62 : 76);
        if (modal == Modal.SETTINGS) {
            PortalPlayerSettings settings = PortalClientState.data().settings();
            button(x + 18, y + 34, boxWidth - 36, 18,
                toggleLabel("screen.riftgun.safety", settings.safetyCheckEnabled()), false,
                ignored -> updateSetting(0));
            button(x + 18, y + 57, boxWidth - 36, 18,
                toggleLabel("screen.riftgun.animations", settings.animationsEnabled()), false,
                ignored -> updateSetting(1));
            button(x + 18, y + 80, boxWidth - 36, 18,
                toggleLabel("screen.riftgun.sounds", settings.soundsEnabled()), false,
                ignored -> updateSetting(2));
            buttonY = y + 110;
        }

        if (modal.isConfirmation()) {
            button(x + 18, buttonY, (boxWidth - 42) / 2, 19, "screen.riftgun.cancel", false,
                ignored -> cancelConfirmation());
            button(x + 24 + (boxWidth - 42) / 2, buttonY, (boxWidth - 42) / 2, 19,
                modal == Modal.CONFIRM_UNSAFE ? "screen.riftgun.open_anyway" : "screen.riftgun.confirm",
                modal == Modal.CONFIRM_UNSAFE, ignored -> acceptConfirmation());
        } else {
            button(x + 18, buttonY, (boxWidth - 42) / 2, 19, "screen.riftgun.cancel", false,
                ignored -> requestCloseModal());
            button(x + 24 + (boxWidth - 42) / 2, buttonY, (boxWidth - 42) / 2, 19,
                modal == Modal.SETTINGS ? "screen.riftgun.done" : "screen.riftgun.save", false,
                ignored -> submitModal());
        }
    }

    private EditBox addField(int x, int y, int width, String value, int maxLength, Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 18, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            dirty = true;
        });
        return addRenderableWidget(field);
    }

    private ThemedButton button(int x, int y, int width, int height, String key, boolean portalAction,
                                Consumer<ThemedButton> action) {
        return button(x, y, width, height, Component.translatable(key), portalAction, action);
    }

    private ThemedButton button(int x, int y, int width, int height, Component label, boolean portalAction,
                                Consumer<ThemedButton> action) {
        return addRenderableWidget(new ThemedButton(x, y, width, height, label, portalAction, action));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.fill(panelX + listWidth, panelY, panelX + listWidth + 1,
            panelY + panelHeight - FOOTER_HEIGHT, PortalTheme.BORDER);
        graphics.fill(panelX, panelY + HEADER_HEIGHT - 1, panelX + panelWidth,
            panelY + HEADER_HEIGHT, PortalTheme.BORDER);
        graphics.fill(panelX, listBottom, panelX + panelWidth, listBottom + 1, PortalTheme.BORDER);
        graphics.drawString(font, title, panelX + 10, panelY + 8, PortalTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftgun.details"),
            panelX + listWidth + 8, panelY + 8, PortalTheme.TEXT_MUTED, false);

        renderRows(graphics, mouseX, mouseY);
        renderDetails(graphics, mouseX, mouseY);
        if (modal != Modal.NONE) renderModal(graphics);
        graphics.flush();
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
        }
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        hitRows.clear();
        List<Row> rows = buildRows();
        contentHeight = rows.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        graphics.enableScissor(panelX + 1, listTop, panelX + listWidth - 1, listBottom);
        int y = listTop - scroll;
        for (Row row : rows) {
            if (y + ROW_HEIGHT >= listTop && y <= listBottom) {
                Row positioned = new Row(row.kind, row.id, y);
                hitRows.add(positioned);
                boolean hover = mouseX >= panelX + 4 && mouseX < panelX + listWidth - 4
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
                boolean selected = row.kind == RowKind.DESTINATION
                    ? row.id.equals(viewedDestination) : row.id.equals(selectedGroup);
                if (selected) graphics.fill(panelX + 4, y, panelX + listWidth - 4, y + ROW_HEIGHT, 0x663F7180);
                else if (hover) graphics.fill(panelX + 4, y, panelX + listWidth - 4, y + ROW_HEIGHT, 0x5530333A);
                if (row.kind == RowKind.GROUP) renderGroupRow(graphics, row.id, y);
                else renderDestinationRow(graphics, row.id, y);
            }
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int track = listBottom - listTop - 4;
            int thumb = Math.max(12, track * (listBottom - listTop) / contentHeight);
            int thumbY = listTop + 2 + (track - thumb) * scroll / maxScroll;
            graphics.fill(panelX + listWidth - 3, thumbY, panelX + listWidth - 1, thumbY + thumb, PortalTheme.ICE);
        }
    }

    private void renderGroupRow(GuiGraphics graphics, UUID id, int y) {
        PortalPlayerData data = PortalClientState.data();
        boolean expanded = data.expandedGroups().contains(id);
        String name = id.equals(PortalPlayerData.DEFAULT_GROUP_ID) ? "Default"
            : data.group(id).map(DestinationGroup::name).orElse("?");
        long count = data.destinations().stream().filter(destination -> destination.groupId().equals(id)).count();
        graphics.drawString(font, expanded ? "▾" : "▸", panelX + 9, y + 5, PortalTheme.ICE, false);
        graphics.drawString(font, name, panelX + 20, y + 5, PortalTheme.TEXT, false);
        String countText = Long.toString(count);
        graphics.drawString(font, countText, panelX + listWidth - 11 - font.width(countText), y + 5,
            PortalTheme.TEXT_MUTED, false);
    }

    private void renderDestinationRow(GuiGraphics graphics, UUID id, int y) {
        Destination destination = PortalClientState.data().destination(id).orElse(null);
        if (destination == null) return;
        int x = panelX + 23;
        if (destination.pinned()) {
            graphics.drawString(font, "◆", x, y + 5, PortalTheme.WARNING, false);
            x += 10;
        }
        boolean target = id.equals(PortalClientState.data().selectedDestinationId());
        graphics.drawString(font, trim(destination.name(), listWidth - (x - panelX) - 32), x, y + 5,
            target ? PortalTheme.ICE : PortalTheme.TEXT_MUTED, false);
        Integer flags = PortalClientState.safety(id);
        if (flags != null && flags != 0) {
            graphics.drawString(font, "!", panelX + listWidth - 15, y + 5, PortalTheme.WARNING, false);
        }
    }

    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelX + listWidth + 9;
        int y = listTop + 10;
        Destination destination = viewed();
        if (destination == null) {
            graphics.drawString(font, Component.translatable("screen.riftgun.empty_details"),
                x, y, PortalTheme.TEXT_MUTED, false);
            if (editableGroup()) renderGroupTools(graphics, x, y + 24);
            return;
        }
        if (compactLayout) {
            renderCompactDetails(graphics, destination, x, listTop + 4);
            return;
        }
        label(graphics, "screen.riftgun.name", x, y);
        graphics.drawString(font, trim(destination.name(), panelWidth - listWidth - 20), x, y + 11,
            PortalTheme.TEXT, false);
        y += 31;
        label(graphics, "screen.riftgun.group", x, y);
        graphics.drawString(font, groupName(destination.groupId()), x, y + 11, PortalTheme.TEXT, false);
        y += 31;
        label(graphics, "screen.riftgun.dimension", x, y);
        String dimension = friendlyDimension(destination.dimension().location().getPath());
        graphics.drawString(font, trim(dimension, panelWidth - listWidth - 20), x, y + 11, PortalTheme.TEXT, false);
        if (mouseX >= x && mouseX <= panelX + panelWidth - 8 && mouseY >= y + 9 && mouseY <= y + 22) {
            graphics.renderTooltip(font, Component.literal(destination.dimension().location().toString()), mouseX, mouseY);
        }
        y += 31;
        label(graphics, "screen.riftgun.coordinates", x, y);
        graphics.drawString(font, String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
            destination.x(), destination.y(), destination.z()), x, y + 11, PortalTheme.TEXT, false);
        Integer flags = PortalClientState.safety(destination.id());
        if (flags != null) {
            y += 31;
            graphics.drawString(font, flags == 0 ? Component.translatable("screen.riftgun.safe")
                : Component.translatable("screen.riftgun.unsafe"), x, y,
                flags == 0 ? PortalTheme.ICE : PortalTheme.WARNING, false);
        }
    }

    private void renderCompactDetails(GuiGraphics graphics, Destination destination, int x, int y) {
        int width = panelWidth - listWidth - 18;
        graphics.drawString(font, trim(Component.translatable("screen.riftgun.name").getString() + ": "
            + destination.name(), width), x, y, PortalTheme.TEXT, false);
        graphics.drawString(font, trim(Component.translatable("screen.riftgun.group").getString() + ": "
            + groupName(destination.groupId()), width), x, y + 9, PortalTheme.TEXT_MUTED, false);
        graphics.drawString(font, trim(Component.translatable("screen.riftgun.dimension").getString() + ": "
            + friendlyDimension(destination.dimension().location().getPath()), width), x, y + 18,
            PortalTheme.TEXT_MUTED, false);
        graphics.drawString(font, trim(String.format(Locale.ROOT, "%.1f %.1f %.1f",
            destination.x(), destination.y(), destination.z()), width), x, y + 27, PortalTheme.TEXT_MUTED, false);
    }

    private void renderGroupTools(GuiGraphics graphics, int x, int y) {
        DestinationGroup group = PortalClientState.data().group(selectedGroup).orElse(null);
        if (group == null) return;
        graphics.drawString(font, Component.translatable("screen.riftgun.group_selected", group.name()),
            x, y, PortalTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftgun.group_reorder_hint"),
            x, y + 14, PortalTheme.TEXT_MUTED, false);
    }

    private void renderModal(GuiGraphics graphics) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xB8101115);
        int boxWidth = Math.min(310, panelWidth - (compactLayout ? 12 : 36));
        int x = panelX + (panelWidth - boxWidth) / 2;
        int y = modalY();
        int height = modal.hasCoordinates ? compactLayout ? 148 : 178 : modal.isDestinationForm() ? 146
            : modal == Modal.SETTINGS ? 154 : 106;
        graphics.fill(x, y, x + boxWidth, y + height, PortalTheme.PANEL_RAISED);
        graphics.renderOutline(x, y, boxWidth, height,
            modal == Modal.CONFIRM_UNSAFE ? PortalTheme.WARNING : PortalTheme.BORDER_FOCUS);
        graphics.drawString(font, Component.translatable(modal.titleKey), x + 18, y + 14,
            modal == Modal.CONFIRM_UNSAFE ? PortalTheme.WARNING : PortalTheme.TEXT, false);
        if (modal.hasName) label(graphics, "screen.riftgun.name", x + 18, y + (compactLayout ? 19 : 24));
        if (modal.hasCoordinates) {
            label(graphics, "screen.riftgun.xyz", x + 18, y + (compactLayout ? 43 : 49));
            label(graphics, "screen.riftgun.yaw", x + boxWidth / 2 + 4, y + (compactLayout ? 66 : 74));
        }
        if (modal.isDestinationForm()) {
            label(graphics, "screen.riftgun.group", x + 18,
                y + (modal.hasCoordinates ? compactLayout ? 89 : 99 : 68));
        }
        if (modal == Modal.CREATE_CURRENT && minecraft != null && minecraft.player != null) {
            graphics.drawString(font, String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
                minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()),
                x + 18, y + 55, PortalTheme.TEXT_MUTED, false);
        }
        if (modal == Modal.CONFIRM_DELETE_DESTINATION || modal == Modal.CONFIRM_DELETE_GROUP
            || modal == Modal.CONFIRM_DIRTY || modal == Modal.CONFIRM_UNSAFE) {
            graphics.drawWordWrap(font, Component.translatable(modal.bodyKey), x + 18, y + 36,
                boxWidth - 36, PortalTheme.TEXT_MUTED);
        }
    }

    private int modalY() {
        return compactLayout ? Math.max(2, (height - Math.min(154, height - 4)) / 2) : panelY + 48;
    }

    private List<Row> buildRows() {
        PortalPlayerData data = PortalClientState.data();
        List<Row> rows = new ArrayList<>();
        List<UUID> groups = new ArrayList<>();
        groups.add(PortalPlayerData.DEFAULT_GROUP_ID);
        data.groups().stream().sorted(Comparator.comparingInt(DestinationGroup::order))
            .map(DestinationGroup::id).forEach(groups::add);
        String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);
        for (UUID groupId : groups) {
            String groupName = groupName(groupId);
            List<Destination> destinations = data.destinations().stream()
                .filter(destination -> destination.groupId().equals(groupId))
                .filter(destination -> matches(destination, groupName, normalizedQuery))
                .sorted(destinationComparator(data.settings().sort())).toList();
            boolean groupMatch = normalizedQuery.isEmpty() || groupName.toLowerCase(Locale.ROOT).contains(normalizedQuery);
            if (!groupMatch && destinations.isEmpty()) continue;
            rows.add(new Row(RowKind.GROUP, groupId, 0));
            if (data.expandedGroups().contains(groupId) || !normalizedQuery.isEmpty()) {
                destinations.forEach(destination -> rows.add(new Row(RowKind.DESTINATION, destination.id(), 0)));
            }
        }
        return rows;
    }

    private Comparator<Destination> destinationComparator(DestinationSort sort) {
        Comparator<Destination> comparator = Comparator.comparing(Destination::pinned).reversed();
        Comparator<Destination> secondary = switch (sort) {
            case RECENT -> Comparator.comparingLong(Destination::lastUsedAt).reversed();
            case NAME -> Comparator.comparing(value -> value.name().toLowerCase(Locale.ROOT));
            case CREATED -> Comparator.comparingLong(Destination::createdAt).reversed();
            case DISTANCE -> Comparator.comparingDouble(this::distanceSquared);
        };
        return comparator.thenComparing(secondary).thenComparing(Destination::id);
    }

    private double distanceSquared(Destination destination) {
        if (minecraft == null || minecraft.player == null
            || !minecraft.player.level().dimension().equals(destination.dimension())) return Double.POSITIVE_INFINITY;
        return minecraft.player.position().distanceToSqr(destination.position());
    }

    private boolean matches(Destination destination, String group, String normalized) {
        if (normalized.isEmpty() || group.toLowerCase(Locale.ROOT).contains(normalized)
            || destination.name().toLowerCase(Locale.ROOT).contains(normalized)) return true;
        String coordinates = String.format(Locale.ROOT, "%s %s %s", destination.x(), destination.y(), destination.z());
        return coordinates.contains(normalized);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (modal == Modal.NONE && button == 0 && mouseX >= panelX && mouseX < panelX + listWidth
            && mouseY >= listTop && mouseY < listBottom) {
            for (Row row : hitRows) {
                if (mouseY >= row.y && mouseY < row.y + ROW_HEIGHT) {
                    if (row.kind == RowKind.GROUP) {
                        draggingGroup = row.id;
                        dragStartY = mouseY;
                        selectedGroup = row.id;
                        viewedDestination = null;
                        return true;
                    }
                    activateRow(row);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingGroup != null) {
            UUID moving = draggingGroup;
            draggingGroup = null;
            if (!moving.equals(PortalPlayerData.DEFAULT_GROUP_ID) && Math.abs(mouseY - dragStartY) >= 5.0) {
                Row target = hitRows.stream()
                    .filter(row -> row.kind == RowKind.GROUP)
                    .min(Comparator.comparingDouble(row -> Math.abs(mouseY - (row.y + ROW_HEIGHT / 2.0))))
                    .orElse(null);
                if (target != null) moveGroupTo(moving, groupOrderIndex(target.id));
                return true;
            }
            activateRow(new Row(RowKind.GROUP, moving, (int) mouseY));
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (modal == Modal.NONE && mouseX >= panelX && mouseX < panelX + listWidth
            && mouseY >= listTop && mouseY < listBottom) {
            scroll = Math.max(0, scroll - (int) Math.signum(vertical) * ROW_HEIGHT * 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && modal != Modal.NONE) {
            requestCloseModal();
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && modal != Modal.NONE) {
            if (modal.isConfirmation()) acceptConfirmation();
            else submitModal();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void activateRow(Row row) {
        if (row.kind == RowKind.GROUP) {
            selectedGroup = row.id;
            viewedDestination = null;
            boolean expanded = !PortalClientState.data().expandedGroups().contains(row.id);
            PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
                tag.putUUID("Group", row.id);
                tag.putBoolean("Expanded", expanded);
            });
            return;
        }
        selectedGroup = null;
        viewedDestination = row.id;
        PortalNetworking.sendRequest(PortalAction.VIEW_DESTINATION, tag -> tag.putUUID("Destination", row.id));
        PortalNetworking.sendRequest(PortalAction.CHECK_SAFETY, tag -> tag.putUUID("Destination", row.id));
    }

    private void selectViewed() {
        if (viewedDestination == null) return;
        PortalNetworking.sendRequest(PortalAction.SELECT_DESTINATION,
            tag -> tag.putUUID("Destination", viewedDestination));
    }

    private void generatePortal() {
        if (viewedDestination == null) return;
        PortalNetworking.sendRequest(PortalAction.OPEN_PORTAL, tag -> {
            tag.putUUID("Destination", viewedDestination);
            tag.putBoolean("ConfirmedUnsafe", false);
        });
    }

    private void togglePin() {
        if (viewedDestination == null) return;
        PortalNetworking.sendRequest(PortalAction.TOGGLE_PIN,
            tag -> tag.putUUID("Destination", viewedDestination));
    }

    private void editSelection() {
        if (viewedDestination != null) openForm(Modal.EDIT_DESTINATION, viewedDestination);
        else if (editableGroup()) openForm(Modal.RENAME_GROUP, selectedGroup);
    }

    private void confirmDelete() {
        if (viewedDestination != null) openForm(Modal.CONFIRM_DELETE_DESTINATION, viewedDestination);
        else if (editableGroup()) openForm(Modal.CONFIRM_DELETE_GROUP, selectedGroup);
    }

    private void cycleSort() {
        PortalPlayerSettings current = PortalClientState.data().settings();
        sendSettings(new PortalPlayerSettings(current.safetyCheckEnabled(), current.animationsEnabled(),
            current.soundsEnabled(), current.sort().next()));
    }

    private void moveSelectedGroup(int delta) {
        if (!editableGroup()) return;
        PortalNetworking.sendRequest(PortalAction.MOVE_GROUP, tag -> {
            tag.putUUID("Group", selectedGroup);
            tag.putInt("Delta", delta);
        });
    }

    private void moveGroupTo(UUID group, int index) {
        PortalNetworking.sendRequest(PortalAction.MOVE_GROUP, tag -> {
            tag.putUUID("Group", group);
            tag.putInt("TargetIndex", index);
        });
    }

    private int groupOrderIndex(UUID group) {
        if (group.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return 0;
        List<DestinationGroup> ordered = PortalClientState.data().groups().stream()
            .sorted(Comparator.comparingInt(DestinationGroup::order)).toList();
        for (int index = 0; index < ordered.size(); index++) {
            if (ordered.get(index).id().equals(group)) return index;
        }
        return 0;
    }

    private void updateSetting(int setting) {
        PortalPlayerSettings current = PortalClientState.data().settings();
        PortalPlayerSettings next = switch (setting) {
            case 0 -> new PortalPlayerSettings(!current.safetyCheckEnabled(), current.animationsEnabled(),
                current.soundsEnabled(), current.sort());
            case 1 -> new PortalPlayerSettings(current.safetyCheckEnabled(), !current.animationsEnabled(),
                current.soundsEnabled(), current.sort());
            default -> new PortalPlayerSettings(current.safetyCheckEnabled(), current.animationsEnabled(),
                !current.soundsEnabled(), current.sort());
        };
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void sendSettings(PortalPlayerSettings settings) {
        PortalNetworking.sendRequest(PortalAction.SET_SETTINGS, tag -> {
            tag.putBoolean("SafetyCheck", settings.safetyCheckEnabled());
            tag.putBoolean("Animations", settings.animationsEnabled());
            tag.putBoolean("Sounds", settings.soundsEnabled());
            tag.putString("Sort", settings.sort().name());
        });
    }

    private void openForm(Modal next, @Nullable UUID target) {
        modal = next;
        modalTarget = target;
        dirty = false;
        formName = "";
        formX = formY = formZ = formYaw = "";
        formGroup = creationGroup();
        if (next == Modal.CREATE_COORDINATE && minecraft != null && minecraft.player != null) {
            formX = Double.toString(minecraft.player.getX());
            formY = Double.toString(minecraft.player.getY());
            formZ = Double.toString(minecraft.player.getZ());
            formYaw = Float.toString(minecraft.player.getYRot());
        } else if (next == Modal.EDIT_DESTINATION && target != null) {
            Destination destination = PortalClientState.data().destination(target).orElse(null);
            if (destination != null) {
                formName = destination.name();
                formX = Double.toString(destination.x());
                formY = Double.toString(destination.y());
                formZ = Double.toString(destination.z());
                formYaw = Float.toString(destination.yaw());
                formGroup = destination.groupId();
            }
        } else if (next == Modal.RENAME_GROUP && target != null) {
            formName = PortalClientState.data().group(target).map(DestinationGroup::name).orElse("");
        }
        rebuildWidgets();
    }

    private void cycleFormGroup() {
        List<UUID> ids = new ArrayList<>();
        ids.add(PortalPlayerData.DEFAULT_GROUP_ID);
        PortalClientState.data().groups().stream().sorted(Comparator.comparingInt(DestinationGroup::order))
            .map(DestinationGroup::id).forEach(ids::add);
        int current = ids.indexOf(formGroup);
        formGroup = ids.get((current + 1 + ids.size()) % ids.size());
        dirty = true;
        rebuildWidgets();
    }

    private void submitModal() {
        switch (modal) {
            case CREATE_CURRENT -> sendDestinationForm(PortalAction.CREATE_CURRENT, false);
            case CREATE_COORDINATE -> sendDestinationForm(PortalAction.CREATE_COORDINATE, true);
            case EDIT_DESTINATION -> sendDestinationForm(PortalAction.EDIT_DESTINATION, true);
            case CREATE_GROUP -> PortalNetworking.sendRequest(PortalAction.CREATE_GROUP,
                tag -> tag.putString("Name", formName));
            case RENAME_GROUP -> PortalNetworking.sendRequest(PortalAction.RENAME_GROUP, tag -> {
                if (modalTarget != null) tag.putUUID("Group", modalTarget);
                tag.putString("Name", formName);
            });
            case SETTINGS -> { closeModalNow(); return; }
            default -> { return; }
        }
        closeModalNow();
    }

    private void sendDestinationForm(PortalAction action, boolean coordinates) {
        UUID groupId = formGroup;
        PortalNetworking.sendRequest(action, tag -> {
            tag.putString("Name", formName);
            tag.putUUID("Group", groupId);
            if (action == PortalAction.EDIT_DESTINATION && modalTarget != null) {
                tag.putUUID("Destination", modalTarget);
            }
            if (coordinates) {
                tag.putString("X", formX);
                tag.putString("Y", formY);
                tag.putString("Z", formZ);
                tag.putString("Yaw", formYaw);
            }
        });
    }

    private UUID creationGroup() {
        if (selectedGroup != null) return selectedGroup;
        Destination current = viewed();
        return current == null ? PortalPlayerData.DEFAULT_GROUP_ID : current.groupId();
    }

    private void requestCloseModal() {
        if (modal == Modal.CONFIRM_DIRTY) {
            cancelConfirmation();
        } else if (modal.hasInputs() && dirty) {
            returnModal = modal;
            modal = Modal.CONFIRM_DIRTY;
            rebuildWidgets();
        } else {
            closeModalNow();
        }
    }

    private void acceptConfirmation() {
        if (modal == Modal.CONFIRM_DIRTY) {
            closeModalNow();
        } else if (modal == Modal.CONFIRM_DELETE_DESTINATION && modalTarget != null) {
            UUID id = modalTarget;
            PortalNetworking.sendRequest(PortalAction.DELETE_DESTINATION, tag -> tag.putUUID("Destination", id));
            viewedDestination = null;
            closeModalNow();
        } else if (modal == Modal.CONFIRM_DELETE_GROUP && modalTarget != null) {
            UUID id = modalTarget;
            PortalNetworking.sendRequest(PortalAction.DELETE_GROUP, tag -> tag.putUUID("Group", id));
            selectedGroup = null;
            closeModalNow();
        } else if (modal == Modal.CONFIRM_UNSAFE && unsafeDestination != null) {
            UUID id = unsafeDestination;
            PortalNetworking.sendRequest(PortalAction.OPEN_PORTAL, tag -> {
                tag.putUUID("Destination", id);
                tag.putBoolean("ConfirmedUnsafe", true);
            });
            closeModalNow();
        }
    }

    private void cancelConfirmation() {
        if (modal == Modal.CONFIRM_DIRTY) {
            modal = returnModal;
            rebuildWidgets();
        } else closeModalNow();
    }

    private void closeModalNow() {
        modal = Modal.NONE;
        returnModal = Modal.NONE;
        modalTarget = null;
        dirty = false;
        rebuildWidgets();
    }

    public void refreshFromServer() {
        if (viewedDestination != null && PortalClientState.data().destination(viewedDestination).isEmpty()) {
            viewedDestination = PortalClientState.data().selectedDestinationId();
        }
        if (selectedGroup != null && !selectedGroup.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && PortalClientState.data().group(selectedGroup).isEmpty()) selectedGroup = null;
        if (modal == Modal.NONE) rebuildWidgets();
    }

    public void onSafetyResult(UUID destinationId, int flags, boolean confirmation) {
        if (confirmation && flags != 0) {
            unsafeDestination = destinationId;
            openForm(Modal.CONFIRM_UNSAFE, destinationId);
        }
    }

    private @Nullable Destination viewed() {
        return viewedDestination == null ? null : PortalClientState.data().destination(viewedDestination).orElse(null);
    }

    private boolean editableGroup() {
        return selectedGroup != null && !selectedGroup.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && PortalClientState.data().group(selectedGroup).isPresent();
    }

    private String groupName(UUID id) {
        if (id.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return "Default";
        return PortalClientState.data().group(id).map(DestinationGroup::name).orElse("Default");
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, PortalTheme.TEXT_MUTED, false);
    }

    private String trim(String value, int maxWidth) {
        return font.width(value) <= maxWidth ? value : font.plainSubstrByWidth(value, Math.max(0, maxWidth - 8)) + "…";
    }

    private static String friendlyDimension(String path) {
        String[] words = path.replace('_', ' ').split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1));
        }
        return result.toString();
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable(key).append(": ").append(Component.translatable(
            value ? "screen.riftgun.on" : "screen.riftgun.off"));
    }

    private enum RowKind { GROUP, DESTINATION }
    private record Row(RowKind kind, UUID id, int y) {}

    private enum Modal {
        NONE("", "", false, false),
        CREATE_CURRENT("screen.riftgun.create_current", "", true, false),
        CREATE_COORDINATE("screen.riftgun.create_coordinate", "", true, true),
        EDIT_DESTINATION("screen.riftgun.edit_destination", "", true, true),
        CREATE_GROUP("screen.riftgun.create_group", "", true, false),
        RENAME_GROUP("screen.riftgun.rename_group", "", true, false),
        SETTINGS("screen.riftgun.settings", "", false, false),
        CONFIRM_DELETE_DESTINATION("screen.riftgun.delete", "screen.riftgun.delete_destination_body", false, false),
        CONFIRM_DELETE_GROUP("screen.riftgun.delete", "screen.riftgun.delete_group_body", false, false),
        CONFIRM_DIRTY("screen.riftgun.unsaved", "screen.riftgun.unsaved_body", false, false),
        CONFIRM_UNSAFE("screen.riftgun.unsafe", "screen.riftgun.unsafe_body", false, false);

        private final String titleKey;
        private final String bodyKey;
        private final boolean hasName;
        private final boolean hasCoordinates;

        Modal(String titleKey, String bodyKey, boolean hasName, boolean hasCoordinates) {
            this.titleKey = titleKey;
            this.bodyKey = bodyKey;
            this.hasName = hasName;
            this.hasCoordinates = hasCoordinates;
        }

        boolean isConfirmation() { return name().startsWith("CONFIRM_"); }
        boolean hasInputs() { return hasName || hasCoordinates; }
        boolean isDestinationForm() {
            return this == CREATE_CURRENT || this == CREATE_COORDINATE || this == EDIT_DESTINATION;
        }
    }
}
