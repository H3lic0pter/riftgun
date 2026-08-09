package dev.riftgun.client.screen;

import dev.riftgun.client.PlayerListState;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.render.PortalVisualPreferences;
import dev.riftgun.client.render.PortalVisualOption;
import dev.riftgun.client.render.PortalVisualOptions;
import dev.riftgun.client.render.PortalVisualRegistry;
import dev.riftgun.client.render.PortalVisualType;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.data.DestinationSafetyResult;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.data.PortalPlacementMode;
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
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
    private @Nullable UUID draggingDestination;
    private boolean destinationDragActive;
    private double dragStartX;
    private double dragStartY;
    private @Nullable UUID ensureVisibleId;
    private final List<Row> hitRows = new ArrayList<>();
    private final Map<UUID, Float> animatedRowY = new HashMap<>();

    private Modal modal = Modal.NONE;
    private Modal returnModal = Modal.NONE;
    private @Nullable UUID modalTarget;
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
    private boolean visualDropdownOpen;
    private int visualDropdownIndex;
    private int visualOptionsScroll;
    private int visualOptionsContentHeight;
    private boolean visualSettingsDirty;
    private long visualSettingsSaveDueTick = -1L;

    private @Nullable EditBox searchBox;
    private @Nullable ThemedButton firstCreateButton;
    private @Nullable ThemedButton coordinateButton;
    private @Nullable ThemedButton gunSettingsButton;
    private @Nullable ThemedButton moduleBayButton;
    private @Nullable ThemedButton closePortalsButton;
    private @Nullable ThemedButton gunSettingsBackButton;
    private @Nullable ThemedButton portalDurationSettingsButton;
    private @Nullable ThemedButton smartDistanceSettingsButton;
    private @Nullable ThemedButton surfaceRangeSettingsButton;
    private @Nullable ThemedButton entityTransitSettingsButton;
    private @Nullable ThemedButton apertureSettingsButton;
    private @Nullable ThemedButton fallGuardSettingsButton;
    private @Nullable ThemedButton moduleSettingBackButton;
    private @Nullable ThemedButton passiveTransitButton;
    private @Nullable ThemedButton hostileTransitButton;
    private @Nullable ThemedButton bossTransitButton;
    private @Nullable ThemedButton apertureToggleButton;
    private @Nullable ThemedButton fallGuardToggleButton;
    private @Nullable ThemedButton playerTargetButton;
    private @Nullable ThemedButton playerExcludeButton;
    private @Nullable ThemedButton playerTargetSettingsButton;
    private final List<EditBox> coordinateEditFields = new ArrayList<>();
    private @Nullable UUID selectedPlayerId;
    private boolean playerSectionExpanded = true;
    private boolean playerListRequested;
    private @Nullable ThemedButton groupSelector;
    private @Nullable ThemedButton groupDropdownButton;
    private @Nullable ThemedButton motionPredictionButton;
    private @Nullable ThemedButton placementModeButton;
    private @Nullable ThemedButton visualSettingsButton;
    private @Nullable ThemedButton visualBackButton;
    private @Nullable ThemedButton swirlAnimationBackButton;
    private @Nullable ThemedButton visualSelector;
    private @Nullable ThemedButton visualDropdownButton;
    private @Nullable ThemedButton visualAnimationSettingsButton;
    private @Nullable ThemedButton visualResetButton;
    private final List<VisualWidgetBinding> visualOptionWidgets = new ArrayList<>();
    private final List<VisualToggleBinding> visualToggleWidgets = new ArrayList<>();
    private @Nullable ThemedButton openPortalButton;
    private @Nullable ThemedButton bucketModeButton;
    private @Nullable ThemedButton clearFluidButton;
    private int groupSelectorX;
    private int groupSelectorY;
    private int groupSelectorWidth;
    private int visualSelectorX;
    private int visualSelectorY;
    private int visualSelectorWidth;
    private int fuelGaugeX;
    private int fuelGaugeY;
    private static final int FUEL_GAUGE_WIDTH = 42;
    private long clientTicks;
    private @Nullable UUID pendingSelection;
    private long selectionDueTick = -1L;

    public PortalConfigScreen() {
        super(Component.translatable("screen.riftgun.config"));
        PortalPlayerData data = PortalClientState.data();
        selectedPlayerId = data.selectedPlayerId();
        playerSectionExpanded = data.expandedGroups().contains(PortalPlayerData.PLAYER_SECTION_ID);
        if (selectedPlayerId != null) {
            viewedDestination = null;
            focusedRowId = selectedPlayerId;
            focusedRowKind = RowKind.PLAYER;
        } else {
            viewedDestination = data.selectedDestinationId() != null
                ? data.selectedDestinationId() : data.lastViewedDestinationId();
            focusedRowId = viewedDestination;
            focusedRowKind = viewedDestination == null ? null : RowKind.DESTINATION;
        }
    }

    @Override
    protected void init() {
        placementModeButton = null;
        coordinateButton = null;
        gunSettingsButton = null;
        moduleBayButton = null;
        closePortalsButton = null;
        gunSettingsBackButton = null;
        portalDurationSettingsButton = null;
        smartDistanceSettingsButton = null;
        surfaceRangeSettingsButton = null;
        entityTransitSettingsButton = null;
        apertureSettingsButton = null;
        fallGuardSettingsButton = null;
        moduleSettingBackButton = null;
        passiveTransitButton = null;
        hostileTransitButton = null;
        bossTransitButton = null;
        apertureToggleButton = null;
        fallGuardToggleButton = null;
        playerTargetButton = null;
        playerExcludeButton = null;
        playerTargetSettingsButton = null;
        coordinateEditFields.clear();
        groupDropdownButton = null;
        motionPredictionButton = null;
        visualSettingsButton = null;
        visualBackButton = null;
        swirlAnimationBackButton = null;
        visualSelector = null;
        visualDropdownButton = null;
        visualAnimationSettingsButton = null;
        visualResetButton = null;
        visualOptionWidgets.clear();
        visualToggleWidgets.clear();
        openPortalButton = null;
        bucketModeButton = null;
        clearFluidButton = null;
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
        coordinateButton = button(rightX + compactButtonWidth + 3, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.add_coordinate", false, ignored -> openForm(Modal.CREATE_COORDINATE, null));
        coordinateButton.visible = coordinateOverrideUnlocked();
        button(rightX + (compactButtonWidth + 3) * 2, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.add_group", false, ignored -> openForm(Modal.CREATE_GROUP, null));

        moduleBayButton = button(panelX + panelWidth - 29, panelY + 3, 19, 18,
            Component.empty(), false, ignored -> PortalNetworking.sendRequest(PortalAction.OPEN_MODULES));
        gunSettingsButton = button(panelX + panelWidth - 51, panelY + 3, 19, 18,
            Component.empty(), false, ignored -> openGunSettings());
        closePortalsButton = button(panelX + panelWidth - 73, panelY + 3, 19, 18,
            Component.empty(), false, ignored -> PortalNetworking.sendRequest(PortalAction.CLOSE_PORTALS));

        int footerY = panelY + panelHeight - 28;
        button(panelX + 10, footerY, 54, 19, "screen.riftgun.settings", false,
            ignored -> openForm(Modal.SETTINGS, null));
        int sortWidth = Math.max(12, Math.min(82, listWidth - 120));
        button(panelX + 67, footerY, sortWidth, 19,
            Component.translatable("screen.riftgun.sort_mode", Component.translatable(
                "screen.riftgun.sort." + PortalClientState.data().settings().sort().name().toLowerCase(Locale.ROOT))),
            false, ignored -> cycleSort());
        motionPredictionButton = button(panelX + listWidth - 50, footerY, 19, 19,
            Component.empty(), false, ignored -> cycleMotionPrediction());
        placementModeButton = button(panelX + listWidth - 28, footerY, 19, 19,
            Component.empty(), false, ignored -> cyclePlacementMode());
        fuelGaugeX = rightX;
        fuelGaugeY = footerY;
        bucketModeButton = button(rightX + FUEL_GAUGE_WIDTH + 3, footerY, 19, 19, Component.empty(), false,
            ignored -> PortalNetworking.sendRequest(PortalAction.TOGGLE_BUCKET_MODE));
        clearFluidButton = button(rightX + FUEL_GAUGE_WIDTH + 25, footerY, 19, 19, Component.empty(), false,
            ignored -> requestClearFluid());
        clearFluidButton.active = PortalClientState.gun().getInt("Amount") > 0;
        int portalButtonX = rightX + FUEL_GAUGE_WIDTH + 47;
        ThemedButton generate = button(portalButtonX, footerY,
            Math.max(24, rightX + available - portalButtonX), 19,
            "screen.riftgun.generate", true, ignored -> generatePortal());
        generate.active = viewed() != null;
        openPortalButton = generate;
        updateOpenPortalButton();

        if (playerSectionVisible() && !playerListRequested) {
            playerListRequested = true;
            requestPlayerListRefresh();
        }
    }

    @Override
    public void tick() {
        super.tick();
        clientTicks++;
        if (pendingSelection != null && clientTicks >= selectionDueTick) flushSelection();
        if (visualSettingsDirty && clientTicks >= visualSettingsSaveDueTick) flushVisualSettings();
    }

    @Override
    public void onClose() {
        flushSelection();
        flushVisualSettings();
        super.onClose();
    }

    @Override
    public void removed() {
        flushSelection();
        flushVisualSettings();
        super.removed();
    }

    private void initModal() {
        ModalBox box = modalBox();
        int x = box.x();
        int y = box.y();
        int fieldWidth = box.width() - 36;
        groupDropdownOpen = false;
        visualDropdownOpen = false;

        if (modal == Modal.CREATE_COORDINATE || modal == Modal.EDIT_DESTINATION) {
            addField(x + 18, y + 41, fieldWidth, formName, 48, value -> formName = value);
            int half = (fieldWidth - 10) / 2;
            boolean coordinatesEditable = modal == Modal.CREATE_COORDINATE || coordinateOverrideUnlocked();
            addCoordinateField(x + 18, y + 80, half, formX, value -> formX = value, coordinatesEditable);
            addCoordinateField(x + 28 + half, y + 80, half, formY, value -> formY = value, coordinatesEditable);
            addCoordinateField(x + 18, y + 119, half, formZ, value -> formZ = value, coordinatesEditable);
            addCoordinateField(x + 28 + half, y + 119, half, formYaw, value -> formYaw = value, coordinatesEditable);
            addGroupSelector(x + 18, y + 158, fieldWidth);
        } else if (modal == Modal.CREATE_CURRENT) {
            addField(x + 18, y + 41, fieldWidth, formName, 48, value -> formName = value);
            addGroupSelector(x + 18, y + 111, fieldWidth);
        } else if (modal == Modal.CREATE_GROUP || modal == Modal.RENAME_GROUP) {
            addField(x + 18, y + 44, fieldWidth, formName, 32, value -> formName = value);
        } else if (modal == Modal.SETTINGS) {
            PortalPlayerSettings settings = PortalClientState.data().settings();
            button(x + 18, y + 28, fieldWidth, 18,
                toggleLabel("screen.riftgun.safety", settings.safetyCheckEnabled()), false,
                ignored -> updateSetting(0));
            button(x + 18, y + 47, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_deletion", settings.confirmDeletion()), false,
                ignored -> updateSetting(1));
            button(x + 18, y + 66, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_discard", settings.confirmDiscardedChanges()), false,
                ignored -> updateSetting(2));
            button(x + 18, y + 85, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_clear_fluid", settings.confirmClearFluid()), false,
                ignored -> updateSetting(3));
            button(x + 18, y + 104, fieldWidth, 18,
                toggleLabel("screen.riftgun.animations", settings.animationsEnabled()), false,
                ignored -> updateSetting(4));
            button(x + 18, y + 123, fieldWidth, 18,
                toggleLabel("screen.riftgun.sounds", settings.soundsEnabled()), false,
                ignored -> updateSetting(5));
            visualSettingsButton = button(x + box.width() - 40, y + 8, 20, 18, Component.empty(), false,
                ignored -> openVisualSettings());
        } else if (modal == Modal.GUN_SETTINGS) {
            int buttonX = x + 18;
            portalDurationSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                ignored -> openGunSetting(Modal.PORTAL_DURATION_SETTINGS));
            buttonX += 31;
            smartDistanceSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                ignored -> openGunSetting(Modal.SMART_DISTANCE_SETTINGS));
            buttonX += 31;
            if (moduleCount("SURFACE_RANGE") > 0) {
                surfaceRangeSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(Modal.SURFACE_RANGE_SETTINGS));
                buttonX += 31;
            }
            if (hasEntityTransitModule()) {
                entityTransitSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(Modal.ENTITY_TRANSIT_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("PLAYER_TARGET") > 0) {
                playerTargetSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(Modal.PLAYER_TARGET_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("APERTURE_EXPANSION") > 0) {
                apertureSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(Modal.APERTURE_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("FALL_GUARD") > 0) {
                fallGuardSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(Modal.FALL_GUARD_SETTINGS));
            }
        } else if (modal == Modal.PORTAL_DURATION_SETTINGS) {
            boolean eternal = PortalClientState.gun().getBoolean("EternalDurationInstalled");
            int maximum = eternal ? 301 : Math.max(1, PortalClientState.gun().getInt("MaximumPortalDurationSeconds"));
            addRenderableWidget(new GunDistanceSlider(x + 18, y + 45, fieldWidth, 18,
                "PortalDuration", "screen.riftgun.portal_duration_value", 1, maximum,
                PortalClientState.gun().getInt("PortalDurationSeconds"), 1.0,
                eternal ? 301 : 0, "screen.riftgun.portal_duration_permanent"));
            int cooldownMaximum = Math.max(1, PortalClientState.gun().getInt("MaximumTransitCooldownTenths"));
            addRenderableWidget(new GunDistanceSlider(x + 18, y + 69, fieldWidth, 18,
                "TransitCooldown", "screen.riftgun.transit_cooldown_value", 0, cooldownMaximum,
                PortalClientState.gun().getInt("TransitCooldownTenths"), 10.0));
        } else if (modal == Modal.SMART_DISTANCE_SETTINGS) {
            addRenderableWidget(new GunDistanceSlider(x + 18, y + 45, fieldWidth, 18,
                "SmartDistance", "screen.riftgun.smart_distance_value", 1,
                Math.max(1, PortalClientState.gun().getInt("SurfaceRange")),
                PortalClientState.gun().getInt("SmartDistance")));
        } else if (modal == Modal.SURFACE_RANGE_SETTINGS) {
            int minimum = PortalClientState.moduleRules().baseSurfaceRange();
            int maximum = Math.max(minimum, PortalClientState.gun().getInt("MaximumSurfaceRange"));
            addRenderableWidget(new GunDistanceSlider(x + 18, y + 45, fieldWidth, 18,
                "SurfaceRange", "screen.riftgun.surface_range_value", minimum, maximum,
                PortalClientState.gun().getInt("SurfaceRange")));
        } else if (modal == Modal.ENTITY_TRANSIT_SETTINGS) {
            addEntityTransitButtons(x + 18, y + 45);
        } else if (modal == Modal.PLAYER_TARGET_SETTINGS) {
            int buttonX = x + 18;
            playerTargetButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean("PlayerTarget", "PlayerTargetEnabled"));
            buttonX += 31;
            playerExcludeButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                ignored -> cyclePlayerExclude());
        } else if (modal == Modal.APERTURE_SETTINGS) {
            apertureToggleButton = button(x + 18, y + 45, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean("ExpandedAperture", "ExpandedApertureEnabled"));
        } else if (modal == Modal.FALL_GUARD_SETTINGS) {
            fallGuardToggleButton = button(x + 18, y + 45, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean("FallGuard", "FallGuardEnabled"));
        } else if (modal == Modal.VISUAL_SETTINGS) {
            addVisualSelector(x + 18, y + 51, fieldWidth);
            if (!PortalVisualPreferences.selected().options().isEmpty()) {
                visualAnimationSettingsButton = button(x + fieldWidth - 2, y + 76, 20, 18,
                    Component.empty(), false, ignored -> openSwirlAnimationSettings());
            }
        } else if (modal == Modal.SWIRL_ANIMATION_SETTINGS) {
            addVisualOptionWidgets(box, fieldWidth);
        }

        int actionY = y + box.height() - 27;
        if (modal.isConfirmation()) {
            button(x + 18, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.cancel", false, ignored -> cancelConfirmation());
            button(x + 24 + (box.width() - 42) / 2, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.confirm", false, ignored -> acceptConfirmation());
        } else if (modal == Modal.SETTINGS) {
            button(x + 18, actionY, fieldWidth, 19, "screen.riftgun.done", false,
                ignored -> closeModalNow());
        } else if (modal == Modal.GUN_SETTINGS) {
            gunSettingsBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> closeModalNow());
        } else if (modal.isGunSettingPage()) {
            moduleSettingBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToGunSettings());
        } else if (modal == Modal.VISUAL_SETTINGS) {
            visualBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToSettings());
        } else if (modal == Modal.SWIRL_ANIMATION_SETTINGS) {
            swirlAnimationBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToVisualSettings());
        } else {
            button(x + 18, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.cancel", false, ignored -> requestCloseModal());
            button(x + 24 + (box.width() - 42) / 2, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.save", false, ignored -> submitModal());
        }
    }

    private void addCoordinateField(int x, int y, int width, String value,
                                    Consumer<String> responder, boolean editable) {
        EditBox field = addField(x, y, width, value, 64, responder);
        field.setEditable(editable);
        if (!editable) {
            field.setTextColorUneditable(PortalTheme.TEXT_MUTED);
            coordinateEditFields.add(field);
        }
    }

    private void addEntityTransitButtons(int x, int y) {
        int buttonX = x;
        if (moduleCount("PASSIVE_TRANSIT") > 0) {
            passiveTransitButton = button(buttonX, y, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean("PassiveTransit", "PassiveTransitEnabled"));
            buttonX += 31;
        }
        if (moduleCount("HOSTILE_TRANSIT") > 0) {
            hostileTransitButton = button(buttonX, y, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean("HostileTransit", "HostileTransitEnabled"));
            buttonX += 31;
        }
        if (moduleCount("BOSS_TRANSIT") > 0) {
            bossTransitButton = button(buttonX, y, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean("BossTransit", "BossTransitEnabled"));
        }
    }

    private void addGroupSelector(int x, int y, int width) {
        groupSelectorX = x;
        groupSelectorY = y;
        groupSelectorWidth = width;
        groupSelector = button(x, y, width - 22, 18,
            Component.translatable("screen.riftgun.group_value", groupName(formGroup)), false, ignored -> {});
        groupDropdownButton = button(x + width - 20, y, 20, 18, Component.empty(), false,
            ignored -> openGroupDropdown());
    }

    private void addVisualSelector(int x, int y, int width) {
        visualSelectorX = x;
        visualSelectorY = y;
        visualSelectorWidth = width;
        visualSelector = button(x, y, width - 22, 18, visualName(PortalVisualPreferences.selected()),
            false, ignored -> openVisualDropdown());
        visualDropdownButton = button(x + width - 20, y, 20, 18, Component.empty(), false,
            ignored -> openVisualDropdown());
    }

    private void addVisualOptionWidgets(ModalBox box, int width) {
        PortalVisualOptions options = PortalVisualPreferences.selected().options();
        if (options.isEmpty()) {
            visualOptionsScroll = 0;
            visualOptionsContentHeight = 0;
            return;
        }

        int x = box.x() + 18;
        visualOptionsContentHeight = 20 + options.entries().size() * 20;
        visualOptionsScroll = Mth.clamp(visualOptionsScroll, 0, visualOptionsMaxScroll(box));
        visualResetButton = addVisualWidget(new ThemedButton(x + width - 18, 0, 18, 16,
            Component.empty(), false, ignored -> resetVisualOptions()), 0);

        int offset = 20;
        for (PortalVisualOption option : options.entries()) {
            if (option instanceof PortalVisualOption.Toggle toggle) {
                ThemedButton widget = new ThemedButton(x, 0, width, 18,
                    visualToggleLabel(toggle), false, button -> {
                        toggle.toggle();
                        button.setMessage(visualToggleLabel(toggle));
                        refreshVisualOptionWidgets();
                        markVisualSettingsDirty();
                    });
                widget.active = toggle.active();
                addVisualWidget(widget, offset);
                visualToggleWidgets.add(new VisualToggleBinding(widget, toggle));
            } else if (option instanceof PortalVisualOption.Range range) {
                addVisualWidget(new VisualPeriodSlider(x, 0, width, 18, range), offset);
            }
            offset += 20;
        }
        layoutVisualOptionWidgets(box);
    }

    private <T extends AbstractWidget> T addVisualWidget(T widget, int contentOffset) {
        addWidget(widget);
        visualOptionWidgets.add(new VisualWidgetBinding(widget, contentOffset));
        return widget;
    }

    private void layoutVisualOptionWidgets(ModalBox box) {
        int top = visualOptionsTop(box);
        int bottom = visualOptionsBottom(box);
        for (VisualWidgetBinding binding : visualOptionWidgets) {
            AbstractWidget widget = binding.widget();
            int y = top + binding.contentOffset() - visualOptionsScroll;
            widget.setY(y);
            widget.visible = y >= top && y + widget.getHeight() <= bottom;
        }
    }

    private void refreshVisualOptionWidgets() {
        for (VisualToggleBinding binding : visualToggleWidgets) {
            binding.widget().setMessage(visualToggleLabel(binding.option()));
            binding.widget().active = binding.option().active();
        }
        for (VisualWidgetBinding binding : visualOptionWidgets) {
            if (binding.widget() instanceof VisualPeriodSlider slider) slider.refreshFromOption();
        }
    }

    private Component visualToggleLabel(PortalVisualOption.Toggle option) {
        return toggleLabel(option.labelKey(), option.value().getAsBoolean());
    }

    private void resetVisualOptions() {
        PortalVisualOptions options = PortalVisualPreferences.selected().options();
        if (options.isEmpty()) return;
        options.reset();
        refreshVisualOptionWidgets();
        markVisualSettingsDirty();
    }

    private void markVisualSettingsDirty() {
        visualSettingsDirty = true;
        visualSettingsSaveDueTick = clientTicks + 10L;
    }

    private void flushVisualSettings() {
        if (!visualSettingsDirty) return;
        PortalVisualPreferences.flush();
        visualSettingsDirty = false;
        visualSettingsSaveDueTick = -1L;
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

        int backgroundMouseX = modal == Modal.NONE ? mouseX : Integer.MIN_VALUE;
        int backgroundMouseY = modal == Modal.NONE ? mouseY : Integer.MIN_VALUE;
        renderRows(graphics, backgroundMouseX, backgroundMouseY);
        renderDetails(graphics, backgroundMouseX, backgroundMouseY);
        graphics.flush();
        if (modal != Modal.NONE) {
            renderModal(graphics);
            graphics.flush();
        }
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
            graphics.flush();
        }
        renderVisualOptionWidgets(graphics, mouseX, mouseY, partialTick);
        if (groupDropdownOpen) renderGroupDropdown(graphics, mouseX, mouseY);
        if (visualDropdownOpen) renderVisualDropdown(graphics, mouseX, mouseY);
        renderPlacementIcons(graphics, mouseX, mouseY);
        renderPlacementTooltips(graphics, mouseX, mouseY);
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
        Map<UUID, Integer> visibleGroupRows = new HashMap<>();
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
                && row.id().equals(PortalClientState.data().selectedDestinationId())
                || row.kind() == RowKind.PLAYER && row.id().equals(selectedPlayerId);
            hitRows.add(new Row(row.kind(), row.id(), y));
            if (row.kind() == RowKind.GROUP) visibleGroupRows.put(row.id(), y);
            if (selected) graphics.fill(panelX + 4, y, panelX + listWidth - 4, y + ROW_HEIGHT, 0x663F7180);
            else if (hover || focused) graphics.fill(panelX + 4, y, panelX + listWidth - 4,
                y + ROW_HEIGHT, 0x5530333A);
            if (focused) graphics.renderOutline(panelX + 4, y, listWidth - 8, ROW_HEIGHT, PortalTheme.BORDER_FOCUS);
            if (row.kind() == RowKind.GROUP) renderGroupRow(graphics, row.id(), y, hover, focused);
            else if (row.kind() == RowKind.PLAYER_SECTION) {
                renderPlayerSectionRow(graphics, row.id(), y, hover, focused);
            } else if (row.kind() == RowKind.PLAYER) {
                renderPlayerRow(graphics, row.id(), y, hover, focused, mouseX, mouseY);
            } else renderDestinationRow(graphics, row.id(), y, hover, focused, mouseX, mouseY);
            if (destinationDragActive && row.kind() == RowKind.DESTINATION
                && row.id().equals(draggingDestination)) {
                graphics.fill(panelX + 4, y, panelX + listWidth - 4, y + ROW_HEIGHT, 0x55101115);
                drawDestinationDragDot(graphics, panelX + 12, y + 8, PortalTheme.ICE);
            }
        }
        UUID dropGroup = destinationDragActive ? destinationDropGroupAt(mouseX, mouseY) : null;
        if (dropGroup != null && draggingDestination != null
            && !dropGroup.equals(destinationGroup(draggingDestination))) {
            Integer groupY = visibleGroupRows.get(dropGroup);
            if (groupY != null) {
                graphics.renderOutline(panelX + 4, groupY, listWidth - 8, ROW_HEIGHT,
                    PortalTheme.BORDER_FOCUS);
            }
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

    private void renderPlayerSectionRow(GuiGraphics graphics, UUID id, int y, boolean hover, boolean focused) {
        boolean expanded = playerSectionExpanded;
        drawDisclosure(graphics, panelX + 17, y + 6, expanded);
        int right = panelX + listWidth - 6;
        boolean actions = hover || focused;
        String title = "Player";
        if (actions) {
            int refreshRight = right - 11;
            drawPlayerRefreshIcon(graphics, refreshRight - 16, y + 5);
            graphics.drawString(font, trim(title, listWidth - 34 - 36), panelX + 28, y + 5,
                PortalTheme.ICE, false);
        } else {
            long count = sortedPlayers("").size();
            String countText = Long.toString(count);
            graphics.drawString(font, trim(title, listWidth - 34 - 20), panelX + 28, y + 5,
                PortalTheme.ICE, false);
            graphics.drawString(font, countText, right - font.width(countText), y + 5,
                PortalTheme.TEXT_MUTED, false);
        }
    }

    private void renderPlayerRow(GuiGraphics graphics, UUID id, int y, boolean hover, boolean focused,
                                 int mouseX, int mouseY) {
        PlayerListState.PlayerEntry entry = PlayerListState.player(id);
        if (entry == null) return;
        int right = panelX + listWidth - 6;
        int starLeft = right - ROW_ACTION_SIZE - 2;
        boolean selected = id.equals(selectedPlayerId);
        String name = entry.name();
        if (!entry.self()) {
            String localDimension = minecraft != null && minecraft.player != null
                ? minecraft.player.level().dimension().location().toString() : "";
            if (!entry.dimension().equals(localDimension)) {
                String dim = entry.dimension().contains(":")
                    ? entry.dimension().substring(entry.dimension().lastIndexOf(':') + 1) : entry.dimension();
                name = name + " (" + dim + ")";
            }
        }
        int nameX = panelX + 23;
        int nameWidth = starLeft - nameX - 12;
        String shown = trim(name, nameWidth);
        drawDestinationDragDot(graphics, panelX + 12, y + 8,
            hover || focused ? PortalTheme.TEXT_MUTED : 0xFF50535A);
        graphics.drawString(font, shown, nameX, y + 5,
            selected ? PortalTheme.ICE : (entry.self() ? PortalTheme.TEXT_MUTED : PortalTheme.TEXT), false);
        drawStar(graphics, starLeft + 4, y + 5, entry.pinned());
        if (hover && mouseX >= nameX && mouseX < starLeft && font.width(name) > nameWidth) {
            graphics.renderTooltip(font, Component.literal(name), mouseX, mouseY);
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
            && PortalClientState.data().safetyResult(id) == DestinationSafetyResult.UNSAFE;
        if (unsafe) {
            graphics.drawString(font, "!", starLeft - 10, y + 5, PortalTheme.WARNING, false);
            nameWidth -= 10;
        }
        String shown = trim(destination.name(), nameWidth);
        int dotColor = destinationDragActive && id.equals(draggingDestination)
            ? PortalTheme.ICE : hover || focused ? PortalTheme.TEXT_MUTED : 0xFF50535A;
        drawDestinationDragDot(graphics, panelX + 12, y + 8, dotColor);
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
        if (selectedPlayerId != null) {
            PlayerListState.PlayerEntry entry = PlayerListState.player(selectedPlayerId);
            if (entry == null) {
                selectedPlayerId = null;
            } else {
                int textWidth = panelWidth - listWidth - 20;
                y = detailField(graphics, "screen.riftgun.name", entry.name(), x, y, textWidth);
                y = detailField(graphics, "screen.riftgun.group", Component.translatable("screen.riftgun.player_group"),
                    x, y, textWidth);
                String dimension = friendlyDimension(entry.dimension().substring(
                    entry.dimension().lastIndexOf(':') + 1));
                int dimensionY = y;
                y = detailField(graphics, "screen.riftgun.dimension", dimension, x, y, textWidth);
                if (mouseX >= x && mouseX < right - 6
                    && mouseY >= dimensionY + 9 && mouseY < dimensionY + 23) {
                    graphics.renderTooltip(font, Component.literal(entry.dimension()), mouseX, mouseY);
                }
                if (minecraft != null && minecraft.player != null
                    && !entry.dimension().equals(minecraft.player.level().dimension().location().toString())) {
                    graphics.drawString(font, Component.translatable("screen.riftgun.cross_dimension_fuel_required"),
                        x, y, PortalTheme.WARNING, false);
                    y += 18;
                }
                y = detailField(graphics, "screen.riftgun.coordinates", "—", x, y, textWidth);
                if (entry.self()) {
                    graphics.drawString(font, Component.translatable("screen.riftgun.player_self"),
                        x, y, PortalTheme.TEXT_MUTED, false);
                    y += 18;
                }
                detailEditY = -1;
            }
        } else if (destination == null) {
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
            if (minecraft != null && minecraft.player != null
                && !minecraft.player.level().dimension().equals(destination.dimension())
                && !PortalClientState.gun().getBoolean("CrossDimension")) {
                graphics.drawString(font, Component.translatable("screen.riftgun.cross_dimension_fuel_required"),
                    x, y, PortalTheme.WARNING, false);
                y += 18;
            }
            y = detailField(graphics, "screen.riftgun.coordinates", String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
                destination.x(), destination.y(), destination.z()), x, y, textWidth);
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

    private int detailField(GuiGraphics graphics, String labelKey, Component value, int x, int y, int width) {
        label(graphics, labelKey, x, y);
        graphics.drawString(font, trim(value.getString(), width), x, y + 11, PortalTheme.TEXT, false);
        return y + DETAIL_LINE_HEIGHT;
    }

    private void renderModal(GuiGraphics graphics) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xB8101115);
        ModalBox box = modalBox();
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.PANEL_RAISED);
        graphics.renderOutline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        graphics.drawString(font, Component.translatable(modal.titleKey), box.x() + 18, box.y() + 13,
            PortalTheme.TEXT, false);

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
        } else if (modal == Modal.GUN_SETTINGS) {
            graphics.drawString(font, Component.translatable("screen.riftgun.gun_settings_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.PORTAL_DURATION_SETTINGS) {
            graphics.drawString(font, Component.translatable("screen.riftgun.portal_timing_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.SMART_DISTANCE_SETTINGS) {
            label(graphics, "screen.riftgun.smart_distance", x, y + 30);
            graphics.drawString(font, Component.translatable("screen.riftgun.maximum_surface_range",
                Math.max(1, PortalClientState.gun().getInt("SurfaceRange"))), x, y + 69,
                PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.SURFACE_RANGE_SETTINGS) {
            label(graphics, "screen.riftgun.surface_range", x, y + 30);
            graphics.drawString(font, Component.translatable("screen.riftgun.surface_range_modules",
                moduleCount("SURFACE_RANGE")), x, y + 69, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.ENTITY_TRANSIT_SETTINGS) {
            graphics.drawString(font, Component.translatable("screen.riftgun.entity_transit_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.PLAYER_TARGET_SETTINGS) {
            graphics.drawString(font, Component.translatable("screen.riftgun.player_target_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.APERTURE_SETTINGS) {
            graphics.drawString(font, Component.translatable("screen.riftgun.aperture_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.FALL_GUARD_SETTINGS) {
            graphics.drawString(font, Component.translatable("screen.riftgun.fall_guard_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (modal == Modal.VISUAL_SETTINGS) {
            label(graphics, "screen.riftgun.portal_visual", x, y + 34);
        } else if (modal == Modal.SWIRL_ANIMATION_SETTINGS) {
            renderVisualOptionsChrome(graphics, box);
        } else if (modal.isConfirmation()) {
            Component body = modal == Modal.CONFIRM_CLEAR_FLUID
                ? Component.translatable(modal.bodyKey, gunFluidName(), PortalClientState.gun().getInt("Amount"))
                : Component.translatable(modal.bodyKey);
            graphics.drawWordWrap(font, body, x, y + 35,
                box.width() - 36, PortalTheme.TEXT_MUTED);
        }
    }

    private void renderVisualOptionsChrome(GuiGraphics graphics, ModalBox box) {
        PortalVisualOptions options = PortalVisualPreferences.selected().options();
        if (options.isEmpty()) return;
        int top = visualOptionsTop(box);
        int bottom = visualOptionsBottom(box);
        int headerY = top + 4 - visualOptionsScroll;
        graphics.enableScissor(box.x() + 17, top, box.x() + box.width() - 17, bottom);
        graphics.drawString(font, Component.translatable(options.sectionTitleKey()), box.x() + 18,
            headerY, PortalTheme.TEXT_MUTED, false);
        graphics.disableScissor();
        renderScrollbar(graphics, box.x() + box.width() - 20, top, bottom,
            visualOptionsScroll, visualOptionsContentHeight, visualOptionsViewportHeight(box));
    }

    private void renderVisualOptionWidgets(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (modal != Modal.SWIRL_ANIMATION_SETTINGS || visualOptionWidgets.isEmpty()) return;
        ModalBox box = modalBox();
        int top = visualOptionsTop(box);
        int bottom = visualOptionsBottom(box);
        int effectiveMouseX = visualDropdownOpen ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = visualDropdownOpen ? Integer.MIN_VALUE : mouseY;
        graphics.enableScissor(box.x() + 17, top, box.x() + box.width() - 17, bottom);
        for (VisualWidgetBinding binding : visualOptionWidgets) {
            if (binding.widget().visible) {
                binding.widget().render(graphics, effectiveMouseX, effectiveMouseY, partialTick);
            }
        }
        graphics.disableScissor();
        graphics.flush();
    }

    private void renderPlacementIcons(GuiGraphics graphics, int mouseX, int mouseY) {
        if (modal.isDestinationForm() && groupDropdownButton != null) {
            drawDownIcon(graphics, groupDropdownButton.getX() + 6, groupDropdownButton.getY() + 7);
        }
        if (modal == Modal.NONE && placementModeButton != null) {
            renderMainModuleIcons(graphics);
            if (motionPredictionButton != null) {
                boolean active = PortalClientState.data().settings().predictionMode()
                    != dev.riftgun.data.PortalPredictionMode.OFF;
                drawPredictionIcon(graphics, motionPredictionButton.getX() + 5,
                    motionPredictionButton.getY() + 5, active ? PortalTheme.ICE : PortalTheme.TEXT_MUTED);
            }
            int x = placementModeButton.getX() + 5;
            int y = placementModeButton.getY() + 5;
            drawPlacementModeIcon(graphics, x, y, PortalClientState.data().settings().placementMode());
            renderGunControls(graphics, mouseX, mouseY);
        }
        if (modal == Modal.SETTINGS) {
            if (visualSettingsButton != null) {
                drawEyeIcon(graphics, visualSettingsButton.getX() + 5, visualSettingsButton.getY() + 5);
            }
        }
        if (modal == Modal.GUN_SETTINGS) {
            renderGunSettingEntries(graphics);
            renderBackButton(graphics, gunSettingsBackButton);
        }
        if (modal.isGunSettingPage()) {
            renderBackButton(graphics, moduleSettingBackButton);
            if (modal == Modal.ENTITY_TRANSIT_SETTINGS) {
                renderEntityTransitButtons(graphics);
            } else if (modal == Modal.PLAYER_TARGET_SETTINGS) {
                renderPlayerTargetButtons(graphics);
            } else if (modal == Modal.APERTURE_SETTINGS && apertureToggleButton != null) {
                boolean enabled = PortalClientState.gun().getBoolean("ExpandedApertureEnabled");
                drawApertureIcon(graphics, apertureToggleButton.getX() + 7,
                    apertureToggleButton.getY() + 7, enabled);
            } else if (modal == Modal.FALL_GUARD_SETTINGS && fallGuardToggleButton != null) {
                boolean enabled = PortalClientState.gun().getBoolean("FallGuardEnabled");
                drawFallGuardIcon(graphics, fallGuardToggleButton.getX() + 7,
                    fallGuardToggleButton.getY() + 7, enabled);
            }
        }
        if (modal == Modal.VISUAL_SETTINGS) {
            if (visualBackButton != null) {
                drawBackIcon(graphics, visualBackButton.getX() + 7, visualBackButton.getY() + 6);
            }
            if (visualDropdownButton != null) {
                drawDownIcon(graphics, visualDropdownButton.getX() + 6, visualDropdownButton.getY() + 7);
            }
            if (!visualDropdownOpen && visualAnimationSettingsButton != null) {
                drawSwirlIcon(graphics, visualAnimationSettingsButton.getX() + 5,
                    visualAnimationSettingsButton.getY() + 5, PortalTheme.ICE);
            }
        }
        if (modal == Modal.SWIRL_ANIMATION_SETTINGS) {
            if (swirlAnimationBackButton != null) {
                drawBackIcon(graphics, swirlAnimationBackButton.getX() + 7,
                    swirlAnimationBackButton.getY() + 6);
            }
            if (visualResetButton != null && visualResetButton.visible) {
                drawResetIcon(graphics, visualResetButton.getX() + 5, visualResetButton.getY() + 4,
                    visualResetButton.active ? PortalTheme.ICE : PortalTheme.TEXT_MUTED);
            }
        }
    }

    /** Rendered last so icons never cover tooltip text. */
    private void renderPlacementTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (modal.isDestinationForm() && groupDropdownButton != null) {
            // no tooltip
        }
        if (modal == Modal.NONE && placementModeButton != null) {
            if (gunSettingsButton != null && gunSettingsButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.configure_gun"), mouseX, mouseY);
            }
            if (moduleBayButton != null && moduleBayButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.open_modules"), mouseX, mouseY);
            }
            if (closePortalsButton != null && closePortalsButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.close_portals"), mouseX, mouseY);
            }
            if (motionPredictionButton != null) {
                dev.riftgun.data.PortalPredictionMode mode =
                    PortalClientState.data().settings().predictionMode();
                if (motionPredictionButton.isHovered()) {
                    String modeKey = "screen.riftgun.prediction."
                        + mode.name().toLowerCase(Locale.ROOT);
                    graphics.renderComponentTooltip(font, List.of(
                        Component.translatable("screen.riftgun.motion_prediction_tooltip",
                            Component.translatable(modeKey)),
                        Component.translatable(modeKey + ".description")
                    ), mouseX, mouseY);
                }
            }
            if (placementModeButton.isHovered()) {
                graphics.renderTooltip(font, Component.translatable("screen.riftgun.placement_mode_tooltip",
                    Component.translatable("screen.riftgun.placement_mode."
                        + PortalClientState.data().settings().placementMode().name().toLowerCase(Locale.ROOT))),
                    mouseX, mouseY);
            }
            if (openPortalButton != null && openPortalButton.isHovered()) {
                graphics.renderTooltip(font, Component.translatable("screen.riftgun.open_front_tooltip"),
                    mouseX, mouseY);
            }
            renderGunControlTooltips(graphics, mouseX, mouseY);
        }
        if (modal == Modal.SETTINGS) {
            if (visualSettingsButton != null && visualSettingsButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.visual_settings"), mouseX, mouseY);
            }
        }
        if (modal == Modal.GUN_SETTINGS) {
            renderGunSettingTooltips(graphics, mouseX, mouseY);
            renderBackButtonTooltip(graphics, gunSettingsBackButton, mouseX, mouseY,
                "screen.riftgun.back");
        }
        if (modal.isGunSettingPage()) {
            renderBackButtonTooltip(graphics, moduleSettingBackButton, mouseX, mouseY,
                "screen.riftgun.back_to_gun_settings");
            if (modal == Modal.ENTITY_TRANSIT_SETTINGS) {
                renderEntityTransitTooltips(graphics, mouseX, mouseY);
            } else if (modal == Modal.PLAYER_TARGET_SETTINGS) {
                renderPlayerTargetTooltips(graphics, mouseX, mouseY);
            } else if (modal == Modal.APERTURE_SETTINGS && apertureToggleButton != null) {
                boolean enabled = PortalClientState.gun().getBoolean("ExpandedApertureEnabled");
                entityTooltip(graphics, apertureToggleButton,
                    "screen.riftgun.aperture", enabled, mouseX, mouseY);
            } else if (modal == Modal.FALL_GUARD_SETTINGS && fallGuardToggleButton != null) {
                boolean enabled = PortalClientState.gun().getBoolean("FallGuardEnabled");
                entityTooltip(graphics, fallGuardToggleButton,
                    "screen.riftgun.fall_guard", enabled, mouseX, mouseY);
            }
        }
        if (modal == Modal.VISUAL_SETTINGS) {
            if (visualBackButton != null && visualBackButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.back_to_settings"), mouseX, mouseY);
            }
            if (!visualDropdownOpen && visualSelector != null && visualSelector.isHovered()) {
                graphics.renderTooltip(font, Component.translatable(
                    PortalVisualPreferences.selected().descriptionKey()), mouseX, mouseY);
            }
            if (!visualDropdownOpen && visualAnimationSettingsButton != null
                && visualAnimationSettingsButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.visual.swirl_animation_settings"), mouseX, mouseY);
            }
        }
        if (modal == Modal.SWIRL_ANIMATION_SETTINGS) {
            if (swirlAnimationBackButton != null && swirlAnimationBackButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable("screen.riftgun.visual.back_to_visuals"), mouseX, mouseY);
            }
            if (visualResetButton != null && visualResetButton.visible && visualResetButton.isHovered()) {
                graphics.renderTooltip(font,
                    Component.translatable(PortalVisualPreferences.selected().options().resetTooltipKey()),
                    mouseX, mouseY);
            }
        }
        if (modal == Modal.EDIT_DESTINATION && !coordinateOverrideUnlocked()) {
            for (EditBox field : coordinateEditFields) {
                if (field.isHovered()) {
                    graphics.renderTooltip(font, Component.translatable(
                        "screen.riftgun.coordinate_read_only"), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void renderMainModuleIcons(GuiGraphics graphics) {
        if (gunSettingsButton != null) {
            drawGunSettingsIcon(graphics, gunSettingsButton.getX() + 5, gunSettingsButton.getY() + 4,
                PortalTheme.ICE);
        }
        if (moduleBayButton != null) {
            drawModuleBayIcon(graphics, moduleBayButton.getX() + 4, moduleBayButton.getY() + 4,
                PortalTheme.WARNING);
        }
        if (closePortalsButton != null) {
            drawPortalCloseIcon(graphics, closePortalsButton.getX() + 4, closePortalsButton.getY() + 4);
        }
    }

    private void renderGunSettingEntries(GuiGraphics graphics) {
        if (portalDurationSettingsButton != null) {
            drawPortalDurationIcon(graphics, portalDurationSettingsButton.getX() + 7,
                portalDurationSettingsButton.getY() + 7);
        }
        if (smartDistanceSettingsButton != null) {
            drawSmartDistanceIcon(graphics, smartDistanceSettingsButton.getX() + 7,
                smartDistanceSettingsButton.getY() + 7, PortalTheme.ICE);
        }
        if (surfaceRangeSettingsButton != null) {
            drawSurfaceRangeIcon(graphics, surfaceRangeSettingsButton.getX() + 7,
                surfaceRangeSettingsButton.getY() + 8, PortalTheme.WARNING);
        }
        if (entityTransitSettingsButton != null) {
            drawEntityAccessIcon(graphics, entityTransitSettingsButton.getX() + 7,
                entityTransitSettingsButton.getY() + 7, PortalTheme.PORTAL);
        }
        if (playerTargetSettingsButton != null) {
            drawPlayerTargetIcon(graphics, playerTargetSettingsButton.getX() + 6,
                playerTargetSettingsButton.getY() + 6, PortalTheme.ICE);
        }
        if (apertureSettingsButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("ExpandedApertureEnabled");
            drawApertureIcon(graphics, apertureSettingsButton.getX() + 7,
                apertureSettingsButton.getY() + 7, enabled);
        }
        if (fallGuardSettingsButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("FallGuardEnabled");
            drawFallGuardIcon(graphics, fallGuardSettingsButton.getX() + 7,
                fallGuardSettingsButton.getY() + 7, enabled);
        }
    }

    private void renderGunSettingTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        settingTooltip(graphics, portalDurationSettingsButton,
            "screen.riftgun.portal_duration", mouseX, mouseY);
        settingTooltip(graphics, smartDistanceSettingsButton,
            "screen.riftgun.smart_distance", mouseX, mouseY);
        settingTooltip(graphics, surfaceRangeSettingsButton,
            "screen.riftgun.surface_range", mouseX, mouseY);
        settingTooltip(graphics, entityTransitSettingsButton,
            "screen.riftgun.entity_transit", mouseX, mouseY);
        settingTooltip(graphics, playerTargetSettingsButton,
            "screen.riftgun.player_target", mouseX, mouseY);
        settingTooltip(graphics, apertureSettingsButton,
            "screen.riftgun.aperture", mouseX, mouseY);
        settingTooltip(graphics, fallGuardSettingsButton,
            "screen.riftgun.fall_guard", mouseX, mouseY);
    }

    private void renderEntityTransitButtons(GuiGraphics graphics) {
        if (passiveTransitButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("PassiveTransitEnabled");
            drawPigIcon(graphics, passiveTransitButton.getX() + 6, passiveTransitButton.getY() + 7,
                enabled ? 0xFFA7D79B : PortalTheme.TEXT_MUTED);
        }
        if (hostileTransitButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("HostileTransitEnabled");
            drawZombieIcon(graphics, hostileTransitButton.getX() + 7, hostileTransitButton.getY() + 7,
                enabled ? 0xFFD98264 : PortalTheme.TEXT_MUTED);
        }
        if (bossTransitButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("BossTransitEnabled");
            drawDragonIcon(graphics, bossTransitButton.getX() + 6, bossTransitButton.getY() + 7,
                enabled ? 0xFFB38AD8 : PortalTheme.TEXT_MUTED);
        }
    }

    private void renderEntityTransitTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (passiveTransitButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("PassiveTransitEnabled");
            entityTooltip(graphics, passiveTransitButton, "screen.riftgun.passive_transit", enabled, mouseX, mouseY);
        }
        if (hostileTransitButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("HostileTransitEnabled");
            entityTooltip(graphics, hostileTransitButton, "screen.riftgun.hostile_transit", enabled, mouseX, mouseY);
        }
        if (bossTransitButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("BossTransitEnabled");
            entityTooltip(graphics, bossTransitButton, "screen.riftgun.boss_transit", enabled, mouseX, mouseY);
        }
    }

    private void renderPlayerTargetButtons(GuiGraphics graphics) {
        if (playerTargetButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("PlayerTargetEnabled");
            drawPlayerTargetIcon(graphics, playerTargetButton.getX() + 7, playerTargetButton.getY() + 7,
                enabled ? 0xFF5CC8D9 : PortalTheme.TEXT_MUTED);
        }
        if (playerExcludeButton != null) {
            int mode = PortalClientState.gun().getInt("PlayerExcludeMode");
            drawPlayerExcludeIcon(graphics, playerExcludeButton.getX() + 7, playerExcludeButton.getY() + 7,
                mode == 0 ? PortalTheme.TEXT_MUTED : 0xFF5CC8D9);
        }
    }

    private void renderPlayerTargetTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (playerTargetButton != null) {
            boolean enabled = PortalClientState.gun().getBoolean("PlayerTargetEnabled");
            entityTooltip(graphics, playerTargetButton, "screen.riftgun.player_target", enabled, mouseX, mouseY);
        }
        if (playerExcludeButton != null) {
            int mode = PortalClientState.gun().getInt("PlayerExcludeMode");
            String key = switch (mode) {
                case 0 -> "screen.riftgun.player_exclude_off";
                case 1 -> "screen.riftgun.player_exclude_entry_exit";
                default -> "screen.riftgun.player_exclude_exit_only";
            };
            if (playerExcludeButton.isHovered()) {
                graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
            }
        }
    }

    private void settingTooltip(GuiGraphics graphics, @Nullable ThemedButton button,
                                String key, int mouseX, int mouseY) {
        if (button != null && button.isHovered()) {
            graphics.renderTooltip(font, Component.translatable(key), mouseX, mouseY);
        }
    }

    private void entityTooltip(GuiGraphics graphics, @Nullable ThemedButton button, String key,
                               boolean enabled, int mouseX, int mouseY) {
        if (button != null && button.isHovered()) {
            graphics.renderTooltip(font, Component.translatable(key).append(": ").append(
                Component.translatable(enabled ? "screen.riftgun.on" : "screen.riftgun.off")), mouseX, mouseY);
        }
    }

    private void renderBackButton(GuiGraphics graphics, @Nullable ThemedButton button) {
        if (button == null) return;
        drawBackIcon(graphics, button.getX() + 7, button.getY() + 6);
    }

    private void renderBackButtonTooltip(GuiGraphics graphics, @Nullable ThemedButton button,
                                         int mouseX, int mouseY, String tooltipKey) {
        if (button != null && button.isHovered()) graphics.renderTooltip(font,
            Component.translatable(tooltipKey), mouseX, mouseY);
    }

    private void renderGunControls(GuiGraphics graphics, int mouseX, int mouseY) {
        renderFuelGauge(graphics, mouseX, mouseY);
        if (bucketModeButton != null) {
            int x = bucketModeButton.getX() + 4;
            int y = bucketModeButton.getY() + 4;
            int iconColor = PortalClientState.gun().getBoolean("BucketMode")
                ? PortalTheme.ICE : PortalTheme.TEXT_MUTED;
            drawBucketIcon(graphics, x, y, iconColor);
        }
        if (clearFluidButton != null) {
            drawDrainIcon(graphics, clearFluidButton.getX() + 5, clearFluidButton.getY() + 4,
                clearFluidButton.active ? PortalTheme.DANGER : PortalTheme.TEXT_MUTED);
        }
    }

    private void renderGunControlTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        renderFuelGaugeTooltip(graphics, mouseX, mouseY);
        if (bucketModeButton != null && bucketModeButton.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("screen.riftgun.bucket_mode_simple",
                Component.translatable(PortalClientState.gun().getBoolean("BucketMode")
                    ? "screen.riftgun.on" : "screen.riftgun.off")), mouseX, mouseY);
        }
        if (clearFluidButton != null && clearFluidButton.isHovered()) {
            graphics.renderTooltip(font,
                Component.translatable("screen.riftgun.clear_fluid_tooltip"), mouseX, mouseY);
        }
    }

    private void renderFuelGauge(GuiGraphics graphics, int mouseX, int mouseY) {
        int amount = PortalClientState.gun().getInt("Amount");
        int capacity = Math.max(1, PortalClientState.gun().getInt("Capacity"));
        boolean overfilled = amount > capacity;
        int rgb = PortalClientState.gun().getInt("Rgb");
        int fluidColor = 0xFF000000 | (rgb == 0 ? 0x34363D : rgb);
        graphics.fill(fuelGaugeX, fuelGaugeY, fuelGaugeX + FUEL_GAUGE_WIDTH, fuelGaugeY + 19,
            PortalTheme.FIELD);
        int fillWidth = Mth.clamp((int) Math.ceil(Math.min(1.0, amount / (double) capacity)
            * (FUEL_GAUGE_WIDTH - 4)), 0, FUEL_GAUGE_WIDTH - 4);
        graphics.fill(fuelGaugeX + 2, fuelGaugeY + 14,
            fuelGaugeX + FUEL_GAUGE_WIDTH - 2, fuelGaugeY + 17, 0xFF292B31);
        if (fillWidth > 0) graphics.fill(fuelGaugeX + 2, fuelGaugeY + 14,
            fuelGaugeX + 2 + fillWidth, fuelGaugeY + 17, fluidColor);
        graphics.renderOutline(fuelGaugeX, fuelGaugeY, FUEL_GAUGE_WIDTH, 19,
            overfilled ? 0xFFFFAA00 : PortalTheme.BORDER);
        String shortAmount = shortFluidAmount(amount);
        graphics.drawCenteredString(font, shortAmount, fuelGaugeX + FUEL_GAUGE_WIDTH / 2,
            fuelGaugeY + 3, amount == 0 ? PortalTheme.TEXT_MUTED : PortalTheme.TEXT);
    }

    private void renderFuelGaugeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int amount = PortalClientState.gun().getInt("Amount");
        int capacity = Math.max(1, PortalClientState.gun().getInt("Capacity"));
        boolean overfilled = amount > capacity;
        if (mouseX >= fuelGaugeX && mouseX < fuelGaugeX + FUEL_GAUGE_WIDTH
            && mouseY >= fuelGaugeY && mouseY < fuelGaugeY + 19) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(gunFluidName());
            tooltip.add(Component.literal(amount + "/" + capacity + " mB"));
            if (overfilled) tooltip.add(Component.translatable("screen.riftgun.overfilled")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    private static String shortFluidAmount(int amount) {
        if (amount < 1_000) return Integer.toString(amount);
        if (amount % 1_000 == 0) return (amount / 1_000) + "k";
        return String.format(Locale.ROOT, "%.1fk", amount / 1_000.0);
    }

    private static void drawBucketIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.BUCKET_OFF : PortalGuiSprites.BUCKET_ON, x - 3, y - 3);
    }

    private static void drawDrainIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.DRAIN_OFF : PortalGuiSprites.DRAIN_ON, x - 3, y - 3);
    }

    private static void drawPlacementModeIcon(GuiGraphics graphics, int x, int y, PortalPlacementMode mode) {
        ResourceLocation sprite = switch (mode) {
            case SMART -> PortalGuiSprites.PLACEMENT_SMART;
            case FRONT -> PortalGuiSprites.PLACEMENT_FRONT;
            case SURFACE -> PortalGuiSprites.PLACEMENT_SURFACE;
        };
        int offsetX = mode == PortalPlacementMode.FRONT ? 4 : 3;
        PortalGuiSprites.draw(graphics, sprite, x - offsetX, y - 3);
    }

    private static void drawPredictionIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PREDICTION_OFF : PortalGuiSprites.PREDICTION_ON, x - 2, y - 4);
    }

    private static void drawGunSettingsIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.CONFIGURE_GUN, x - 3, y - 2);
    }

    private static void drawModuleBayIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.MODULE_BAY, x - 2, y - 2);
    }

    private static void drawPortalCloseIcon(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PORTAL_CLOSE, x - 2, y - 2);
    }

    private static void drawSmartDistanceIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SMART_DISTANCE, x - 2, y - 2);
    }

    private static void drawPortalDurationIcon(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PORTAL_DURATION, x - 2, y - 2);
    }

    private static void drawApertureIcon(GuiGraphics graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.APERTURE_ON : PortalGuiSprites.APERTURE_OFF, x - 2, y - 2);
    }

    private static void drawFallGuardIcon(GuiGraphics graphics, int x, int y, boolean enabled) {
        PortalGuiSprites.draw(graphics, enabled
            ? PortalGuiSprites.FALL_GUARD_ON : PortalGuiSprites.FALL_GUARD_OFF, x - 2, y - 2);
    }

    private static void drawSurfaceRangeIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SURFACE_RANGE, x - 3, y - 2);
    }

    private static void drawEntityAccessIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.ENTITY_ACCESS, x - 2, y - 2);
    }

    private static void drawPigIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PASSIVE_TRANSIT_OFF : PortalGuiSprites.PASSIVE_TRANSIT_ON, x - 1, y - 2);
    }

    private static void drawZombieIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.HOSTILE_TRANSIT_OFF : PortalGuiSprites.HOSTILE_TRANSIT_ON, x - 2, y - 2);
    }

    private static void drawDragonIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.BOSS_TRANSIT_OFF : PortalGuiSprites.BOSS_TRANSIT_ON, x - 1, y - 2);
    }

    private static void drawPlayerTargetIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PLAYER_TARGET_OFF : PortalGuiSprites.PLAYER_TARGET_ON, x - 2, y - 2);
    }

    private static void drawPlayerExcludeIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.PLAYER_EXCLUDE_OFF : PortalGuiSprites.PLAYER_EXCLUDE_ON, x - 2, y - 2);
    }

    private static void drawPlayerRefreshIcon(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PLAYER_REFRESH, x - 4, y - 4);
    }

    private static void drawEyeIcon(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.VISUALS, x - 3, y - 4);
    }

    private static void drawDownIcon(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.DROPDOWN, x - 4, y - 6);
    }

    private static void drawBackIcon(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.BACK, x - 3, y - 5);
    }

    private static void drawResetIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.RESET_OFF : PortalGuiSprites.RESET_ON, x - 4, y - 4);
    }

    private static void drawSwirlIcon(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.SWIRL, x - 4, y - 4);
    }

    private void renderGroupDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        List<UUID> groups = orderedGroupIds();
        DropdownBox box = dropdownBox(groups.size());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
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
        graphics.pose().popPose();
    }

    private void renderVisualDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        List<PortalVisualType> types = PortalVisualRegistry.values();
        DropdownBox box = visualDropdownBox(types.size());
        ResourceLocation selected = PortalVisualPreferences.selectedId();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.renderOutline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        for (int index = 0; index < types.size(); index++) {
            PortalVisualType type = types.get(index);
            int rowY = box.y() + 2 + index * ROW_HEIGHT;
            boolean hover = mouseX >= box.x() + 2 && mouseX < box.x() + box.width() - 2
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hover || index == visualDropdownIndex) {
                graphics.fill(box.x() + 2, rowY, box.x() + box.width() - 2, rowY + ROW_HEIGHT,
                    type.id().equals(selected) ? 0x773F7180 : 0x5530333A);
            }
            graphics.drawString(font, visualName(type), box.x() + 6, rowY + 5,
                type.id().equals(selected) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
        graphics.pose().popPose();
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
            if (groupId.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
                rows.addAll(playerSectionRows(normalizedQuery));
            }
        }
        if (rows.stream().noneMatch(row -> row.kind() == RowKind.PLAYER_SECTION)) {
            rows.addAll(playerSectionRows(normalizedQuery));
        }
        return rows;
    }

    private List<Row> playerSectionRows(String normalizedQuery) {
        List<Row> rows = new ArrayList<>();
        if (!playerSectionVisible()) return rows;
        boolean sectionMatch = normalizedQuery.isEmpty() || "player".contains(normalizedQuery);
        if (!sectionMatch && sortedPlayers(normalizedQuery).isEmpty()) return rows;
        rows.add(new Row(RowKind.PLAYER_SECTION, PortalPlayerData.PLAYER_SECTION_ID, 0));
        if (playerSectionExpanded || !normalizedQuery.isEmpty()) {
            for (PlayerListState.PlayerEntry entry : sortedPlayers(normalizedQuery)) {
                rows.add(new Row(RowKind.PLAYER, entry.id(), 0));
            }
        }
        return rows;
    }

    private List<PlayerListState.PlayerEntry> sortedPlayers(String normalizedQuery) {
        PortalPlayerData data = PortalClientState.data();
        List<PlayerListState.PlayerEntry> list = PlayerListState.players().stream()
            .filter(entry -> normalizedQuery.isEmpty()
                || entry.name().toLowerCase(Locale.ROOT).contains(normalizedQuery))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        DestinationSort sort = data.settings().sort();
        list.sort(Comparator.comparing(PlayerListState.PlayerEntry::pinned).reversed()
            .thenComparing(playerComparator(sort)));
        return list;
    }

    private Comparator<PlayerListState.PlayerEntry> playerComparator(DestinationSort sort) {
        return switch (sort) {
            case NAME -> Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT));
            case CREATED -> Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT));
            case DISTANCE -> Comparator.comparingDouble(this::playerDistanceSquared)
                .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT));
            case RECENT -> Comparator.comparingLong(PlayerListState.PlayerEntry::lastUse).reversed()
                .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT));
        };
    }

    private double playerDistanceSquared(PlayerListState.PlayerEntry entry) {
        if (minecraft == null || minecraft.player == null) return Double.POSITIVE_INFINITY;
        if (entry.x() == null || entry.z() == null) return Double.POSITIVE_INFINITY;
        if (!entry.dimension().equals(minecraft.player.level().dimension().location().toString())) {
            return Double.POSITIVE_INFINITY;
        }
        return minecraft.player.position().distanceToSqr(new Vec3(entry.x(), minecraft.player.getY(), entry.z()));
    }

    private boolean playerSectionVisible() {
        return moduleCount("PLAYER_TARGET") > 0 && PortalClientState.gun().getBoolean("PlayerTargetEnabled");
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
        if (visualDropdownOpen) {
            if (button == 0) clickVisualDropdown(mouseX, mouseY);
            visualDropdownOpen = false;
            return true;
        }
        if (groupDropdownOpen) {
            if (button == 0) clickGroupDropdown(mouseX, mouseY);
            groupDropdownOpen = false;
            return true;
        }
        if (modal != Modal.NONE && modal.isDestinationForm() && groupSelector != null
            && (button == 0 || button == 1) && mouseX >= groupSelector.getX()
            && mouseX < groupSelector.getX() + groupSelector.getWidth()
            && mouseY >= groupSelector.getY() && mouseY < groupSelector.getY() + groupSelector.getHeight()) {
            UUID before = formGroup;
            shiftFormGroup(button == 0 ? 1 : -1);
            if (!before.equals(formGroup) && minecraft != null) {
                groupSelector.playDownSound(minecraft.getSoundManager());
            }
            setFocused(groupSelector);
            return true;
        }
        if (modal == Modal.VISUAL_SETTINGS && visualSelector != null
            && (button == 0 || button == 1) && mouseX >= visualSelector.getX()
            && mouseX < visualSelector.getX() + visualSelector.getWidth()
            && mouseY >= visualSelector.getY() && mouseY < visualSelector.getY() + visualSelector.getHeight()) {
            ResourceLocation before = PortalVisualPreferences.selectedId();
            shiftVisual(button == 0 ? 1 : -1);
            if (!before.equals(PortalVisualPreferences.selectedId()) && minecraft != null) {
                visualSelector.playDownSound(minecraft.getSoundManager());
            }
            setFocused(visualSelector);
            return true;
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
                if (row.kind() == RowKind.PLAYER_SECTION) {
                    if (mouseX >= right - 27 && mouseX < right - 11) {
                        requestPlayerListRefresh();
                    } else {
                        togglePlayerSection();
                    }
                    return true;
                }
                if (row.kind() == RowKind.PLAYER) {
                    PlayerListState.PlayerEntry entry = PlayerListState.player(row.id());
                    if (entry != null) {
                        int starLeft = right - ROW_ACTION_SIZE - 2;
                        if (mouseX >= starLeft && mouseX < starLeft + ROW_ACTION_SIZE) {
                            togglePlayerPin(entry.id(), entry.pinned());
                        } else {
                            selectPlayer(entry.id());
                        }
                    }
                    return true;
                }
                if (row.kind() == RowKind.DESTINATION) {
                    if (mouseX >= panelX + 7 && mouseX < panelX + 20) {
                        draggingDestination = row.id();
                        destinationDragActive = false;
                        dragStartX = mouseX;
                        dragStartY = mouseY;
                        return true;
                    }
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
        if (button == 0 && draggingDestination != null) {
            if (Math.hypot(mouseX - dragStartX, mouseY - dragStartY) >= 5.0) {
                destinationDragActive = true;
            }
            return true;
        }
        if (draggingDetailScrollbar && button == 0) {
            updateDetailScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingDetailScrollbar = false;
        if (button == 0 && draggingDestination != null) {
            UUID moving = draggingDestination;
            boolean active = destinationDragActive;
            draggingDestination = null;
            destinationDragActive = false;
            UUID targetGroup = active ? destinationDropGroupAt(mouseX, mouseY) : null;
            if (targetGroup != null && !targetGroup.equals(destinationGroup(moving))) {
                moveDestinationToGroup(moving, targetGroup);
            } else if (!active) {
                selectDestination(moving);
            }
            return true;
        }
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
        if (visualDropdownOpen) return true;
        if (groupDropdownOpen) {
            int visible = Math.min(7, orderedGroupIds().size());
            groupDropdownScroll = Mth.clamp(groupDropdownScroll - (int) Math.signum(vertical), 0,
                Math.max(0, orderedGroupIds().size() - visible));
            return true;
        }
        if (modal == Modal.SWIRL_ANIMATION_SETTINGS && !visualOptionWidgets.isEmpty()) {
            ModalBox box = modalBox();
            int top = visualOptionsTop(box);
            int bottom = visualOptionsBottom(box);
            if (mouseX >= box.x() + 17 && mouseX < box.x() + box.width() - 17
                && mouseY >= top && mouseY < bottom) {
                visualOptionsScroll = Mth.clamp(
                    visualOptionsScroll - (int) Math.signum(vertical) * 20,
                    0, visualOptionsMaxScroll(box));
                layoutVisualOptionWidgets(box);
                return true;
            }
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
        if (visualDropdownOpen) return visualDropdownKeyPressed(keyCode);
        if (groupDropdownOpen) return dropdownKeyPressed(keyCode);
        if (modal != Modal.NONE && modal.isDestinationForm() && groupSelector != null
            && groupSelector.isFocused() && (keyCode == 263 || keyCode == 262)) {
            shiftFormGroup(keyCode == 263 ? -1 : 1);
            return true;
        }
        if (modal == Modal.VISUAL_SETTINGS && visualSelector != null && visualSelector.isFocused()
            && (keyCode == 263 || keyCode == 262)) {
            shiftVisual(keyCode == 263 ? -1 : 1);
            return true;
        }
        if (keyCode == 256 && modal == Modal.SWIRL_ANIMATION_SETTINGS) {
            backToVisualSettings();
            return true;
        }
        if (keyCode == 256 && modal.isGunSettingPage()) {
            backToGunSettings();
            return true;
        }
        if (keyCode == 256 && modal == Modal.VISUAL_SETTINGS) {
            backToSettings();
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
            else if (focusedRowKind == RowKind.PLAYER) {
                PlayerListState.PlayerEntry entry = PlayerListState.player(focusedRowId);
                if (entry != null && !entry.self()) selectPlayer(entry.id());
            } else if (focusedRowKind == RowKind.PLAYER_SECTION) {
                togglePlayerSection();
            } else toggleGroup(focusedRowId);
            return true;
        }
        if (keyCode == 80 && focusedRowKind == RowKind.PLAYER && focusedRowId != null) {
            PlayerListState.PlayerEntry entry = PlayerListState.player(focusedRowId);
            if (entry != null && !entry.self()) togglePlayerPin(entry.id(), entry.pinned());
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
        selectedPlayerId = null;
        if (!id.equals(previous)) detailScroll = 0;
        if (!id.equals(previous)) {
            pendingSelection = id;
            selectionDueTick = clientTicks + 6L;
        }
        updateOpenPortalButton();
    }

    private void selectPlayer(UUID id) {
        selectedPlayerId = id;
        viewedDestination = null;
        selectedGroup = null;
        pendingSelection = null;
        selectionDueTick = -1L;
        detailScroll = 0;
        focusedRowId = id;
        focusedRowKind = RowKind.PLAYER;
        ensureVisibleId = id;
        updateOpenPortalButton();
        PortalNetworking.sendRequest(PortalAction.SELECT_PLAYER, tag -> tag.putUUID("Target", id));
    }

    private void togglePlayerPin(UUID id, boolean pinned) {
        PlayerListState.markPinned(id, !pinned);
        PortalNetworking.sendRequest(PortalAction.TOGGLE_PLAYER_PIN, tag -> tag.putUUID("Target", id));
    }

    private void requestPlayerListRefresh() {
        PortalNetworking.sendRequest(PortalAction.REQUEST_PLAYERS);
    }

    private void flushSelection() {
        UUID id = pendingSelection;
        pendingSelection = null;
        selectionDueTick = -1L;
        if (id == null || minecraft == null || minecraft.getConnection() == null) return;
        PortalNetworking.sendRequest(PortalAction.SELECT_DESTINATION, tag -> tag.putUUID("Destination", id));
    }

    private void generatePortal() {
        if (selectedPlayerId != null) {
            PortalNetworking.sendRequest(PortalAction.OPEN_PLAYER_PORTAL,
                tag -> tag.putUUID("Target", selectedPlayerId));
            return;
        }
        if (viewedDestination == null) return;
        flushSelection();
        PortalNetworking.sendRequest(PortalAction.OPEN_PORTAL,
            tag -> tag.putUUID("Destination", viewedDestination));
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

    private void requestClearFluid() {
        if (PortalClientState.gun().getInt("Amount") <= 0) return;
        if (PortalClientState.data().settings().confirmClearFluid()) {
            openForm(Modal.CONFIRM_CLEAR_FLUID, null);
        } else {
            PortalNetworking.sendRequest(PortalAction.CLEAR_GUN_FLUID);
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

    private void togglePlayerSection() {
        playerSectionExpanded = !playerSectionExpanded;
        if (playerSectionExpanded) requestPlayerListRefresh();
        PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
            tag.putUUID("Group", PortalPlayerData.PLAYER_SECTION_ID);
            tag.putBoolean("Expanded", playerSectionExpanded);
        });
    }

    private void cycleSort() {
        PortalPlayerSettings current = PortalClientState.data().settings();
        sendSettings(new PortalPlayerSettings(current.safetyCheckEnabled(), current.confirmDeletion(),
            current.confirmDiscardedChanges(), current.confirmClearFluid(), current.animationsEnabled(), current.soundsEnabled(),
            current.sort().next(), current.placementMode(), current.smartDistance(),
            current.predictionMode()));
    }

    private void cyclePlacementMode() {
        PortalPlayerSettings old = PortalClientState.data().settings();
        PortalPlayerSettings next = new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
            old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(), old.sort(),
            old.placementMode().next(), old.smartDistance(), old.predictionMode());
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void cycleMotionPrediction() {
        PortalPlayerSettings old = PortalClientState.data().settings();
        PortalPlayerSettings next = new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
            old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(),
            old.sort(), old.placementMode(), old.smartDistance(), old.predictionMode().next());
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void openGunSettings() {
        modal = Modal.GUN_SETTINGS;
        rebuildWidgets();
    }

    private void openGunSetting(Modal page) {
        if (!page.isGunSettingPage()) return;
        modal = page;
        rebuildWidgets();
    }

    private void backToGunSettings() {
        modal = Modal.GUN_SETTINGS;
        rebuildWidgets();
    }

    private void openVisualSettings() {
        modal = Modal.VISUAL_SETTINGS;
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void openSwirlAnimationSettings() {
        if (PortalVisualPreferences.selected().options().isEmpty()) return;
        modal = Modal.SWIRL_ANIMATION_SETTINGS;
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void backToVisualSettings() {
        flushVisualSettings();
        modal = Modal.VISUAL_SETTINGS;
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void backToSettings() {
        flushVisualSettings();
        modal = Modal.SETTINGS;
        visualDropdownOpen = false;
        rebuildWidgets();
    }

    private boolean coordinateOverrideUnlocked() {
        return PortalClientState.gun().getBoolean("CoordinateOverride");
    }

    private int moduleCount(String kind) {
        return PortalClientState.gun().contains("Modules")
            ? PortalClientState.gun().getCompound("Modules").getInt(kind) : 0;
    }

    private boolean hasEntityTransitModule() {
        return moduleCount("PASSIVE_TRANSIT") > 0 || moduleCount("HOSTILE_TRANSIT") > 0
            || moduleCount("BOSS_TRANSIT") > 0;
    }

    private void toggleGunBoolean(String setting, String snapshotKey) {
        boolean enabled = !PortalClientState.gun().getBoolean(snapshotKey);
        PortalClientState.gun().putBoolean(snapshotKey, enabled);
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting);
            tag.putBoolean("Enabled", enabled);
        });
    }

    private void cyclePlayerExclude() {
        int mode = Math.floorMod(PortalClientState.gun().getInt("PlayerExcludeMode") + 1, 3);
        PortalClientState.gun().putInt("PlayerExcludeMode", mode);
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", "PlayerExclude");
            tag.putInt("Step", 1);
        });
    }

    private void updateGunDistance(String setting, int value) {
        PortalClientState.gun().putInt(setting, value);
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting);
            tag.putInt("Value", value);
        });
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

    private void moveDestinationToGroup(UUID destination, UUID group) {
        Destination current = PortalClientState.data().destination(destination).orElse(null);
        if (current == null) return;
        PortalClientState.data().replaceDestination(current.withGroup(group));
        PortalClientState.data().selectedDestinationId(destination);
        PortalClientState.data().lastViewedDestinationId(destination);
        viewedDestination = destination;
        focusedRowId = destination;
        focusedRowKind = RowKind.DESTINATION;
        listFocused = true;
        detailScroll = 0;
        pendingSelection = null;
        selectionDueTick = -1L;
        if (PortalClientState.data().expandedGroups().contains(group)) ensureVisibleId = destination;
        PortalNetworking.sendRequest(PortalAction.MOVE_DESTINATION_GROUP, tag -> {
            tag.putUUID("Destination", destination);
            tag.putUUID("Group", group);
        });
    }

    private @Nullable UUID destinationDropGroupAt(double mouseX, double mouseY) {
        if (mouseX < panelX + 4 || mouseX >= panelX + listWidth - 4
            || mouseY < listTop || mouseY >= listBottom) return null;
        for (Row row : hitRows) {
            if (mouseY < row.y() || mouseY >= row.y() + ROW_HEIGHT) continue;
            return row.kind() == RowKind.GROUP ? row.id() : destinationGroup(row.id());
        }
        return null;
    }

    private @Nullable UUID destinationGroup(UUID destinationId) {
        Destination destination = PortalClientState.data().destination(destinationId).orElse(null);
        return destination == null ? null : destination.groupId();
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
                old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(), old.sort(),
                old.placementMode(), old.smartDistance(), old.predictionMode());
            case 1 -> new PortalPlayerSettings(old.safetyCheckEnabled(), !old.confirmDeletion(),
                old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(), old.sort(),
                old.placementMode(), old.smartDistance(), old.predictionMode());
            case 2 -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                !old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(), old.sort(),
                old.placementMode(), old.smartDistance(), old.predictionMode());
            case 3 -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                old.confirmDiscardedChanges(), !old.confirmClearFluid(), old.animationsEnabled(), old.soundsEnabled(), old.sort(),
                old.placementMode(), old.smartDistance(), old.predictionMode());
            case 4 -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                old.confirmDiscardedChanges(), old.confirmClearFluid(), !old.animationsEnabled(), old.soundsEnabled(), old.sort(),
                old.placementMode(), old.smartDistance(), old.predictionMode());
            default -> new PortalPlayerSettings(old.safetyCheckEnabled(), old.confirmDeletion(),
                old.confirmDiscardedChanges(), old.confirmClearFluid(), old.animationsEnabled(), !old.soundsEnabled(), old.sort(),
                old.placementMode(), old.smartDistance(), old.predictionMode());
        };
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void sendSettings(PortalPlayerSettings settings) {
        PortalNetworking.sendRequest(PortalAction.SET_SETTINGS, tag -> {
            tag.putBoolean("SafetyCheck", settings.safetyCheckEnabled());
            tag.putBoolean("ConfirmDeletion", settings.confirmDeletion());
            tag.putBoolean("ConfirmDiscardedChanges", settings.confirmDiscardedChanges());
            tag.putBoolean("ConfirmClearFluid", settings.confirmClearFluid());
            tag.putBoolean("Animations", settings.animationsEnabled());
            tag.putBoolean("Sounds", settings.soundsEnabled());
            tag.putString("Sort", settings.sort().name());
            tag.putString("PlacementMode", settings.placementMode().name());
            tag.putInt("SmartDistance", settings.smartDistance());
            tag.putString("MotionPrediction", settings.predictionMode().name());
        });
    }

    private void openForm(Modal next, @Nullable UUID target) {
        modal = next;
        modalTarget = target;
        groupDropdownOpen = false;
        visualDropdownOpen = false;
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
            visualDropdownOpen = false;
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
        } else if (modal == Modal.CONFIRM_CLEAR_FLUID) {
            PortalNetworking.sendRequest(PortalAction.CLEAR_GUN_FLUID);
            closeModalNow();
        }
    }

    private void cancelConfirmation() {
        if (modal == Modal.CONFIRM_DIRTY) {
            modal = returnModal;
            rebuildWidgets();
        } else {
            closeModalNow();
        }
    }

    private void closeModalNow() {
        modal = Modal.NONE;
        returnModal = Modal.NONE;
        modalTarget = null;
        groupDropdownOpen = false;
        visualDropdownOpen = false;
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
        UUID next = GroupSelection.cycle(groups, formGroup, delta);
        if (!next.equals(formGroup)) selectFormGroup(next);
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

    private void openVisualDropdown() {
        visualDropdownOpen = true;
        List<PortalVisualType> types = PortalVisualRegistry.values();
        ResourceLocation selected = PortalVisualPreferences.selectedId();
        visualDropdownIndex = 0;
        for (int index = 0; index < types.size(); index++) {
            if (types.get(index).id().equals(selected)) {
                visualDropdownIndex = index;
                break;
            }
        }
        setFocused(visualSelector);
    }

    private void shiftVisual(int direction) {
        flushVisualSettings();
        PortalVisualPreferences.cycle(direction);
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void selectVisual(PortalVisualType type) {
        flushVisualSettings();
        PortalVisualPreferences.select(type.id());
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private boolean clickVisualDropdown(double mouseX, double mouseY) {
        List<PortalVisualType> types = PortalVisualRegistry.values();
        DropdownBox box = visualDropdownBox(types.size());
        if (mouseX < box.x() || mouseX >= box.x() + box.width()
            || mouseY < box.y() || mouseY >= box.y() + box.height()) return false;
        if (mouseY < box.y() + 2 || mouseY >= box.y() + 2 + types.size() * ROW_HEIGHT) return true;
        int index = (int) ((mouseY - box.y() - 2) / ROW_HEIGHT);
        if (index >= 0 && index < types.size()) selectVisual(types.get(index));
        visualDropdownOpen = false;
        return true;
    }

    private boolean visualDropdownKeyPressed(int keyCode) {
        List<PortalVisualType> types = PortalVisualRegistry.values();
        if (keyCode == 256) {
            visualDropdownOpen = false;
            return true;
        }
        if (keyCode == 265 || keyCode == 264) {
            visualDropdownIndex = Mth.clamp(
                visualDropdownIndex + (keyCode == 265 ? -1 : 1), 0, types.size() - 1);
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            selectVisual(types.get(visualDropdownIndex));
            visualDropdownOpen = false;
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

    public void refreshFromServer(Set<UUID> ignoredInvalidatedSafety) {
        UUID serverSelectedPlayer = PortalClientState.data().selectedPlayerId();
        if (serverSelectedPlayer != null) {
            if (!serverSelectedPlayer.equals(selectedPlayerId)) {
                selectedPlayerId = serverSelectedPlayer;
                viewedDestination = null;
                detailScroll = 0;
                ensureVisibleId = serverSelectedPlayer;
                updateOpenPortalButton();
            }
        } else if (selectedPlayerId != null) {
            selectedPlayerId = null;
            updateOpenPortalButton();
        }
        if (pendingSelection != null) {
            PortalClientState.data().selectedDestinationId(pendingSelection);
            PortalClientState.data().lastViewedDestinationId(pendingSelection);
        }
        UUID selected = PortalClientState.data().selectedDestinationId();
        if (selectedPlayerId == null && selected != null && !selected.equals(viewedDestination)) {
            viewedDestination = selected;
            focusedRowId = selected;
            focusedRowKind = RowKind.DESTINATION;
            detailScroll = 0;
            ensureVisibleId = selected;
        } else if (selectedPlayerId == null && viewedDestination != null
            && PortalClientState.data().destination(viewedDestination).isEmpty()) {
            viewedDestination = selected;
            detailScroll = 0;
        }
        if (selectedGroup != null && !selectedGroup.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && PortalClientState.data().group(selectedGroup).isEmpty()) selectedGroup = null;
        playerSectionExpanded = PortalClientState.data().expandedGroups()
            .contains(PortalPlayerData.PLAYER_SECTION_ID);
        if (modal == Modal.NONE) rebuildWidgets();
    }

    public void onPortalOpened() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void updateOpenPortalButton() {
        if (openPortalButton == null) return;
        openPortalButton.active = viewed() != null || selectedPlayerId != null;
        openPortalButton.setMessage(Component.translatable("screen.riftgun.generate"));
    }

    /** Called when the server refreshes the online player roster. */
    public void onPlayerListRefresh() {
        if (selectedPlayerId != null && PlayerListState.player(selectedPlayerId) == null) {
            selectedPlayerId = null;
            updateOpenPortalButton();
        }
        if (modal == Modal.NONE) rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openCoordinateEditorForQa() {
        openForm(Modal.CREATE_COORDINATE, null);
    }

    /** Used only by the opt-in visual QA harness. */
    public void openGroupDropdownForQa() {
        if (modal.isDestinationForm()) openGroupDropdown();
    }

    /** Used only by the opt-in visual QA harness. */
    public void closeGroupDropdownForQa() {
        groupDropdownOpen = false;
    }

    /** Used only by the opt-in visual QA harness. */
    public void openPlacementSettingsForQa() {
        modal = Modal.SMART_DISTANCE_SETTINGS;
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openVisualSettingsForQa() {
        modal = Modal.VISUAL_SETTINGS;
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void selectSwirlVisualForQa() {
        PortalVisualPreferences.select(PortalVisualRegistry.SWIRL_ID);
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openSwirlAnimationSettingsForQa() {
        if (modal == Modal.VISUAL_SETTINGS) openSwirlAnimationSettings();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openVisualDropdownForQa() {
        if (modal == Modal.VISUAL_SETTINGS) openVisualDropdown();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openGunSettingsForQa() {
        visualDropdownOpen = false;
        openGunSettings();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openSurfaceRangeSettingsForQa() {
        modal = Modal.SURFACE_RANGE_SETTINGS;
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openPortalDurationSettingsForQa() {
        modal = Modal.PORTAL_DURATION_SETTINGS;
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openEntityTransitSettingsForQa() {
        modal = Modal.ENTITY_TRANSIT_SETTINGS;
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openApertureSettingsForQa() {
        modal = Modal.APERTURE_SETTINGS;
        rebuildWidgets();
    }

    private @Nullable Destination viewed() {
        return viewedDestination == null ? null : PortalClientState.data().destination(viewedDestination).orElse(null);
    }

    private String groupName(UUID id) {
        if (id.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return "Default";
        return PortalClientState.data().group(id).map(DestinationGroup::name).orElse("Default");
    }

    private Component gunFluidName() {
        String id = PortalClientState.gun().getString("Fluid");
        if (id.isEmpty()) return Component.translatable("screen.riftgun.empty_fluid");
        int separator = id.indexOf(':');
        String namespace = separator >= 0 ? id.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? id.substring(separator + 1) : id;
        return Component.translatable("fluid." + namespace + "." + path);
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

    private static Component visualName(PortalVisualType type) {
        return Component.translatable(type.nameKey());
    }

    private ModalBox modalBox() {
        int desiredHeight = switch (modal) {
            case CREATE_COORDINATE, EDIT_DESTINATION -> 214;
            case CREATE_CURRENT -> 164;
            case SETTINGS -> 182;
            case GUN_SETTINGS, PORTAL_DURATION_SETTINGS, SMART_DISTANCE_SETTINGS,
                 SURFACE_RANGE_SETTINGS, APERTURE_SETTINGS, ENTITY_TRANSIT_SETTINGS,
                 PLAYER_TARGET_SETTINGS, FALL_GUARD_SETTINGS -> 132;
            case VISUAL_SETTINGS -> 132;
            case SWIRL_ANIMATION_SETTINGS -> 210;
            case CREATE_GROUP, RENAME_GROUP, CONFIRM_DELETE_DESTINATION, CONFIRM_DELETE_GROUP,
                 CONFIRM_DIRTY, CONFIRM_CLEAR_FLUID -> 112;
            case NONE -> 0;
        };
        int boxWidth = Math.min(340, panelWidth - 16);
        int boxHeight = Math.min(desiredHeight, height - 8);
        return new ModalBox((width - boxWidth) / 2, (height - boxHeight) / 2, boxWidth, boxHeight);
    }

    private static int visualOptionsTop(ModalBox box) {
        return box.y() + 34;
    }

    private static int visualOptionsBottom(ModalBox box) {
        return box.y() + box.height() - 31;
    }

    private static int visualOptionsViewportHeight(ModalBox box) {
        return Math.max(1, visualOptionsBottom(box) - visualOptionsTop(box));
    }

    private int visualOptionsMaxScroll(ModalBox box) {
        return Math.max(0, visualOptionsContentHeight - visualOptionsViewportHeight(box));
    }

    private DropdownBox dropdownBox(int groupCount) {
        int visible = Math.min(7, groupCount);
        int height = visible * ROW_HEIGHT + 4;
        ModalBox modalBounds = modalBox();
        int minY = modalBounds.y() + 3;
        int maxY = modalBounds.y() + modalBounds.height() - height - 3;
        int upward = groupSelectorY - height - 2;
        int downward = groupSelectorY + 20;
        int top = upward >= minY ? upward : Math.min(downward, maxY);
        top = Mth.clamp(top, minY, Math.max(minY, maxY));
        return new DropdownBox(groupSelectorX, top, groupSelectorWidth, height);
    }

    private DropdownBox visualDropdownBox(int typeCount) {
        int height = typeCount * ROW_HEIGHT + 4;
        ModalBox modalBounds = modalBox();
        int minY = modalBounds.y() + 3;
        int maxY = modalBounds.y() + modalBounds.height() - height - 3;
        int downward = visualSelectorY + 20;
        int upward = visualSelectorY - height - 2;
        int top = downward <= maxY ? downward : Math.max(minY, upward);
        return new DropdownBox(visualSelectorX, top, visualSelectorWidth, height);
    }

    private static void drawDisclosure(GuiGraphics graphics, int x, int y, boolean expanded) {
        PortalGuiSprites.draw(graphics, expanded
            ? PortalGuiSprites.GROUP_EXPANDED : PortalGuiSprites.GROUP_COLLAPSED,
            x - (expanded ? 4 : 6), y - (expanded ? 6 : 4));
    }

    private static void drawDragHandle(GuiGraphics graphics, int x, int y) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.DRAG_HANDLE, x - 5, y - 4);
    }

    private static void drawDestinationDragDot(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, color == PortalTheme.TEXT_MUTED
            ? PortalGuiSprites.DESTINATION_DOT_OFF : PortalGuiSprites.DESTINATION_DOT_ON, x - 7, y - 7);
    }

    private static void drawStar(GuiGraphics graphics, int x, int y, boolean filled) {
        PortalGuiSprites.draw(graphics, filled ? PortalGuiSprites.STAR_ON : PortalGuiSprites.STAR_OFF,
            x - 4, y - 4);
    }

    private static void drawCross(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.DELETE, x - 4, y - 4);
    }

    private static void drawPencil(GuiGraphics graphics, int x, int y, int color) {
        PortalGuiSprites.draw(graphics, PortalGuiSprites.EDIT, x - 4, y - 3);
    }

    private enum RowKind { GROUP, DESTINATION, PLAYER_SECTION, PLAYER }
    private record Row(RowKind kind, UUID id, int y) {}
    private record ModalBox(int x, int y, int width, int height) {}
    private record DropdownBox(int x, int y, int width, int height) {}
    private record VisualWidgetBinding(AbstractWidget widget, int contentOffset) {}
    private record VisualToggleBinding(ThemedButton widget, PortalVisualOption.Toggle option) {}

    private enum Modal {
        NONE("", "", false, false),
        CREATE_CURRENT("screen.riftgun.create_current", "", true, false),
        CREATE_COORDINATE("screen.riftgun.create_coordinate", "", true, true),
        EDIT_DESTINATION("screen.riftgun.edit_destination", "", true, true),
        CREATE_GROUP("screen.riftgun.create_group", "", true, false),
        RENAME_GROUP("screen.riftgun.rename_group", "", true, false),
        SETTINGS("screen.riftgun.settings", "", false, false),
        GUN_SETTINGS("screen.riftgun.configure_gun", "", false, false),
        PORTAL_DURATION_SETTINGS("screen.riftgun.portal_duration", "", false, false),
        SMART_DISTANCE_SETTINGS("screen.riftgun.smart_distance", "", false, false),
        SURFACE_RANGE_SETTINGS("screen.riftgun.surface_range", "", false, false),
        ENTITY_TRANSIT_SETTINGS("screen.riftgun.entity_transit", "", false, false),
        APERTURE_SETTINGS("screen.riftgun.aperture", "", false, false),
        FALL_GUARD_SETTINGS("screen.riftgun.fall_guard", "", false, false),
        PLAYER_TARGET_SETTINGS("screen.riftgun.player_target", "", false, false),
        VISUAL_SETTINGS("screen.riftgun.visual_settings", "", false, false),
        SWIRL_ANIMATION_SETTINGS("screen.riftgun.visual.swirl_animation_settings", "", false, false),
        CONFIRM_DELETE_DESTINATION("screen.riftgun.delete", "screen.riftgun.delete_destination_body", false, false),
        CONFIRM_DELETE_GROUP("screen.riftgun.delete", "screen.riftgun.delete_group_body", false, false),
        CONFIRM_DIRTY("screen.riftgun.unsaved", "screen.riftgun.unsaved_body", false, false),
        CONFIRM_CLEAR_FLUID("screen.riftgun.clear_fluid", "screen.riftgun.clear_fluid_body", false, false);

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
        boolean isGunSettingPage() {
            return this == PORTAL_DURATION_SETTINGS || this == SMART_DISTANCE_SETTINGS
                || this == SURFACE_RANGE_SETTINGS || this == ENTITY_TRANSIT_SETTINGS
                || this == APERTURE_SETTINGS || this == PLAYER_TARGET_SETTINGS
                || this == FALL_GUARD_SETTINGS;
        }
        boolean hasInputs() { return hasName || hasCoordinates; }
        boolean isDestinationForm() {
            return this == CREATE_CURRENT || this == CREATE_COORDINATE || this == EDIT_DESTINATION;
        }
    }

    private final class VisualPeriodSlider extends AbstractSliderButton {
        private final PortalVisualOption.Range option;

        private VisualPeriodSlider(int x, int y, int width, int height, PortalVisualOption.Range option) {
            super(x, y, width, height, Component.empty(), option.normalizedValue());
            this.option = option;
            refreshFromOption();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(option.labelKey(),
                String.format(Locale.ROOT, "%.1f", option.currentValue())));
        }

        @Override
        protected void applyValue() {
            option.update().accept(option.valueAt(value));
            value = option.normalizedValue();
            updateMessage();
            markVisualSettingsDirty();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            flushVisualSettings();
        }

        private void refreshFromOption() {
            value = option.normalizedValue();
            active = option.active();
            updateMessage();
        }
    }

    private final class GunDistanceSlider extends AbstractSliderButton {
        private final String setting;
        private final String labelKey;
        private final int minimum;
        private final int maximum;
        private final double displayDivisor;
        private final int permanentValue;
        private final String permanentLabelKey;
        private int committedValue;

        private GunDistanceSlider(int x, int y, int width, int height, String setting,
                                  String labelKey, int minimum, int maximum, int distance) {
            this(x, y, width, height, setting, labelKey, minimum, maximum, distance, 1.0);
        }

        private GunDistanceSlider(int x, int y, int width, int height, String setting,
                                  String labelKey, int minimum, int maximum, int distance,
                                  double displayDivisor) {
            this(x, y, width, height, setting, labelKey, minimum, maximum, distance,
                displayDivisor, 0, null);
        }

        private GunDistanceSlider(int x, int y, int width, int height, String setting,
                                  String labelKey, int minimum, int maximum, int distance,
                                  double displayDivisor, int permanentValue, String permanentLabelKey) {
            super(x, y, width, height, Component.empty(), normalize(distance, minimum, maximum));
            this.setting = setting;
            this.labelKey = labelKey;
            this.minimum = minimum;
            this.maximum = Math.max(minimum, maximum);
            this.displayDivisor = displayDivisor;
            this.permanentValue = permanentValue;
            this.permanentLabelKey = permanentLabelKey;
            committedValue = distance();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (permanentValue > 0 && distance() >= permanentValue) {
                setMessage(Component.translatable(permanentLabelKey));
            } else if (displayDivisor == 1.0) {
                setMessage(Component.translatable(labelKey, distance()));
            } else {
                setMessage(Component.translatable(labelKey,
                    String.format(Locale.ROOT, "%.1f", distance() / displayDivisor)));
            }
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            commit();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
            if (handled) commit();
            return handled;
        }

        private void commit() {
            int next = distance();
            if (next == committedValue) return;
            committedValue = next;
            updateGunDistance(setting, next);
        }

        private int distance() {
            return minimum + (int) Math.round(value * (maximum - minimum));
        }

        private static double normalize(int value, int minimum, int maximum) {
            if (maximum <= minimum) return 0.0;
            return (Mth.clamp(value, minimum, maximum) - minimum) / (double) (maximum - minimum);
        }
    }
}
