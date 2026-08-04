package dev.riftgun.client.screen;

import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class PortalConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 48;
    private static final int FOOTER_HEIGHT = 36;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_ACTION_SIZE = 14;
    private static final int DETAIL_LINE_HEIGHT = 31;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int listTop;
    private int listBottom;
    private int listScroll;
    private int listContentHeight;
    private int detailScroll;
    private int detailContentHeight;
    private int detailEditY = -1;
    private boolean draggingDetailScrollbar;
    private int detailScrollbarGrab;

    private String query = "";
    private @Nullable UUID viewedDestination;
    private @Nullable UUID selectedGroup;
    private @Nullable UUID focusedRowId;
    private @Nullable RowKind focusedRowKind;
    private boolean listFocused;
    private @Nullable UUID draggingGroup;
    private double dragStartY;
    private @Nullable UUID ensureVisibleId;
    private final List<Row> hitRows = new ArrayList<>();
    private final Map<UUID, Float> animatedRowY = new HashMap<>();

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
    private boolean groupDropdownOpen;
    private int groupDropdownIndex;
    private int groupDropdownScroll;

    private @Nullable EditBox searchBox;
    private @Nullable ThemedButton firstCreateButton;
    private @Nullable ThemedButton groupSelector;
    private int groupSelectorX;
    private int groupSelectorY;
    private int groupSelectorWidth;

    public PortalConfigScreen() {
        super(Component.translatable("screen.riftgun.config"));
        PortalPlayerData data = PortalClientState.data();
        viewedDestination = data.selectedDestinationId() != null
            ? data.selectedDestinationId() : data.lastViewedDestinationId();
        focusedRowId = viewedDestination;
        focusedRowKind = viewedDestination == null ? null : RowKind.DESTINATION;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 12);
        panelHeight = Math.min(320, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        boolean compact = panelWidth < 360;
        listWidth = compact ? Math.max(132, panelWidth * 54 / 100)
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
        firstCreateButton = button(rightX, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.save_here", false, ignored -> openForm(Modal.CREATE_CURRENT, null));
        button(rightX + compactButtonWidth + 3, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.add_coordinate", false, ignored -> openForm(Modal.CREATE_COORDINATE, null));
        button(rightX + (compactButtonWidth + 3) * 2, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.add_group", false, ignored -> openForm(Modal.CREATE_GROUP, null));

        int footerY = panelY + panelHeight - 28;
        button(panelX + 10, footerY, 54, 19, "screen.riftgun.settings", false,
            ignored -> openForm(Modal.SETTINGS, null));
        button(panelX + 67, footerY, Math.min(82, listWidth - 77), 19,
            Component.translatable("screen.riftgun.sort_mode", Component.translatable(
                "screen.riftgun.sort." + PortalClientState.data().settings().sort().name().toLowerCase(Locale.ROOT))),
            false, ignored -> cycleSort());
        ThemedButton generate = button(rightX, footerY, available, 19,
            "screen.riftgun.generate", true, ignored -> generatePortal());
        generate.active = viewed() != null;

        requestSafetyIfNeeded(viewedDestination, false);
    }

    private void initModal() {
        ModalBox box = modalBox();
        int x = box.x();
        int y = box.y();
        int fieldWidth = box.width() - 36;
        groupDropdownOpen = false;

        if (modal == Modal.CREATE_COORDINATE || modal == Modal.EDIT_DESTINATION) {
            addField(x + 18, y + 41, fieldWidth, formName, 48, value -> formName = value);
            int half = (fieldWidth - 10) / 2;
            addField(x + 18, y + 80, half, formX, 64, value -> formX = value);
            addField(x + 28 + half, y + 80, half, formY, 64, value -> formY = value);
            addField(x + 18, y + 119, half, formZ, 64, value -> formZ = value);
            addField(x + 28 + half, y + 119, half, formYaw, 64, value -> formYaw = value);
            addGroupSelector(x + 18, y + 158, fieldWidth);
        } else if (modal == Modal.CREATE_CURRENT) {
            addField(x + 18, y + 41, fieldWidth, formName, 48, value -> formName = value);
            addGroupSelector(x + 18, y + 111, fieldWidth);
        } else if (modal == Modal.CREATE_GROUP || modal == Modal.RENAME_GROUP) {
            addField(x + 18, y + 44, fieldWidth, formName, 32, value -> formName = value);
        } else if (modal == Modal.SETTINGS) {
            PortalPlayerSettings settings = PortalClientState.data().settings();
            button(x + 18, y + 31, fieldWidth, 18,
                toggleLabel("screen.riftgun.safety", settings.safetyCheckEnabled()), false,
                ignored -> updateSetting(0));
            button(x + 18, y + 55, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_deletion", settings.confirmDeletion()), false,
                ignored -> updateSetting(1));
            button(x + 18, y + 79, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_discard", settings.confirmDiscardedChanges()), false,
                ignored -> updateSetting(2));
            button(x + 18, y + 103, fieldWidth, 18,
                toggleLabel("screen.riftgun.animations", settings.animationsEnabled()), false,
                ignored -> updateSetting(3));
            button(x + 18, y + 127, fieldWidth, 18,
                toggleLabel("screen.riftgun.sounds", settings.soundsEnabled()), false,
                ignored -> updateSetting(4));
        }

        int actionY = y + box.height() - 27;
        if (modal.isConfirmation()) {
            button(x + 18, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.cancel", false, ignored -> cancelConfirmation());
            button(x + 24 + (box.width() - 42) / 2, actionY, (box.width() - 42) / 2, 19,
                modal == Modal.CONFIRM_UNSAFE ? "screen.riftgun.open_anyway" : "screen.riftgun.confirm",
                modal == Modal.CONFIRM_UNSAFE, ignored -> acceptConfirmation());
        } else if (modal == Modal.SETTINGS) {
            button(x + 18, actionY, fieldWidth, 19, "screen.riftgun.done", false,
                ignored -> closeModalNow());
        } else {
            button(x + 18, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.cancel", false, ignored -> requestCloseModal());
            button(x + 24 + (box.width() - 42) / 2, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.save", false, ignored -> submitModal());
        }
    }

    private void addGroupSelector(int x, int y, int width) {
        groupSelectorX = x;
        groupSelectorY = y;
        groupSelectorWidth = width;
        groupSelector = button(x, y, width - 22, 18,
            Component.translatable("screen.riftgun.group_value", groupName(formGroup)), false, ignored -> {});
        button(x + width - 20, y, 20, 18, Component.literal("▼"), false, ignored -> openGroupDropdown());
    }

    private EditBox addField(int x, int y, int width, String value, int maxLength,
                             Consumer<String> responder) {
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

        renderRows(graphics, mouseX, mouseY);
        renderDetails(graphics, mouseX, mouseY);
        graphics.flush();
        if (modal != Modal.NONE) {
            renderModal(graphics);
            graphics.flush();
        }
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
        }
        if (groupDropdownOpen) renderGroupDropdown(graphics, mouseX, mouseY);
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY) {
        hitRows.clear();
        List<Row> rows = buildRows();
        listContentHeight = rows.size() * ROW_HEIGHT;
        int maxScroll = listMaxScroll();
        if (ensureVisibleId != null) {
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).id().equals(ensureVisibleId)) {
                    int top = index * ROW_HEIGHT;
                    if (top < listScroll) listScroll = top;
                    if (top + ROW_HEIGHT > listScroll + listViewportHeight()) {
                        listScroll = top + ROW_HEIGHT - listViewportHeight();
                    }
                    break;
                }
            }
            ensureVisibleId = null;
        }
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        graphics.enableScissor(panelX + 1, listTop, panelX + listWidth - 1, listBottom);
        Set<UUID> liveIds = new java.util.HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            liveIds.add(row.id());
            float targetY = listTop - listScroll + index * ROW_HEIGHT;
            float currentY = animatedRowY.getOrDefault(row.id(), targetY);
            float renderedY = PortalClientState.data().settings().animationsEnabled()
                ? Mth.lerp(0.22F, currentY, targetY) : targetY;
            animatedRowY.put(row.id(), renderedY);
            int y = Math.round(renderedY);
            if (y + ROW_HEIGHT < listTop || y > listBottom) continue;
            boolean hover = mouseX >= panelX + 4 && mouseX < panelX + listWidth - 4
                && mouseY >= y && mouseY < y + ROW_HEIGHT;
            boolean focused = listFocused && row.id().equals(focusedRowId) && row.kind() == focusedRowKind;
            boolean selected = row.kind() == RowKind.DESTINATION
                && row.id().equals(PortalClientState.data().selectedDestinationId());
            hitRows.add(new Row(row.kind(), row.id(), y));
            if (selected) graphics.fill(panelX + 4, y, panelX + listWidth - 4, y + ROW_HEIGHT, 0x663F7180);
            else if (hover || focused) graphics.fill(panelX + 4, y, panelX + listWidth - 4,
                y + ROW_HEIGHT, 0x5530333A);
            if (focused) graphics.renderOutline(panelX + 4, y, listWidth - 8, ROW_HEIGHT, PortalTheme.BORDER_FOCUS);
            if (row.kind() == RowKind.GROUP) renderGroupRow(graphics, row.id(), y, hover, focused);
            else renderDestinationRow(graphics, row.id(), y, hover, focused, mouseX, mouseY);
        }
        animatedRowY.keySet().retainAll(liveIds);
        graphics.disableScissor();
        renderScrollbar(graphics, panelX + listWidth - 3, listTop, listBottom,
            listScroll, listContentHeight, listViewportHeight());
    }

    private void renderGroupRow(GuiGraphics graphics, UUID id, int y, boolean hover, boolean focused) {
        PortalPlayerData data = PortalClientState.data();
        boolean custom = !id.equals(PortalPlayerData.DEFAULT_GROUP_ID);
        boolean expanded = data.expandedGroups().contains(id);
        String name = custom ? data.group(id).map(DestinationGroup::name).orElse("?") : "Default";
        if (custom) drawDragHandle(graphics, panelX + 8, y + 5);
        drawDisclosure(graphics, panelX + 17, y + 6, expanded);
        int right = panelX + listWidth - 6;
        boolean actions = custom && (hover || focused);
        int reserved = actions ? 34 : 20;
        graphics.drawString(font, trim(name, listWidth - 34 - reserved), panelX + 28, y + 5,
            PortalTheme.TEXT, false);
        if (actions) {
            drawPencil(graphics, right - 27, y + 5, PortalTheme.ICE);
            drawCross(graphics, right - 11, y + 5, PortalTheme.DANGER);
        } else {
            long count = data.destinations().stream().filter(destination -> destination.groupId().equals(id)).count();
            String countText = Long.toString(count);
            graphics.drawString(font, countText, right - font.width(countText), y + 5,
                PortalTheme.TEXT_MUTED, false);
        }
    }

    private void renderDestinationRow(GuiGraphics graphics, UUID id, int y, boolean hover, boolean focused,
                                      int mouseX, int mouseY) {
        Destination destination = PortalClientState.data().destination(id).orElse(null);
        if (destination == null) return;
        int right = panelX + listWidth - 6;
        int deleteLeft = right - ROW_ACTION_SIZE;
        int starLeft = deleteLeft - ROW_ACTION_SIZE - 2;
        boolean target = id.equals(PortalClientState.data().selectedDestinationId());
        int nameX = panelX + 23;
        int nameWidth = starLeft - nameX - 12;
        boolean unsafe = PortalClientState.data().settings().safetyCheckEnabled()
            && PortalClientState.safety(id) != null && PortalClientState.safety(id) != 0;
        if (unsafe) {
            graphics.drawString(font, "!", starLeft - 10, y + 5, PortalTheme.WARNING, false);
            nameWidth -= 10;
        }
        String shown = trim(destination.name(), nameWidth);
        graphics.drawString(font, shown, nameX, y + 5, target ? PortalTheme.ICE : PortalTheme.TEXT_MUTED, false);
        drawStar(graphics, starLeft + 4, y + 5, destination.pinned());
        if (hover || focused) drawCross(graphics, deleteLeft + 3, y + 5, PortalTheme.DANGER);
        if (hover && mouseX >= nameX && mouseX < starLeft && font.width(destination.name()) > nameWidth) {
            graphics.renderTooltip(font, Component.literal(destination.name()), mouseX, mouseY);
        }
    }

    private void renderDetails(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelX + listWidth + 1;
        int right = panelX + panelWidth;
        int viewport = listViewportHeight();
        int x = left + 8;
        int y = listTop - detailScroll + 8;
        int contentStart = y;
        detailEditY = -1;
        graphics.enableScissor(left + 1, listTop, right - 1, listBottom);
        graphics.drawString(font, Component.translatable("screen.riftgun.details"), x, y,
            PortalTheme.TEXT_MUTED, false);
        y += 19;
        Destination destination = viewed();
        if (destination == null) {
            graphics.drawString(font, Component.translatable("screen.riftgun.empty_details"), x, y,
                PortalTheme.TEXT_MUTED, false);
            y += 22;
        } else {
            int textWidth = panelWidth - listWidth - 20;
            y = detailField(graphics, "screen.riftgun.name", destination.name(), x, y, textWidth);
            y = detailField(graphics, "screen.riftgun.group", groupName(destination.groupId()), x, y, textWidth);
            String dimension = friendlyDimension(destination.dimension().location().getPath());
            int dimensionY = y;
            y = detailField(graphics, "screen.riftgun.dimension", dimension, x, y, textWidth);
            if (mouseX >= x && mouseX < right - 6
                && mouseY >= dimensionY + 9 && mouseY < dimensionY + 23) {
                graphics.renderTooltip(font, Component.literal(destination.dimension().location().toString()), mouseX, mouseY);
            }
            y = detailField(graphics, "screen.riftgun.coordinates", String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
                destination.x(), destination.y(), destination.z()), x, y, textWidth);
            if (PortalClientState.data().settings().safetyCheckEnabled()) {
                if (PortalClientState.checkingSafety(destination.id())) {
                    graphics.drawString(font, Component.translatable("screen.riftgun.checking"), x, y,
                        PortalTheme.ICE, false);
                } else {
                    Integer flags = PortalClientState.safety(destination.id());
                    if (flags != null) graphics.drawString(font,
                        Component.translatable(flags == 0 ? "screen.riftgun.safe" : "screen.riftgun.unsafe"),
                        x, y, flags == 0 ? PortalTheme.ICE : PortalTheme.WARNING, false);
                }
                y += 22;
            }
            detailEditY = modal == Modal.NONE ? y : -1;
            if (modal == Modal.NONE) {
                graphics.fill(x, y, right - 8, y + 18, PortalTheme.PANEL_RAISED);
                graphics.renderOutline(x, y, right - x - 8, 18, PortalTheme.BORDER);
                graphics.drawCenteredString(font, Component.translatable("screen.riftgun.edit"),
                    (x + right - 8) / 2, y + 5, PortalTheme.TEXT);
            }
            y += 26;
        }
        detailContentHeight = Math.max(viewport, y - contentStart + 8);
        detailScroll = Mth.clamp(detailScroll, 0, detailMaxScroll());
        graphics.disableScissor();
        renderScrollbar(graphics, right - 3, listTop, listBottom,
            detailScroll, detailContentHeight, viewport);
    }

    private int detailField(GuiGraphics graphics, String labelKey, String value, int x, int y, int width) {
        label(graphics, labelKey, x, y);
        graphics.drawString(font, trim(value, width), x, y + 11, PortalTheme.TEXT, false);
        return y + DETAIL_LINE_HEIGHT;
    }

    private void renderModal(GuiGraphics graphics) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xB8101115);
        ModalBox box = modalBox();
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.PANEL_RAISED);
        graphics.renderOutline(box.x(), box.y(), box.width(), box.height(),
            modal == Modal.CONFIRM_UNSAFE ? PortalTheme.WARNING : PortalTheme.BORDER_FOCUS);
        graphics.drawString(font, Component.translatable(modal.titleKey), box.x() + 18, box.y() + 13,
            modal == Modal.CONFIRM_UNSAFE ? PortalTheme.WARNING : PortalTheme.TEXT, false);

        int x = box.x() + 18;
        int y = box.y();
        if (modal == Modal.CREATE_COORDINATE || modal == Modal.EDIT_DESTINATION) {
            label(graphics, "screen.riftgun.name", x, y + 29);
            label(graphics, "screen.riftgun.x", x, y + 68);
            label(graphics, "screen.riftgun.y", x + (box.width() - 26) / 2, y + 68);
            label(graphics, "screen.riftgun.z", x, y + 107);
            label(graphics, "screen.riftgun.yaw", x + (box.width() - 26) / 2, y + 107);
            label(graphics, "screen.riftgun.group", x, y + 146);
        } else if (modal == Modal.CREATE_CURRENT) {
            label(graphics, "screen.riftgun.name", x, y + 29);
            label(graphics, "screen.riftgun.coordinates", x, y + 68);
            if (minecraft != null && minecraft.player != null) {
                graphics.drawString(font, String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
                    minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()),
                    x, y + 81, PortalTheme.TEXT_MUTED, false);
            }
            label(graphics, "screen.riftgun.group", x, y + 99);
        } else if (modal == Modal.CREATE_GROUP || modal == Modal.RENAME_GROUP) {
            label(graphics, "screen.riftgun.name", x, y + 32);
        } else if (modal.isConfirmation()) {
            graphics.drawWordWrap(font, Component.translatable(modal.bodyKey), x, y + 35,
                box.width() - 36, PortalTheme.TEXT_MUTED);
        }
    }

    private void renderGroupDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        List<UUID> groups = orderedGroupIds();
        DropdownBox box = dropdownBox(groups.size());
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.renderOutline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        int visible = Math.min(7, groups.size());
        groupDropdownScroll = Mth.clamp(groupDropdownScroll, 0, Math.max(0, groups.size() - visible));
        for (int index = 0; index < visible; index++) {
            int groupIndex = groupDropdownScroll + index;
            UUID id = groups.get(groupIndex);
            int rowY = box.y() + 2 + index * ROW_HEIGHT;
            boolean hover = mouseX >= box.x() + 2 && mouseX < box.x() + box.width() - 2
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hover || groupIndex == groupDropdownIndex) {
                graphics.fill(box.x() + 2, rowY, box.x() + box.width() - 2, rowY + ROW_HEIGHT,
                    id.equals(formGroup) ? 0x773F7180 : 0x5530333A);
            }
            graphics.drawString(font, trim(groupName(id), box.width() - 12), box.x() + 6, rowY + 5,
                id.equals(formGroup) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
    }

    private List<Row> buildRows() {
        PortalPlayerData data = PortalClientState.data();
        List<Row> rows = new ArrayList<>();
        String normalizedQuery = query.strip().toLowerCase(Locale.ROOT);
        for (UUID groupId : orderedGroupIds()) {
            String name = groupName(groupId);
            List<Destination> destinations = data.destinations().stream()
                .filter(destination -> destination.groupId().equals(groupId))
                .filter(destination -> matches(destination, name, normalizedQuery))
                .sorted(destinationComparator(data.settings().sort())).toList();
            boolean groupMatch = normalizedQuery.isEmpty() || name.toLowerCase(Locale.ROOT).contains(normalizedQuery);
            if (!groupMatch && destinations.isEmpty()) continue;
            rows.add(new Row(RowKind.GROUP, groupId, 0));
            if (data.expandedGroups().contains(groupId) || !normalizedQuery.isEmpty()) {
                destinations.forEach(destination -> rows.add(new Row(RowKind.DESTINATION, destination.id(), 0)));
            }
        }
        return rows;
    }

    private List<UUID> orderedGroupIds() {
        List<UUID> ids = new ArrayList<>();
        ids.add(PortalPlayerData.DEFAULT_GROUP_ID);
        PortalClientState.data().groups().stream().sorted(Comparator.comparingInt(DestinationGroup::order))
            .map(DestinationGroup::id).forEach(ids::add);
        return ids;
    }

    private Comparator<Destination> destinationComparator(DestinationSort sort) {
        Comparator<Destination> pinned = Comparator.comparing(Destination::pinned).reversed();
        Comparator<Destination> secondary = switch (sort) {
            case RECENT -> Comparator.comparingLong(Destination::lastUsedAt).reversed();
            case NAME -> Comparator.comparing(value -> value.name().toLowerCase(Locale.ROOT));
            case CREATED -> Comparator.comparingLong(Destination::createdAt).reversed();
            case DISTANCE -> Comparator.comparingDouble(this::distanceSquared);
        };
        return pinned.thenComparing(secondary).thenComparing(Destination::id);
    }

    private double distanceSquared(Destination destination) {
        if (minecraft == null || minecraft.player == null
            || !minecraft.player.level().dimension().equals(destination.dimension())) return Double.POSITIVE_INFINITY;
        return minecraft.player.position().distanceToSqr(destination.position());
    }

    private boolean matches(Destination destination, String group, String normalized) {
        if (normalized.isEmpty() || group.toLowerCase(Locale.ROOT).contains(normalized)
            || destination.name().toLowerCase(Locale.ROOT).contains(normalized)) return true;
        return String.format(Locale.ROOT, "%s %s %s", destination.x(), destination.y(), destination.z())
            .contains(normalized);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (groupDropdownOpen && button == 0) {
            if (clickGroupDropdown(mouseX, mouseY)) return true;
            groupDropdownOpen = false;
        }
        if (modal != Modal.NONE) return super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && mouseX >= panelX + listWidth && mouseX < panelX + panelWidth
            && mouseY >= listTop && mouseY < listBottom) {
            if (clickDetail(mouseX, mouseY)) return true;
        }
        if (button == 0 && mouseX >= panelX && mouseX < panelX + listWidth
            && mouseY >= listTop && mouseY < listBottom) {
            for (Row row : hitRows) {
                if (mouseY < row.y() || mouseY >= row.y() + ROW_HEIGHT) continue;
                focusRow(row);
                int right = panelX + listWidth - 6;
                if (row.kind() == RowKind.DESTINATION) {
                    int deleteLeft = right - ROW_ACTION_SIZE;
                    int starLeft = deleteLeft - ROW_ACTION_SIZE - 2;
                    if (mouseX >= starLeft && mouseX < starLeft + ROW_ACTION_SIZE) {
                        togglePin(row.id());
                    } else if (mouseX >= deleteLeft) {
                        requestDeleteDestination(row.id());
                    } else {
                        selectDestination(row.id());
                    }
                    return true;
                }
                boolean custom = !row.id().equals(PortalPlayerData.DEFAULT_GROUP_ID);
                if (custom && mouseX >= right - 30 && mouseX < right - 16) {
                    openForm(Modal.RENAME_GROUP, row.id());
                } else if (custom && mouseX >= right - 14) {
                    requestDeleteGroup(row.id());
                } else if (custom && mouseX < panelX + 16) {
                    draggingGroup = row.id();
                    dragStartY = mouseY;
                } else {
                    toggleGroup(row.id());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickDetail(double mouseX, double mouseY) {
        int right = panelX + panelWidth;
        if (detailMaxScroll() > 0 && mouseX >= right - 6) {
            int thumbY = scrollbarThumbY(listTop, listBottom, detailScroll, detailContentHeight, listViewportHeight());
            int thumbHeight = scrollbarThumbHeight(listTop, listBottom, detailContentHeight, listViewportHeight());
            draggingDetailScrollbar = true;
            detailScrollbarGrab = mouseY >= thumbY && mouseY < thumbY + thumbHeight
                ? (int) mouseY - thumbY : thumbHeight / 2;
            updateDetailScrollbar(mouseY);
            return true;
        }
        int visibleEditY = detailEditY;
        if (viewed() != null && mouseY >= visibleEditY && mouseY < visibleEditY + 18) {
            openForm(Modal.EDIT_DESTINATION, viewedDestination);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingDetailScrollbar && button == 0) {
            updateDetailScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingDetailScrollbar = false;
        if (button == 0 && draggingGroup != null) {
            UUID moving = draggingGroup;
            draggingGroup = null;
            if (Math.abs(mouseY - dragStartY) >= 5.0) {
                Row target = hitRows.stream().filter(row -> row.kind() == RowKind.GROUP)
                    .min(Comparator.comparingDouble(row -> Math.abs(mouseY - (row.y() + ROW_HEIGHT / 2.0))))
                    .orElse(null);
                if (target != null) moveGroupTo(moving, groupOrderIndex(target.id()));
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (groupDropdownOpen) {
            int visible = Math.min(7, orderedGroupIds().size());
            groupDropdownScroll = Mth.clamp(groupDropdownScroll - (int) Math.signum(vertical), 0,
                Math.max(0, orderedGroupIds().size() - visible));
            return true;
        }
        if (modal == Modal.NONE && mouseY >= listTop && mouseY < listBottom) {
            if (mouseX < panelX + listWidth) {
                listScroll = Mth.clamp(listScroll - (int) Math.signum(vertical) * ROW_HEIGHT * 2,
                    0, listMaxScroll());
                return true;
            }
            if (mouseX < panelX + panelWidth) {
                detailScroll = Mth.clamp(detailScroll - (int) Math.signum(vertical) * ROW_HEIGHT,
                    0, detailMaxScroll());
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (groupDropdownOpen) return dropdownKeyPressed(keyCode);
        if (modal != Modal.NONE && modal.isDestinationForm() && groupSelector != null
            && groupSelector.isFocused() && (keyCode == 263 || keyCode == 262)) {
            shiftFormGroup(keyCode == 263 ? -1 : 1);
            return true;
        }
        if (keyCode == 256 && modal != Modal.NONE) {
            requestCloseModal();
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && modal != Modal.NONE) {
            if (modal.isConfirmation()) acceptConfirmation();
            else submitModal();
            return true;
        }
        if (modal == Modal.NONE && keyCode == 258) {
            if (searchBox != null && searchBox.isFocused() && !hasShiftDown()) {
                focusFirstRow();
                return true;
            }
            if (listFocused) {
                listFocused = false;
                setFocused(hasShiftDown() ? searchBox : firstCreateButton);
                return true;
            }
        }
        if (modal == Modal.NONE && listFocused && listKeyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean listKeyPressed(int keyCode) {
        if ((keyCode == 265 || keyCode == 264) && hasAltDown() && focusedRowKind == RowKind.GROUP
            && focusedRowId != null && !focusedRowId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
            moveGroup(focusedRowId, keyCode == 265 ? -1 : 1);
            return true;
        }
        if (keyCode == 265 || keyCode == 264) {
            moveRowFocus(keyCode == 265 ? -1 : 1);
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && focusedRowId != null) {
            if (focusedRowKind == RowKind.DESTINATION) selectDestination(focusedRowId);
            else toggleGroup(focusedRowId);
            return true;
        }
        if (keyCode == 80 && focusedRowKind == RowKind.DESTINATION && focusedRowId != null) {
            togglePin(focusedRowId);
            return true;
        }
        if (keyCode == 69 && focusedRowKind == RowKind.DESTINATION && focusedRowId != null) {
            openForm(Modal.EDIT_DESTINATION, focusedRowId);
            return true;
        }
        if (keyCode == 82 && focusedRowKind == RowKind.GROUP && focusedRowId != null
            && !focusedRowId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
            openForm(Modal.RENAME_GROUP, focusedRowId);
            return true;
        }
        if (keyCode == 261 && focusedRowId != null) {
            if (focusedRowKind == RowKind.DESTINATION) requestDeleteDestination(focusedRowId);
            else requestDeleteGroup(focusedRowId);
            return true;
        }
        return false;
    }

    private void selectDestination(UUID id) {
        if (PortalClientState.data().destination(id).isEmpty()) return;
        UUID previous = PortalClientState.data().selectedDestinationId();
        PortalClientState.data().selectedDestinationId(id);
        PortalClientState.data().lastViewedDestinationId(id);
        viewedDestination = id;
        selectedGroup = null;
        if (!id.equals(previous)) detailScroll = 0;
        PortalNetworking.sendRequest(PortalAction.SELECT_DESTINATION, tag -> tag.putUUID("Destination", id));
        requestSafetyIfNeeded(id, true);
    }

    private void requestSafetyIfNeeded(@Nullable UUID id, boolean force) {
        if (id == null || minecraft == null || minecraft.getConnection() == null
            || !PortalClientState.data().settings().safetyCheckEnabled()
            || PortalClientState.checkingSafety(id)) return;
        if (!force && PortalClientState.safety(id) != null) return;
        PortalClientState.beginSafetyCheck(id);
        PortalNetworking.sendRequest(PortalAction.CHECK_SAFETY, tag -> tag.putUUID("Destination", id));
    }

    private void generatePortal() {
        if (viewedDestination == null) return;
        PortalNetworking.sendRequest(PortalAction.OPEN_PORTAL, tag -> {
            tag.putUUID("Destination", viewedDestination);
            tag.putBoolean("ConfirmedUnsafe", false);
        });
    }

    private void togglePin(UUID id) {
        focusedRowId = id;
        focusedRowKind = RowKind.DESTINATION;
        ensureVisibleId = id;
        PortalNetworking.sendRequest(PortalAction.TOGGLE_PIN, tag -> tag.putUUID("Destination", id));
    }

    private void requestDeleteDestination(UUID id) {
        if (PortalClientState.data().settings().confirmDeletion()) {
            openForm(Modal.CONFIRM_DELETE_DESTINATION, id);
        } else {
            PortalNetworking.sendRequest(PortalAction.DELETE_DESTINATION, tag -> tag.putUUID("Destination", id));
        }
    }

    private void requestDeleteGroup(UUID id) {
        if (id.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return;
        if (PortalClientState.data().settings().confirmDeletion()) {
            openForm(Modal.CONFIRM_DELETE_GROUP, id);
        } else {
            PortalNetworking.sendRequest(PortalAction.DELETE_GROUP, tag -> tag.putUUID("Group", id));
        }
    }

    private void toggleGroup(UUID id) {
        selectedGroup = id;
        boolean expanded = !PortalClientState.data().expandedGroups().contains(id);
        PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
            tag.putUUID("Group", id);
            tag.putBoolean("Expanded", expanded);
        });
    }

    private void cycleSort() {
        PortalPlayerSettings current = PortalClientState.data().settings();
        sendSettings(new PortalPlayerSettings(current.safetyCheckEnabled(), current.confirmDeletion(),
            current.confirmDiscardedChanges(), current.animationsEnabled(), current.soundsEnabled(),
            current.sort().next()));
    }

    private void moveGroup(UUID group, int delta) {
        PortalNetworking.sendRequest(PortalAction.MOVE_GROUP, tag -> {
            tag.putUUID("Group", group);
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
        PortalPlayerSettings old = PortalClientState.data().settings();
        PortalPlayerSettings next = switch (setting) {
            case 0 -> new PortalPlayerSettings(!old.safetyCheckEnabled(), old.confirmDeletion(),
                old.confirmDiscardedChanges(), old.animationsEnabled(), old.soundsEnabled(), old.sort());
            case 1 -> new PortalPlayerSettings(old.safetyCheckEnabled(), !old.confirmDeletion(),
                old.confirmDiscardedChanges(), old.animationsEnabled(), old.soundsEnabled(), old.sort());
            case 2 -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                !old.confirmDiscardedChanges(), old.animationsEnabled(), old.soundsEnabled(), old.sort());
            case 3 -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                old.confirmDiscardedChanges(), !old.animationsEnabled(), old.soundsEnabled(), old.sort());
            default -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                old.confirmDiscardedChanges(), old.animationsEnabled(), !old.soundsEnabled(), old.sort());
        };
        PortalClientState.data().settings(next);
        if (!next.safetyCheckEnabled()) PortalClientState.clearSafety();
        sendSettings(next);
        rebuildWidgets();
        if (!old.safetyCheckEnabled() && next.safetyCheckEnabled()) requestSafetyIfNeeded(viewedDestination, true);
    }

    private void sendSettings(PortalPlayerSettings settings) {
        PortalNetworking.sendRequest(PortalAction.SET_SETTINGS, tag -> {
            tag.putBoolean("SafetyCheck", settings.safetyCheckEnabled());
            tag.putBoolean("ConfirmDeletion", settings.confirmDeletion());
            tag.putBoolean("ConfirmDiscardedChanges", settings.confirmDiscardedChanges());
            tag.putBoolean("Animations", settings.animationsEnabled());
            tag.putBoolean("Sounds", settings.soundsEnabled());
            tag.putString("Sort", settings.sort().name());
        });
    }

    private void openForm(Modal next, @Nullable UUID target) {
        modal = next;
        modalTarget = target;
        groupDropdownOpen = false;
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
            default -> { return; }
        }
        closeModalNow();
    }

    private void sendDestinationForm(PortalAction action, boolean coordinates) {
        PortalNetworking.sendRequest(action, tag -> {
            tag.putString("Name", formName);
            tag.putUUID("Group", formGroup);
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
        } else if (modal.hasInputs() && dirty
            && PortalClientState.data().settings().confirmDiscardedChanges()) {
            returnModal = modal;
            modal = Modal.CONFIRM_DIRTY;
            groupDropdownOpen = false;
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
            closeModalNow();
        } else if (modal == Modal.CONFIRM_DELETE_GROUP && modalTarget != null) {
            UUID id = modalTarget;
            PortalNetworking.sendRequest(PortalAction.DELETE_GROUP, tag -> tag.putUUID("Group", id));
            closeModalNow();
        } else if (modal == Modal.CONFIRM_UNSAFE && unsafeDestination != null) {
            UUID id = unsafeDestination;
            PortalNetworking.sendRequest(PortalAction.OPEN_PORTAL, tag -> {
                tag.putUUID("Destination", id);
                tag.putBoolean("ConfirmedUnsafe", true);
            });
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
        groupDropdownOpen = false;
        dirty = false;
        rebuildWidgets();
    }

    private void openGroupDropdown() {
        groupDropdownOpen = true;
        List<UUID> groups = orderedGroupIds();
        groupDropdownIndex = Math.max(0, groups.indexOf(formGroup));
        groupDropdownScroll = Mth.clamp(groupDropdownIndex - 3, 0, Math.max(0, groups.size() - 7));
        setFocused(groupSelector);
    }

    private void shiftFormGroup(int delta) {
        List<UUID> groups = orderedGroupIds();
        int index = Math.max(0, groups.indexOf(formGroup));
        int next = Mth.clamp(index + delta, 0, groups.size() - 1);
        if (next == index) return;
        selectFormGroup(groups.get(next));
    }

    private void selectFormGroup(UUID id) {
        formGroup = id;
        dirty = true;
        if (groupSelector != null) groupSelector.setMessage(
            Component.translatable("screen.riftgun.group_value", groupName(formGroup)));
    }

    private boolean clickGroupDropdown(double mouseX, double mouseY) {
        List<UUID> groups = orderedGroupIds();
        DropdownBox box = dropdownBox(groups.size());
        if (mouseX < box.x() || mouseX >= box.x() + box.width()
            || mouseY < box.y() || mouseY >= box.y() + box.height()) return false;
        int visible = Math.min(7, groups.size());
        if (mouseY < box.y() + 2 || mouseY >= box.y() + 2 + visible * ROW_HEIGHT) return true;
        int index = (int) ((mouseY - box.y() - 2) / ROW_HEIGHT) + groupDropdownScroll;
        if (index >= 0 && index < groups.size()) selectFormGroup(groups.get(index));
        groupDropdownOpen = false;
        return true;
    }

    private boolean dropdownKeyPressed(int keyCode) {
        List<UUID> groups = orderedGroupIds();
        if (keyCode == 256) {
            groupDropdownOpen = false;
            return true;
        }
        if (keyCode == 265 || keyCode == 264) {
            groupDropdownIndex = Mth.clamp(groupDropdownIndex + (keyCode == 265 ? -1 : 1), 0, groups.size() - 1);
            if (groupDropdownIndex < groupDropdownScroll) groupDropdownScroll = groupDropdownIndex;
            if (groupDropdownIndex >= groupDropdownScroll + 7) groupDropdownScroll = groupDropdownIndex - 6;
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            selectFormGroup(groups.get(groupDropdownIndex));
            groupDropdownOpen = false;
            return true;
        }
        return true;
    }

    private void focusRow(Row row) {
        listFocused = true;
        focusedRowId = row.id();
        focusedRowKind = row.kind();
        setFocused(null);
    }

    private void focusFirstRow() {
        List<Row> rows = buildRows();
        if (rows.isEmpty()) return;
        Row preferred = rows.stream().filter(row -> row.id().equals(PortalClientState.data().selectedDestinationId()))
            .findFirst().orElse(rows.getFirst());
        focusRow(preferred);
        ensureVisibleId = preferred.id();
    }

    private void moveRowFocus(int delta) {
        List<Row> rows = buildRows();
        if (rows.isEmpty()) return;
        int index = 0;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(focusedRowId) && rows.get(i).kind() == focusedRowKind) index = i;
        }
        Row next = rows.get(Mth.clamp(index + delta, 0, rows.size() - 1));
        focusedRowId = next.id();
        focusedRowKind = next.kind();
        ensureVisibleId = next.id();
    }

    private void updateDetailScrollbar(double mouseY) {
        int thumb = scrollbarThumbHeight(listTop, listBottom, detailContentHeight, listViewportHeight());
        int track = listViewportHeight() - thumb;
        if (track <= 0) return;
        double position = Mth.clamp(mouseY - listTop - detailScrollbarGrab, 0.0, track);
        detailScroll = (int) Math.round(position / track * detailMaxScroll());
    }

    private int listViewportHeight() {
        return Math.max(1, listBottom - listTop);
    }

    private int listMaxScroll() {
        return Math.max(0, listContentHeight - listViewportHeight());
    }

    private int detailMaxScroll() {
        return Math.max(0, detailContentHeight - listViewportHeight());
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int top, int bottom, int scroll,
                                 int contentHeight, int viewportHeight) {
        int max = Math.max(0, contentHeight - viewportHeight);
        if (max <= 0) return;
        int thumb = scrollbarThumbHeight(top, bottom, contentHeight, viewportHeight);
        int thumbY = scrollbarThumbY(top, bottom, scroll, contentHeight, viewportHeight);
        graphics.fill(x, thumbY, x + 2, thumbY + thumb, PortalTheme.ICE);
    }

    private static int scrollbarThumbHeight(int top, int bottom, int contentHeight, int viewportHeight) {
        int track = bottom - top - 4;
        return Math.max(12, track * viewportHeight / Math.max(viewportHeight, contentHeight));
    }

    private static int scrollbarThumbY(int top, int bottom, int scroll, int contentHeight, int viewportHeight) {
        int max = Math.max(1, contentHeight - viewportHeight);
        int thumb = scrollbarThumbHeight(top, bottom, contentHeight, viewportHeight);
        return top + 2 + (bottom - top - 4 - thumb) * scroll / max;
    }

    public void refreshFromServer(Set<UUID> invalidatedSafety) {
        UUID selected = PortalClientState.data().selectedDestinationId();
        if (selected != null && !selected.equals(viewedDestination)) {
            viewedDestination = selected;
            focusedRowId = selected;
            focusedRowKind = RowKind.DESTINATION;
            detailScroll = 0;
            ensureVisibleId = selected;
        } else if (viewedDestination != null && PortalClientState.data().destination(viewedDestination).isEmpty()) {
            viewedDestination = selected;
            detailScroll = 0;
        }
        if (selectedGroup != null && !selectedGroup.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && PortalClientState.data().group(selectedGroup).isEmpty()) selectedGroup = null;
        if (modal == Modal.NONE) rebuildWidgets();
        if (selected != null && invalidatedSafety.contains(selected)) requestSafetyIfNeeded(selected, true);
        else requestSafetyIfNeeded(selected, false);
    }

    public void onSafetyResult(UUID destinationId, int flags, boolean confirmation) {
        if (confirmation && flags != 0) {
            unsafeDestination = destinationId;
            openForm(Modal.CONFIRM_UNSAFE, destinationId);
        }
    }

    public void onPortalOpened() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    /** Used only by the opt-in visual QA harness. */
    public void openCoordinateEditorForQa() {
        openForm(Modal.CREATE_COORDINATE, null);
    }

    private @Nullable Destination viewed() {
        return viewedDestination == null ? null : PortalClientState.data().destination(viewedDestination).orElse(null);
    }

    private String groupName(UUID id) {
        if (id.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return "Default";
        return PortalClientState.data().group(id).map(DestinationGroup::name).orElse("Default");
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, PortalTheme.TEXT_MUTED, false);
    }

    private String trim(String value, int maxWidth) {
        if (maxWidth <= 8) return "";
        return font.width(value) <= maxWidth ? value
            : font.plainSubstrByWidth(value, maxWidth - 8) + "…";
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

    private ModalBox modalBox() {
        int desiredHeight = switch (modal) {
            case CREATE_COORDINATE, EDIT_DESTINATION -> 214;
            case CREATE_CURRENT -> 164;
            case SETTINGS -> 184;
            case CREATE_GROUP, RENAME_GROUP, CONFIRM_DELETE_DESTINATION, CONFIRM_DELETE_GROUP,
                 CONFIRM_DIRTY, CONFIRM_UNSAFE -> 112;
            case NONE -> 0;
        };
        int boxWidth = Math.min(340, panelWidth - 16);
        int boxHeight = Math.min(desiredHeight, height - 8);
        return new ModalBox((width - boxWidth) / 2, (height - boxHeight) / 2, boxWidth, boxHeight);
    }

    private DropdownBox dropdownBox(int groupCount) {
        int visible = Math.min(7, groupCount);
        int height = visible * ROW_HEIGHT + 4;
        int top = Math.max(panelY + 3, groupSelectorY - height - 2);
        return new DropdownBox(groupSelectorX, top, groupSelectorWidth, height);
    }

    private static void drawDisclosure(GuiGraphics graphics, int x, int y, boolean expanded) {
        int color = PortalTheme.ICE;
        if (expanded) {
            graphics.fill(x, y, x + 7, y + 1, color);
            graphics.fill(x + 1, y + 1, x + 6, y + 2, color);
            graphics.fill(x + 2, y + 2, x + 5, y + 3, color);
            graphics.fill(x + 3, y + 3, x + 4, y + 4, color);
        } else {
            graphics.fill(x, y, x + 1, y + 7, color);
            graphics.fill(x + 1, y + 1, x + 2, y + 6, color);
            graphics.fill(x + 2, y + 2, x + 3, y + 5, color);
            graphics.fill(x + 3, y + 3, x + 4, y + 4, color);
        }
    }

    private static void drawDragHandle(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            graphics.fill(x, y + row * 3, x + 5, y + row * 3 + 1, PortalTheme.TEXT_MUTED);
        }
    }

    private static void drawStar(GuiGraphics graphics, int x, int y, boolean filled) {
        int color = filled ? 0xFFFFD766 : 0xFFD4AA52;
        graphics.fill(x + 3, y, x + 4, y + 7, color);
        graphics.fill(x, y + 3, x + 7, y + 4, color);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, color);
        graphics.fill(x + 5, y + 1, x + 6, y + 2, color);
        graphics.fill(x + 1, y + 5, x + 2, y + 6, color);
        graphics.fill(x + 5, y + 5, x + 6, y + 6, color);
        if (filled) graphics.fill(x + 2, y + 2, x + 5, y + 5, color);
    }

    private static void drawCross(GuiGraphics graphics, int x, int y, int color) {
        for (int pixel = 0; pixel < 7; pixel++) {
            graphics.fill(x + pixel, y + pixel, x + pixel + 1, y + pixel + 1, color);
            graphics.fill(x + 6 - pixel, y + pixel, x + 7 - pixel, y + pixel + 1, color);
        }
    }

    private static void drawPencil(GuiGraphics graphics, int x, int y, int color) {
        for (int pixel = 0; pixel < 6; pixel++) {
            graphics.fill(x + pixel, y + 6 - pixel, x + pixel + 2, y + 8 - pixel, color);
        }
        graphics.fill(x, y + 7, x + 2, y + 9, PortalTheme.WARNING);
    }

    private enum RowKind { GROUP, DESTINATION }
    private record Row(RowKind kind, UUID id, int y) {}
    private record ModalBox(int x, int y, int width, int height) {}
    private record DropdownBox(int x, int y, int width, int height) {}

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
