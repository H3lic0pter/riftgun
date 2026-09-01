package dev.riftgun.client.screen;

import static dev.riftgun.client.screen.PortalGuiIcons.*;
import static dev.riftgun.ui.PortalConfigLayout.*;
import static dev.riftgun.ui.PortalConfigPresentation.*;
import static dev.riftgun.ui.PortalConfigRows.*;

import dev.riftgun.client.DimensionLabelState;
import dev.riftgun.client.PlayerListState;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.PortalGuiScrollMemory;
import dev.riftgun.client.PortalInputLabels;
import dev.riftgun.client.external.ClientMapWaypointIntegration;
import dev.riftgun.external.client.ExternalDestination;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.client.render.PortalVisualPreferences;
import dev.riftgun.client.render.PortalVisualOption;
import dev.riftgun.client.render.PortalVisualOptions;
import dev.riftgun.client.render.PortalVisualRegistry;
import dev.riftgun.client.render.PortalVisualType;
import dev.riftgun.config.ClientConfig;
import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.DestinationSafetyResult;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlayerSettings;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.ExternalDestinationRequest;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.pairing.PortalFloatingFallback;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.state.PortalGunViewStateReducer;
import dev.riftgun.state.PortalGunViewStateReducer.BooleanSetting;
import dev.riftgun.state.PortalGunViewStateReducer.DistanceSetting;
import dev.riftgun.state.PortalGunViewStateReducer.FallbackSetting;
import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.external.ExternalDestinationSource;
import dev.riftgun.sound.PortalSoundChannel;
import dev.riftgun.sound.PortalSoundChoice;
import dev.riftgun.sound.PortalSoundRegistry;
import dev.riftgun.sound.PortalSoundSettings;
import dev.riftgun.ui.PortalConfigLayout;
import dev.riftgun.ui.PortalConfigLayout.Box;
import dev.riftgun.ui.PortalConfigPage;
import dev.riftgun.ui.PortalConfigRows;
import dev.riftgun.ui.PortalConfigRows.ExternalSection;
import dev.riftgun.ui.PortalConfigRows.PlayerEntry;
import dev.riftgun.ui.PortalConfigRows.PlayerSection;
import dev.riftgun.ui.PortalConfigRows.Row;
import dev.riftgun.ui.PortalConfigRows.RowKind;
import dev.riftgun.ui.PortalConfigSession;
import dev.riftgun.ui.PortalConfigSettings;
import dev.riftgun.ui.PortalConfigSettings.Toggle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class PortalConfigScreen extends Screen {
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
    private int detailShareY = -1;
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
    private @Nullable UUID viewedExternalRow;
    private @Nullable UUID selectedExternalRow;
    private final Map<UUID, ExternalDestination> externalRows = new HashMap<>();
    private final Set<ExternalDestinationSource> expandedExternalGroups =
        EnumSet.allOf(ExternalDestinationSource.class);
    private boolean externalDestinationsInitialized;
    private final List<Row> hitRows = new ArrayList<>();
    private final Map<UUID, Float> animatedRowY = new HashMap<>();

    private final PortalConfigSession session = new PortalConfigSession();
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
    private @Nullable PortalSoundChannel soundDropdownChannel;
    private int soundDropdownIndex;

    private @Nullable EditBox searchBox;
    private @Nullable ThemedButton firstCreateButton;
    private @Nullable ThemedButton coordinateButton;
    private @Nullable ThemedButton gunSettingsButton;
    private @Nullable ThemedButton moduleBayButton;
    private @Nullable ThemedButton closePortalsButton;
    private @Nullable ThemedButton dimensionalTraversalButton;
    private @Nullable ThemedButton gunSettingsBackButton;
    private @Nullable ThemedButton portalDurationSettingsButton;
    private @Nullable ThemedButton smartRangeSettingsButton;
    private @Nullable ThemedButton remoteSettingsButton;
    private @Nullable ThemedButton entityTransitSettingsButton;
    private @Nullable ThemedButton apertureSettingsButton;
    private @Nullable ThemedButton fallGuardSettingsButton;
    private @Nullable ThemedButton entityRelocationSettingsButton;
    private @Nullable ThemedButton moduleSettingBackButton;
    private @Nullable ThemedButton passiveTransitButton;
    private @Nullable ThemedButton hostileTransitButton;
    private @Nullable ThemedButton bossTransitButton;
    private @Nullable ThemedButton projectileTransitButton;
    private @Nullable ThemedButton apertureToggleButton;
    private @Nullable ThemedButton fallGuardToggleButton;
    private @Nullable ThemedButton entityFallGuardToggleButton;
    private @Nullable ThemedButton playerTargetButton;
    private @Nullable ThemedButton playerExcludeButton;
    private @Nullable ThemedButton playerTargetSettingsButton;
    private @Nullable ThemedButton entityRelocationEnabledButton;
    private @Nullable ThemedButton entityRelocationSmartButton;
    private final List<EditBox> coordinateEditFields = new ArrayList<>();
    private final PlayerTargetController playerTargets;
    private @Nullable ThemedButton groupSelector;
    private @Nullable ThemedButton groupDropdownButton;
    private @Nullable ThemedButton motionPredictionButton;
    private @Nullable ThemedButton placementModeButton;
    private @Nullable ThemedButton functionModeButton;
    private @Nullable ThemedButton portalPairingSettingsButton;
    private @Nullable ThemedButton coordinateFallbackButton;
    private @Nullable ThemedButton pairingFallbackButton;
    private @Nullable ThemedButton remoteScrollAdjustmentButton;
    private @Nullable ThemedButton remoteRadialSliderButton;
    private @Nullable ThemedButton remotePlacementPreviewButton;
    private @Nullable ThemedButton visualSettingsButton;
    private @Nullable ThemedButton soundSettingsButton;
    private @Nullable ThemedButton soundBackButton;
    private @Nullable ThemedButton splashSoundButton;
    private final Map<PortalSoundChannel, ThemedButton> soundSelectors =
        new EnumMap<>(PortalSoundChannel.class);
    private final Map<PortalSoundChannel, ThemedButton> soundDropdownButtons =
        new EnumMap<>(PortalSoundChannel.class);
    private @Nullable ThemedButton visualBackButton;
    private @Nullable ThemedButton swirlAnimationBackButton;
    private @Nullable ThemedButton visualSelector;
    private @Nullable ThemedButton visualDropdownButton;
    private @Nullable ThemedButton visualAnimationSettingsButton;
    private @Nullable ThemedButton visualResetButton;
    private final List<VisualWidgetBinding> visualOptionWidgets = new ArrayList<>();
    private final List<VisualToggleBinding> visualToggleWidgets = new ArrayList<>();
    private @Nullable ThemedButton openPortalButton;
    private @Nullable ThemedButton randomRiftButton;
    private @Nullable ThemedButton bucketModeButton;
    private @Nullable ThemedButton clearFluidButton;
    private @Nullable ThemedButton mapRefreshButton;
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
        PortalGuiScrollMemory.Position scroll = PortalGuiScrollMemory.restore(rememberScrollPosition());
        listScroll = scroll.listScroll();
        detailScroll = scroll.detailScroll();
        PortalPlayerData data = PortalClientState.data();
        playerTargets = new PlayerTargetController(data);
        syncExternalGroupExpansion(data);
        ExternalDestinationSelection externalSelection = ClientMapWaypointIntegration.selected();
        if (externalSelection != null) {
            selectedExternalRow = externalRowId(externalSelection.source(), externalSelection.stableId());
            viewedExternalRow = selectedExternalRow;
        }
        if (playerTargets.selectedId() != null) {
            viewedDestination = null;
            focusedRowId = playerTargets.selectedId();
            focusedRowKind = RowKind.PLAYER;
        } else {
            viewedDestination = data.selectedDestinationId() != null
                ? data.selectedDestinationId() : data.lastViewedDestinationId();
            focusedRowId = viewedDestination;
            focusedRowKind = viewedDestination == null ? null : RowKind.DESTINATION;
        }
        if (externalSelection != null) {
            viewedDestination = null;
            playerTargets.clearSelection();
            focusedRowId = selectedExternalRow;
            focusedRowKind = RowKind.EXTERNAL_DESTINATION;
        }
    }

    @Override
    protected void init() {
        placementModeButton = null;
        functionModeButton = null;
        coordinateButton = null;
        gunSettingsButton = null;
        moduleBayButton = null;
        closePortalsButton = null;
        dimensionalTraversalButton = null;
        gunSettingsBackButton = null;
        portalDurationSettingsButton = null;
        smartRangeSettingsButton = null;
        remoteSettingsButton = null;
        entityTransitSettingsButton = null;
        apertureSettingsButton = null;
        fallGuardSettingsButton = null;
        entityRelocationSettingsButton = null;
        portalPairingSettingsButton = null;
        coordinateFallbackButton = null;
        pairingFallbackButton = null;
        remoteScrollAdjustmentButton = null;
        remoteRadialSliderButton = null;
        remotePlacementPreviewButton = null;
        moduleSettingBackButton = null;
        passiveTransitButton = null;
        hostileTransitButton = null;
        bossTransitButton = null;
        projectileTransitButton = null;
        apertureToggleButton = null;
        fallGuardToggleButton = null;
        entityFallGuardToggleButton = null;
        playerTargetButton = null;
        playerExcludeButton = null;
        playerTargetSettingsButton = null;
        entityRelocationEnabledButton = null;
        entityRelocationSmartButton = null;
        coordinateEditFields.clear();
        groupDropdownButton = null;
        motionPredictionButton = null;
        visualSettingsButton = null;
        soundSettingsButton = null;
        soundBackButton = null;
        splashSoundButton = null;
        soundSelectors.clear();
        soundDropdownButtons.clear();
        visualBackButton = null;
        swirlAnimationBackButton = null;
        visualSelector = null;
        visualDropdownButton = null;
        visualAnimationSettingsButton = null;
        visualResetButton = null;
        visualOptionWidgets.clear();
        visualToggleWidgets.clear();
        openPortalButton = null;
        randomRiftButton = null;
        bucketModeButton = null;
        clearFluidButton = null;
        mapRefreshButton = null;
        panelWidth = Math.min(520, width - 12);
        panelHeight = Math.min(320, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        boolean compact = panelWidth < 360;
        listWidth = compact ? Math.max(132, panelWidth * 54 / 100)
            : Math.max(156, panelWidth * 57 / 100);
        listTop = panelY + HEADER_HEIGHT;
        listBottom = panelY + panelHeight - footerHeight();

        if (session.page() != PortalConfigPage.NONE) {
            initModal();
            return;
        }

        refreshExternalDestinations(false);
        int searchWidth = ClientMapWaypointIntegration.anyInstalled() ? listWidth - 42 : listWidth - 20;
        searchBox = new EditBox(font, panelX + 10, panelY + 25, searchWidth, 17,
            Component.translatable("screen.riftgun.search"));
        searchBox.setValue(query);
        searchBox.setHint(Component.translatable("screen.riftgun.search_hint"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(value -> query = value);
        addRenderableWidget(searchBox);
        if (ClientMapWaypointIntegration.anyInstalled()) {
            mapRefreshButton = button(panelX + listWidth - 29, panelY + 25, 19, 17,
                Component.empty(), false, ignored -> refreshExternalDestinations(true));
        }

        int rightX = panelX + listWidth + 8;
        int available = panelWidth - listWidth - 16;
        int compactButtonWidth = Math.max(26, (available - 6) / 3);
        firstCreateButton = button(rightX, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.save_here", false, ignored -> openForm(PortalConfigPage.CREATE_CURRENT, null));
        coordinateButton = button(rightX + compactButtonWidth + 3, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.add_coordinate", false, ignored -> openForm(PortalConfigPage.CREATE_COORDINATE, null));
        coordinateButton.visible = coordinateOverrideUnlocked();
        button(rightX + (compactButtonWidth + 3) * 2, panelY + 24, compactButtonWidth, 18,
            "screen.riftgun.add_group", false, ignored -> openForm(PortalConfigPage.CREATE_GROUP, null));

        moduleBayButton = button(panelX + panelWidth - 29, panelY + 3, 19, 18,
            Component.empty(), false, ignored -> PortalNetworking.sendRequest(PortalAction.OPEN_MODULES));
        gunSettingsButton = button(panelX + panelWidth - 51, panelY + 3, 19, 18,
            Component.empty(), false, ignored -> openGunSettings());
        closePortalsButton = button(panelX + panelWidth - 73, panelY + 3, 19, 18,
            Component.empty(), false, ignored -> PortalNetworking.sendRequest(PortalAction.CLOSE_PORTALS));
        if (PortalClientState.gun().dimensionalTraversalInstalled()
            && PortalClientState.gun().dimensionalTraversalEnabled()) {
            dimensionalTraversalButton = button(panelX + panelWidth - 95, panelY + 3, 19, 18,
                Component.empty(), false, ignored -> minecraft.setScreen(
                    new DimensionalNavigationScreen(this, creationGroup())));
        }

        int footerY = panelY + panelHeight - 28;
        boolean showFunctionMode = pairingInstalled();
        int modeControlStart = panelX + listWidth - (showFunctionMode ? 72 : 50);
        int settingsWidth = showFunctionMode
            ? Math.max(18, Math.min(54, (modeControlStart - panelX - 16) / 2)) : 54;
        ThemedButton settingsButton = button(panelX + 10, footerY, settingsWidth, 19,
            "screen.riftgun.settings", false, ignored -> openForm(PortalConfigPage.SETTINGS, null));
        settingsButton.horizontalMarquee();
        int sortX = panelX + 13 + settingsWidth;
        int sortWidth = showFunctionMode
            ? Math.max(12, Math.min(82, modeControlStart - sortX - 3))
            : Math.max(12, Math.min(82, listWidth - 120));
        ThemedButton sortButton = button(sortX, footerY, sortWidth, 19,
            Component.translatable("screen.riftgun.sort_mode", Component.translatable(
                "screen.riftgun.sort." + PortalClientState.data().settings().sort().name().toLowerCase(Locale.ROOT))),
            false, ignored -> cycleSort());
        sortButton.horizontalMarquee();
        if (showFunctionMode) {
            functionModeButton = button(panelX + listWidth - 72, footerY, 19, 19,
                Component.empty(), false,
                ignored -> PortalNetworking.sendRequest(PortalAction.TOGGLE_FUNCTION_MODE));
        }
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
        clearFluidButton.active = PortalClientState.gun().amount() > 0;
        int portalButtonX = rightX + FUEL_GAUGE_WIDTH + 47;
        if (Nbt.getBoolean(PortalClientState.randomRift(), "Enabled") && coordinateOverrideUnlocked()) {
            randomRiftButton = button(portalButtonX, footerY, 19, 19,
                Component.empty(), false,
                ignored -> requestRandomRift());
            randomRiftButton.active = !Nbt.getBoolean(PortalClientState.randomRift(), "Searching")
                && PortalClientState.randomRiftCooldownTicks() <= 0;
            portalButtonX += 22;
        }
        ThemedButton generate = button(portalButtonX, footerY,
            Math.max(1, rightX + available - portalButtonX), 19,
            "screen.riftgun.generate", true, ignored -> generatePortal());
        generate.horizontalMarquee();
        generate.active = viewed() != null;
        openPortalButton = generate;
        updateOpenPortalButton();

        playerTargets.requestListIfNeeded();
    }

    @Override
    public void tick() {
        super.tick();
        clientTicks++;
        updateRandomRiftButton();
        if (pendingSelection != null && clientTicks >= selectionDueTick) flushSelection();
        if (visualSettingsDirty && clientTicks >= visualSettingsSaveDueTick) flushVisualSettings();
        if (clientTicks % 10L == 0L) refreshJourneyMapIfDirty();
    }

    @Override
    public void onClose() {
        saveScrollPosition();
        flushSelection();
        flushVisualSettings();
        super.onClose();
    }

    @Override
    public void removed() {
        saveScrollPosition();
        flushSelection();
        flushVisualSettings();
        super.removed();
    }

    private void initModal() {
        Box box = modalBox();
        int x = box.x();
        int y = box.y();
        int fieldWidth = box.width() - 36;
        int gunSettingControlY = gunSettingControlTop(box);
        groupDropdownOpen = false;
        visualDropdownOpen = false;
        soundDropdownChannel = null;

        if (session.page() == PortalConfigPage.CREATE_COORDINATE || session.page() == PortalConfigPage.EDIT_DESTINATION) {
            addField(x + 18, y + 41, fieldWidth, formName, 48, value -> formName = value);
            int half = (fieldWidth - 10) / 2;
            boolean coordinatesEditable = session.page() == PortalConfigPage.CREATE_COORDINATE || coordinateEditingUnlocked();
            addCoordinateField(x + 18, y + 80, half, formX, value -> formX = value, coordinatesEditable);
            addCoordinateField(x + 28 + half, y + 80, half, formY, value -> formY = value, coordinatesEditable);
            addCoordinateField(x + 18, y + 119, half, formZ, value -> formZ = value, coordinatesEditable);
            addCoordinateField(x + 28 + half, y + 119, half, formYaw, value -> formYaw = value, coordinatesEditable);
            addGroupSelector(x + 18, y + 158, fieldWidth);
        } else if (session.page() == PortalConfigPage.CREATE_CURRENT) {
            addField(x + 18, y + 41, fieldWidth, formName, 48, value -> formName = value);
            addGroupSelector(x + 18, y + 111, fieldWidth);
        } else if (session.page() == PortalConfigPage.CREATE_GROUP || session.page() == PortalConfigPage.RENAME_GROUP) {
            addField(x + 18, y + 44, fieldWidth, formName, 32, value -> formName = value);
        } else if (session.page() == PortalConfigPage.SHARE_DESTINATION) {
            button(x + 18, y + 38, fieldWidth, 19, "screen.riftgun.share_chat", false,
                ignored -> shareViewed(PortalAction.SHARE_DESTINATION_CHAT));
            button(x + 18, y + 62, fieldWidth, 19, "screen.riftgun.share_item", false,
                ignored -> shareViewed(PortalAction.CREATE_COORDINATE_NOTE));
        } else if (session.page() == PortalConfigPage.SETTINGS) {
            PortalPlayerSettings settings = PortalClientState.data().settings();
            button(x + 18, y + 28, fieldWidth, 18,
                toggleLabel("screen.riftgun.safety", settings.safetyCheckEnabled()), false,
                ignored -> updateSetting(Toggle.SAFETY_CHECK));
            button(x + 18, y + 47, fieldWidth, 18,
                toggleLabel("screen.riftgun.animations", settings.animationsEnabled()), false,
                ignored -> updateSetting(Toggle.ANIMATIONS));
            button(x + 18, y + 66, fieldWidth, 18,
                toggleLabel("screen.riftgun.sounds", settings.soundsEnabled()), false,
                ignored -> updateSetting(Toggle.SOUNDS));
            button(x + 18, y + 85, fieldWidth, 18,
                toggleLabel("screen.riftgun.remember_scroll_position", rememberScrollPosition()), false,
                ignored -> toggleRememberScrollPosition());
            button(x + 18, y + 104, fieldWidth, 18,
                "screen.riftgun.confirm_settings", false,
                ignored -> openSettingsPage(PortalConfigPage.CONFIRM_SETTINGS));
            if (ClientMapWaypointIntegration.anyInstalled()) {
                button(x + 18, y + 123, fieldWidth, 18,
                    "screen.riftgun.map_integration_settings", false,
                    ignored -> openSettingsPage(PortalConfigPage.MAP_INTEGRATION_SETTINGS));
            }
            visualSettingsButton = button(x + box.width() - 64, y + 8, 20, 18,
                Component.empty(), false, ignored -> openVisualSettings());
            soundSettingsButton = button(x + box.width() - 40, y + 8, 20, 18,
                Component.empty(), false, ignored -> openSoundSettings());
        } else if (session.page() == PortalConfigPage.CONFIRM_SETTINGS) {
            PortalPlayerSettings settings = PortalClientState.data().settings();
            button(x + 18, y + 35, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_deletion", settings.confirmDeletion()), false,
                ignored -> updateSetting(Toggle.CONFIRM_DELETION));
            button(x + 18, y + 58, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_discard", settings.confirmDiscardedChanges()), false,
                ignored -> updateSetting(Toggle.CONFIRM_DISCARD));
            button(x + 18, y + 81, fieldWidth, 18,
                toggleLabel("screen.riftgun.confirm_clear_fluid", settings.confirmClearFluid()), false,
                ignored -> updateSetting(Toggle.CONFIRM_CLEAR_FLUID));
        } else if (session.page() == PortalConfigPage.MAP_INTEGRATION_SETTINGS) {
            int optionY = y + 34;
            if (ClientMapWaypointIntegration.installed(ExternalDestinationSource.JOURNEYMAP)) {
                button(x + 18, optionY, fieldWidth, 18,
                    toggleLabel("screen.riftgun.map.journeymap",
                        ClientConfig.VALUES.journeyMapWaypointsEnabled.get()), false,
                    ignored -> toggleMapSource(ExternalDestinationSource.JOURNEYMAP));
                optionY += 23;
            }
            if (ClientMapWaypointIntegration.installed(ExternalDestinationSource.XAERO_MINIMAP)) {
                button(x + 18, optionY, fieldWidth, 18,
                    toggleLabel("screen.riftgun.map.xaero",
                        ClientConfig.VALUES.xaeroWaypointsEnabled.get()), false,
                    ignored -> toggleMapSource(ExternalDestinationSource.XAERO_MINIMAP));
                optionY += 23;
            }
            addRenderableWidget(new MapWaypointLimitSlider(x + 18, optionY, fieldWidth, 18));
        } else if (session.page() == PortalConfigPage.GUN_SETTINGS) {
            int buttonX = x + 18;
            portalDurationSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                ignored -> openGunSetting(PortalConfigPage.PORTAL_DURATION_SETTINGS));
            buttonX += 31;
            smartRangeSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                ignored -> openGunSetting(PortalConfigPage.SMART_DISTANCE_SETTINGS));
            buttonX += 31;
            if (remoteInstalled()) {
                remoteSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.REMOTE_SETTINGS));
                buttonX += 31;
            }
            if (hasEntityTransitModule()) {
                entityTransitSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.ENTITY_TRANSIT_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("PLAYER_TARGET") > 0) {
                playerTargetSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.PLAYER_TARGET_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("APERTURE_EXPANSION") > 0) {
                apertureSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.APERTURE_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("FALL_GUARD") > 0) {
                fallGuardSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.FALL_GUARD_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("ENTITY_RELOCATION") > 0) {
                entityRelocationSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.ENTITY_RELOCATION_SETTINGS));
                buttonX += 31;
            }
            if (moduleCount("PORTAL_PAIRING") > 0 || remoteInstalled()) {
                portalPairingSettingsButton = button(buttonX, y + 45, 26, 26, Component.empty(), false,
                    ignored -> openGunSetting(PortalConfigPage.PORTAL_PAIRING_SETTINGS));
            }
        } else if (session.page() == PortalConfigPage.PORTAL_DURATION_SETTINGS) {
            boolean eternal = PortalClientState.gun().eternalDurationInstalled();
            int maximum = eternal ? 301 : Math.max(1, PortalClientState.gun().maximumPortalDurationSeconds());
            addRenderableWidget(new GunDistanceSlider(x + 18, gunSettingControlY, fieldWidth, 18,
                DistanceSetting.PORTAL_DURATION, "screen.riftgun.portal_duration_value", 1, maximum,
                PortalClientState.gun().portalDurationSeconds(), 1.0,
                eternal ? 301 : 0, "screen.riftgun.portal_duration_permanent"));
            int cooldownMaximum = Math.max(1, PortalClientState.gun().maximumTransitCooldownTenths());
            addRenderableWidget(new GunDistanceSlider(x + 18, gunSettingControlY + 24, fieldWidth, 18,
                DistanceSetting.TRANSIT_COOLDOWN, "screen.riftgun.transit_cooldown_value", 0, cooldownMaximum,
                PortalClientState.gun().transitCooldownTenths(), 10.0));
        } else if (session.page() == PortalConfigPage.SMART_DISTANCE_SETTINGS) {
            int maximum = Math.max(1,
                PortalClientState.gun().maximumSurfaceRange());
            addRenderableWidget(new GunDistanceSlider(x + 18, gunSettingControlY, fieldWidth, 18,
                DistanceSetting.SMART_DISTANCE, "screen.riftgun.smart_distance_value", 1,
                maximum,
                PortalClientState.gun().smartDistance()));
        } else if (session.page() == PortalConfigPage.ENTITY_TRANSIT_SETTINGS) {
            addEntityTransitButtons(x + 18, gunSettingControlY);
        } else if (session.page() == PortalConfigPage.PLAYER_TARGET_SETTINGS) {
            int buttonX = x + 18;
            playerTargetButton = button(buttonX, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.PLAYER_TARGET));
            buttonX += 31;
            playerExcludeButton = button(buttonX, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> cyclePlayerExclude());
        } else if (session.page() == PortalConfigPage.APERTURE_SETTINGS) {
            apertureToggleButton = button(x + 18, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.EXPANDED_APERTURE));
        } else if (session.page() == PortalConfigPage.FALL_GUARD_SETTINGS) {
            fallGuardToggleButton = button(x + 18, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.FALL_GUARD));
            entityFallGuardToggleButton = button(x + 49, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.FALL_GUARD_ENTITIES));
        } else if (session.page() == PortalConfigPage.ENTITY_RELOCATION_SETTINGS) {
            entityRelocationEnabledButton = button(x + 18, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.ENTITY_RELOCATION));
            entityRelocationSmartButton = button(x + 49, gunSettingControlY, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.ENTITY_RELOCATION_SMART_ROUTING));
        } else if (session.page() == PortalConfigPage.PORTAL_PAIRING_SETTINGS) {
            int optionY = gunSettingControlY;
            if (remoteInstalled()) {
                coordinateFallbackButton = button(x + 18, optionY, fieldWidth, 19,
                    fallbackLabel("screen.riftgun.pairing.coordinate_fallback",
                        FallbackSetting.COORDINATE_SMART),
                    false, ignored -> cyclePairingFallback(FallbackSetting.COORDINATE_SMART));
                optionY += 24;
            }
            if (pairingInstalled()) {
                pairingFallbackButton = button(x + 18, optionY, fieldWidth, 19,
                    fallbackLabel("screen.riftgun.pairing.pairing_fallback",
                        FallbackSetting.PAIRING_SMART),
                    false, ignored -> cyclePairingFallback(FallbackSetting.PAIRING_SMART));
                pairingFallbackButton.active = remoteInstalled();
            }
        } else if (session.page() == PortalConfigPage.REMOTE_SETTINGS) {
            int maximum = Math.max(1,
                PortalClientState.gun().maximumSurfaceRange());
            addRenderableWidget(new GunDistanceSlider(x + 18, gunSettingControlY, fieldWidth, 18,
                DistanceSetting.REMOTE_DISTANCE, "screen.riftgun.remote_distance_value", 1, maximum,
                PortalClientState.gun().remoteDistance()));
            remoteScrollAdjustmentButton = button(x + 18, gunSettingControlY + 24, fieldWidth, 19,
                toggleLabel("screen.riftgun.remote.scroll_adjustment",
                    PortalClientState.gun().remoteScrollAdjustmentEnabled(),
                    PortalInputLabels.sneakKey()),
                false, ignored -> toggleGunBoolean(BooleanSetting.REMOTE_SCROLL_ADJUSTMENT));
            remoteRadialSliderButton = button(x + 18, gunSettingControlY + 48, fieldWidth, 19,
                toggleLabel("screen.riftgun.remote.radial_slider",
                    PortalClientState.gun().remoteRadialSliderEnabled()),
                false, ignored -> toggleGunBoolean(BooleanSetting.REMOTE_RADIAL_SLIDER));
            remotePlacementPreviewButton = button(x + 18, gunSettingControlY + 72, fieldWidth, 19,
                toggleLabel("screen.riftgun.remote.placement_preview",
                    PortalClientState.gun().remotePreviewEnabled()),
                false, ignored -> toggleGunBoolean(BooleanSetting.REMOTE_PLACEMENT_PREVIEW));
        } else if (session.page() == PortalConfigPage.VISUAL_SETTINGS) {
            addVisualSelector(x + 18, y + 51, fieldWidth);
            if (!PortalVisualPreferences.selected().options().isEmpty()) {
                visualAnimationSettingsButton = button(x + fieldWidth - 2, y + 76, 20, 18,
                    Component.empty(), false, ignored -> openSwirlAnimationSettings());
            }
        } else if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {
            addVisualOptionWidgets(box, fieldWidth);
        } else if (session.page() == PortalConfigPage.SOUND_SETTINGS) {
            int selectorY = y + 34;
            for (PortalSoundChannel channel : PortalSoundChannel.values()) {
                addSoundSelector(channel, x + 18, selectorY, fieldWidth);
                selectorY += 24;
            }
            splashSoundButton = button(x + 18, selectorY, fieldWidth, 18,
                toggleLabel("screen.riftgun.sound.splash",
                    PortalClientState.data().settings().portalSounds().splashEnabled()), false,
                ignored -> toggleSplashSound());
        }

        int actionY = y + box.height() - 27;
        if (session.page().isConfirmation()) {
            button(x + 18, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.cancel", false, ignored -> cancelConfirmation());
            button(x + 24 + (box.width() - 42) / 2, actionY, (box.width() - 42) / 2, 19,
                "screen.riftgun.confirm", false, ignored -> acceptConfirmation());
        } else if (session.page() == PortalConfigPage.SHARE_DESTINATION) {
            button(x + 18, actionY, fieldWidth, 19, "screen.riftgun.cancel", false,
                ignored -> closeModalNow());
        } else if (session.page() == PortalConfigPage.SETTINGS) {
            button(x + 18, actionY, fieldWidth, 19, "screen.riftgun.done", false,
                ignored -> closeModalNow());
        } else if (session.page() == PortalConfigPage.GUN_SETTINGS) {
            gunSettingsBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> closeModalNow());
        } else if (session.page().isGunSettingPage()) {
            moduleSettingBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToGunSettings());
        } else if (session.page() == PortalConfigPage.CONFIRM_SETTINGS || session.page() == PortalConfigPage.MAP_INTEGRATION_SETTINGS) {
            button(x + 18, actionY, 24, 19, Component.literal("←"), false,
                ignored -> backToSettings());
        } else if (session.page() == PortalConfigPage.VISUAL_SETTINGS) {
            visualBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToSettings());
        } else if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {
            swirlAnimationBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToVisualSettings());
        } else if (session.page() == PortalConfigPage.SOUND_SETTINGS) {
            soundBackButton = button(x + 18, actionY, 24, 19, Component.empty(), false,
                ignored -> backToSettings());
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
                ignored -> toggleGunBoolean(BooleanSetting.PASSIVE_TRANSIT));
            buttonX += 31;
        }
        if (moduleCount("HOSTILE_TRANSIT") > 0) {
            hostileTransitButton = button(buttonX, y, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.HOSTILE_TRANSIT));
            buttonX += 31;
        }
        if (moduleCount("BOSS_TRANSIT") > 0) {
            bossTransitButton = button(buttonX, y, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.BOSS_TRANSIT));
        }
        if (moduleCount("PROJECTILE_TRANSIT") > 0) {
            projectileTransitButton = button(x, y + 31, 26, 26, Component.empty(), false,
                ignored -> toggleGunBoolean(BooleanSetting.PROJECTILE_TRANSIT));
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

    private void addSoundSelector(PortalSoundChannel channel, int x, int y, int width) {
        ThemedButton selector = button(x, y, width - 22, 18, soundName(channel), false,
            ignored -> openSoundDropdown(channel));
        ThemedButton dropdown = button(x + width - 20, y, 20, 18, Component.empty(), false,
            ignored -> openSoundDropdown(channel));
        soundSelectors.put(channel, selector);
        soundDropdownButtons.put(channel, dropdown);
    }

    private void addVisualOptionWidgets(Box box, int width) {
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

    private void layoutVisualOptionWidgets(Box box) {
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
            session.markDirty();
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.fill(panelX + listWidth, panelY, panelX + listWidth + 1,
            panelY + panelHeight - footerHeight(), PortalTheme.BORDER);
        graphics.fill(panelX, panelY + HEADER_HEIGHT - 1, panelX + panelWidth,
            panelY + HEADER_HEIGHT, PortalTheme.BORDER);
        graphics.fill(panelX, listBottom, panelX + panelWidth, listBottom + 1, PortalTheme.BORDER);
        graphics.text(font, title, panelX + 10, panelY + 8, PortalTheme.TEXT, false);

        int backgroundMouseX = session.page() == PortalConfigPage.NONE ? mouseX : Integer.MIN_VALUE;
        int backgroundMouseY = session.page() == PortalConfigPage.NONE ? mouseY : Integer.MIN_VALUE;
        renderRows(graphics, backgroundMouseX, backgroundMouseY);
        renderDetails(graphics, backgroundMouseX, backgroundMouseY);
        if (session.page() != PortalConfigPage.NONE) {
            renderModal(graphics);
        }
        for (Renderable renderable : renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        renderVisualOptionWidgets(graphics, mouseX, mouseY, partialTick);
        if (groupDropdownOpen) renderGroupDropdown(graphics, mouseX, mouseY);
        if (visualDropdownOpen) renderVisualDropdown(graphics, mouseX, mouseY);
        if (soundDropdownChannel != null) renderSoundDropdown(graphics, mouseX, mouseY);
        renderPlacementIcons(graphics, mouseX, mouseY);
        renderPlacementTooltips(graphics, mouseX, mouseY);
    }

    private void renderRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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
                || row.kind() == RowKind.EXTERNAL_DESTINATION && row.id().equals(selectedExternalRow)
                || row.kind() == RowKind.PLAYER && row.id().equals(playerTargets.selectedId());
            hitRows.add(new Row(row.kind(), row.id(), y));
            if (row.kind() == RowKind.GROUP) visibleGroupRows.put(row.id(), y);
            if (selected) graphics.fill(panelX + 4, y, panelX + listWidth - 4, y + ROW_HEIGHT, 0x663F7180);
            else if (hover || focused) graphics.fill(panelX + 4, y, panelX + listWidth - 4,
                y + ROW_HEIGHT, 0x5530333A);
            if (focused) graphics.outline(panelX + 4, y, listWidth - 8, ROW_HEIGHT, PortalTheme.BORDER_FOCUS);
            if (row.kind() == RowKind.GROUP) renderGroupRow(graphics, row.id(), y, hover, focused);
            else if (row.kind() == RowKind.EXTERNAL_GROUP) {
                renderExternalGroupRow(graphics, row.id(), y);
            } else if (row.kind() == RowKind.EXTERNAL_DESTINATION) {
                renderExternalDestinationRow(graphics, row.id(), y, hover, focused, mouseX, mouseY);
            }
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
                graphics.outline(panelX + 4, groupY, listWidth - 8, ROW_HEIGHT,
                    PortalTheme.BORDER_FOCUS);
            }
        }
        animatedRowY.keySet().retainAll(liveIds);
        graphics.disableScissor();
        renderScrollbar(graphics, panelX + listWidth - 3, listTop, listBottom,
            listScroll, listContentHeight, listViewportHeight());
    }

    private void renderGroupRow(GuiGraphicsExtractor graphics, UUID id, int y, boolean hover, boolean focused) {
        PortalPlayerData data = PortalClientState.data();
        boolean shared = id.equals(PortalPlayerData.SHARED_SECTION_ID);
        boolean custom = !id.equals(PortalPlayerData.DEFAULT_GROUP_ID) && !shared;
        boolean expanded = data.expandedGroups().contains(id);
        String name = shared ? Component.translatable("screen.riftgun.shared_group").getString()
            : custom ? data.group(id).map(DestinationGroup::name).orElse("?")
            : Component.translatable("screen.riftgun.default_group").getString();
        if (custom) drawDragHandle(graphics, panelX + 8, y + 5);
        drawDisclosure(graphics, panelX + 17, y + 6, expanded);
        int right = panelX + listWidth - 6;
        boolean actions = custom && (hover || focused);
        int reserved = actions ? 34 : 20;
        graphics.text(font, trim(name, listWidth - 34 - reserved), panelX + 28, y + 5,
            PortalTheme.TEXT, false);
        if (actions) {
            drawPencil(graphics, right - 27, y + 5, PortalTheme.ICE);
            drawCross(graphics, right - 11, y + 5, PortalTheme.DANGER);
        } else {
            long count = data.destinations().stream().filter(destination -> destination.groupId().equals(id)).count();
            String countText = Long.toString(count);
            graphics.text(font, countText, right - font.width(countText), y + 5,
                PortalTheme.TEXT_MUTED, false);
        }
    }

    private void renderPlayerSectionRow(GuiGraphicsExtractor graphics, UUID id, int y, boolean hover, boolean focused) {
        boolean expanded = playerTargets.expanded();
        drawDisclosure(graphics, panelX + 17, y + 6, expanded);
        int right = panelX + listWidth - 6;
        boolean actions = hover || focused;
        String title = Component.translatable("screen.riftgun.player_group").getString();
        if (actions) {
            int refreshRight = right - 11;
            drawPlayerRefreshIcon(graphics, refreshRight - 16, y + 5);
            graphics.text(font, trim(title, listWidth - 34 - 36), panelX + 28, y + 5,
                PortalTheme.ICE, false);
        } else {
            long count = PlayerListState.players().size();
            String countText = Long.toString(count);
            graphics.text(font, trim(title, listWidth - 34 - 20), panelX + 28, y + 5,
                PortalTheme.ICE, false);
            graphics.text(font, countText, right - font.width(countText), y + 5,
                PortalTheme.TEXT_MUTED, false);
        }
    }

    private void renderExternalGroupRow(GuiGraphicsExtractor graphics, UUID id, int y) {
        ExternalDestinationSource source = externalSource(id);
        if (source == null) return;
        drawDisclosure(graphics, panelX + 17, y + 6, expandedExternalGroups.contains(source));
        int right = panelX + listWidth - 6;
        String count = Integer.toString(ClientMapWaypointIntegration.catalog().destinations(source).size());
        graphics.text(font, trim(source.displayName(), listWidth - 54), panelX + 28, y + 5,
            PortalTheme.ICE, false);
        graphics.text(font, count, right - font.width(count), y + 5, PortalTheme.TEXT_MUTED, false);
    }

    private void renderExternalDestinationRow(GuiGraphicsExtractor graphics, UUID id, int y,
                                               boolean hover, boolean focused, int mouseX, int mouseY) {
        ExternalDestination destination = externalRows.get(id);
        if (destination == null) return;
        int nameX = panelX + 23;
        int nameWidth = listWidth - 49;
        int color = !destination.selectable() ? PortalTheme.TEXT_MUTED
            : id.equals(selectedExternalRow) ? PortalTheme.ICE : PortalTheme.TEXT;
        drawDestinationDragDot(graphics, panelX + 12, y + 8,
            destination.selectable() ? (hover || focused ? PortalTheme.TEXT_MUTED : 0xFF50535A)
                : 0xFF3E4148);
        graphics.text(font, trim(destination.name(), nameWidth), nameX, y + 5, color, false);
    }

    private void renderPlayerRow(GuiGraphicsExtractor graphics, UUID id, int y, boolean hover, boolean focused,
                                 int mouseX, int mouseY) {
        PlayerListState.PlayerEntry entry = PlayerListState.player(id);
        if (entry == null) return;
        int right = panelX + listWidth - 6;
        int starLeft = right - ROW_ACTION_SIZE - 2;
        boolean selected = id.equals(playerTargets.selectedId());
        String name = entry.name();
        if (!entry.self()) {
            String localDimension = minecraft != null && minecraft.player != null
                ? minecraft.player.level().dimension().identifier().toString() : "";
            if (!entry.dimension().equals(localDimension)) {
                String dim = displayDimension(entry.dimension());
                name = name + " (" + dim + ")";
            }
        }
        int nameX = panelX + 23;
        int nameWidth = starLeft - nameX - 12;
        String shown = trim(name, nameWidth);
        drawDestinationDragDot(graphics, panelX + 12, y + 8,
            hover || focused ? PortalTheme.TEXT_MUTED : 0xFF50535A);
        graphics.text(font, shown, nameX, y + 5,
            selected ? PortalTheme.ICE : (entry.self() ? PortalTheme.TEXT_MUTED : PortalTheme.TEXT), false);
        drawStar(graphics, starLeft + 4, y + 5, entry.pinned());
        if (hover && mouseX >= nameX && mouseX < starLeft && font.width(name) > nameWidth) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(name)), mouseX, mouseY);
        }
    }

    private void renderDestinationRow(GuiGraphicsExtractor graphics, UUID id, int y, boolean hover, boolean focused,
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
            graphics.text(font, "!", starLeft - 10, y + 5, PortalTheme.WARNING, false);
            nameWidth -= 10;
        }
        String shown = trim(destination.name(), nameWidth);
        int dotColor = destinationDragActive && id.equals(draggingDestination)
            ? PortalTheme.ICE : hover || focused ? PortalTheme.TEXT_MUTED : 0xFF50535A;
        drawDestinationDragDot(graphics, panelX + 12, y + 8, dotColor);
        graphics.text(font, shown, nameX, y + 5, target ? PortalTheme.ICE : PortalTheme.TEXT_MUTED, false);
        drawStar(graphics, starLeft + 4, y + 5, destination.pinned());
        if (hover || focused) drawCross(graphics, deleteLeft + 3, y + 5, PortalTheme.DANGER);
        if (hover && mouseX >= nameX && mouseX < starLeft && font.width(destination.name()) > nameWidth) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(destination.name())), mouseX, mouseY);
        }
    }

    private void renderDetails(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int left = panelX + listWidth + 1;
        int right = panelX + panelWidth;
        int viewport = listViewportHeight();
        int x = left + 8;
        int y = listTop - detailScroll + 8;
        int contentStart = y;
        detailEditY = -1;
        detailShareY = -1;
        graphics.enableScissor(left + 1, listTop, right - 1, listBottom);
        drawDetailText(graphics, Component.translatable("screen.riftgun.details"),
            x, right - 8, y, PortalTheme.TEXT_MUTED, false);
        y += 19;
        Destination destination = viewed();
        ExternalDestination external = viewedExternal();
        if (playerTargets.selectedId() != null) {
            PlayerListState.PlayerEntry entry = PlayerListState.player(playerTargets.selectedId());
            if (entry == null) {
                playerTargets.clearSelection();
            } else {
                int textWidth = panelWidth - listWidth - 20;
                y = detailField(graphics, "screen.riftgun.name", entry.name(), x, y, textWidth);
                y = detailField(graphics, "screen.riftgun.group", Component.translatable("screen.riftgun.player_group"),
                    x, y, textWidth);
                String dimension = displayDimension(entry.dimension());
                int dimensionY = y;
                y = detailField(graphics, "screen.riftgun.dimension", dimension, x, y, textWidth);
                if (mouseX >= x && mouseX < right - 6
                    && mouseY >= dimensionY + 9 && mouseY < dimensionY + 23) {
                    graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(dimension)), mouseX, mouseY);
                }
                if (minecraft != null && minecraft.player != null
                    && !entry.dimension().equals(minecraft.player.level().dimension().identifier().toString())
                    && !hasCrossDimensionFuel()) {
                    drawDetailText(graphics,
                        Component.translatable("screen.riftgun.cross_dimension_fuel_required"),
                        x, right - 8, y, PortalTheme.WARNING, false);
                    y += 18;
                }
                y = detailField(graphics, "screen.riftgun.coordinates", "—", x, y, textWidth);
                if (entry.self()) {
                    drawDetailText(graphics, Component.translatable("screen.riftgun.player_self"),
                        x, right - 8, y, PortalTheme.TEXT_MUTED, false);
                    y += 18;
                }
                detailEditY = -1;
            }
        } else if (external != null) {
            int textWidth = panelWidth - listWidth - 20;
            y = detailField(graphics, "screen.riftgun.name", external.name(), x, y, textWidth);
            String sourceGroup = external.sourceGroup().isBlank() ? external.source().displayName()
                : external.source().displayName() + " - " + external.sourceGroup();
            y = detailField(graphics, "screen.riftgun.group", sourceGroup, x, y, textWidth);
            String dimension = displayDimension(external.dimensionId());
            y = detailField(graphics, "screen.riftgun.dimension", dimension, x, y, textWidth);
            y = detailField(graphics, "screen.riftgun.coordinates",
                String.format(Locale.ROOT, "%.1f  %.1f  %.1f", external.x(), external.y(), external.z()),
                x, y, textWidth);
            drawDetailText(graphics, Component.translatable("screen.riftgun.external_read_only"),
                x, right - 8, y, PortalTheme.TEXT_MUTED, false);
            y += 18;
            if (!external.selectable()) {
                drawDetailText(graphics,
                    Component.translatable("screen.riftgun.external_unknown_dimension"),
                    x, right - 8, y, PortalTheme.WARNING, false);
                y += 18;
            }
        } else if (destination == null) {
            drawDetailText(graphics, Component.translatable("screen.riftgun.empty_details"),
                x, right - 8, y, PortalTheme.TEXT_MUTED, false);
            y += 22;
        } else {
            int textWidth = panelWidth - listWidth - 20;
            y = detailField(graphics, "screen.riftgun.name", destination.name(), x, y, textWidth);
            y = detailField(graphics, "screen.riftgun.group", groupName(destination.groupId()), x, y, textWidth);
            String dimension = displayDimension(destination.dimension().identifier().toString());
            int dimensionY = y;
            y = detailField(graphics, "screen.riftgun.dimension", dimension, x, y, textWidth);
            if (mouseX >= x && mouseX < right - 6
                && mouseY >= dimensionY + 9 && mouseY < dimensionY + 23) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.literal(dimension)), mouseX, mouseY);
            }
            if (minecraft != null && minecraft.player != null
                && !minecraft.player.level().dimension().equals(destination.dimension())
                && !hasCrossDimensionFuel()) {
                drawDetailText(graphics,
                    Component.translatable("screen.riftgun.cross_dimension_fuel_required"),
                    x, right - 8, y, PortalTheme.WARNING, false);
                y += 18;
            }
            y = detailField(graphics, "screen.riftgun.coordinates", String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
                destination.x(), destination.y(), destination.z()), x, y, textWidth);
            detailEditY = session.page() == PortalConfigPage.NONE ? y : -1;
            if (session.page() == PortalConfigPage.NONE) {
                graphics.fill(x, y, right - 8, y + 18, PortalTheme.PANEL_RAISED);
                graphics.outline(x, y, right - x - 8, 18, PortalTheme.BORDER);
                drawDetailText(graphics, Component.translatable("screen.riftgun.edit"),
                    x + 2, right - 10, y + 5, PortalTheme.TEXT, true);
            }
            y += 22;
            detailShareY = session.page() == PortalConfigPage.NONE ? y : -1;
            if (session.page() == PortalConfigPage.NONE) {
                graphics.fill(x, y, right - 8, y + 18, PortalTheme.PANEL_RAISED);
                graphics.outline(x, y, right - x - 8, 18, PortalTheme.BORDER);
                drawDetailText(graphics, Component.translatable("screen.riftgun.share"),
                    x + 2, right - 10, y + 5, PortalTheme.TEXT, true);
            }
            y += 26;
        }
        detailContentHeight = Math.max(viewport, y - contentStart + 8);
        detailScroll = Mth.clamp(detailScroll, 0, detailMaxScroll());
        graphics.disableScissor();
        renderScrollbar(graphics, right - 3, listTop, listBottom,
            detailScroll, detailContentHeight, viewport);
    }

    private int detailField(GuiGraphicsExtractor graphics, String labelKey, String value, int x, int y, int width) {
        drawDetailText(graphics, Component.translatable(labelKey), x, x + width, y,
            PortalTheme.TEXT_MUTED, false);
        drawDetailText(graphics, Component.literal(value), x, x + width, y + 11,
            PortalTheme.TEXT, false);
        return y + DETAIL_LINE_HEIGHT;
    }

    private int detailField(GuiGraphicsExtractor graphics, String labelKey, Component value, int x, int y, int width) {
        drawDetailText(graphics, Component.translatable(labelKey), x, x + width, y,
            PortalTheme.TEXT_MUTED, false);
        drawDetailText(graphics, value, x, x + width, y + 11, PortalTheme.TEXT, false);
        return y + DETAIL_LINE_HEIGHT;
    }

    private void drawDetailText(GuiGraphicsExtractor graphics, Component text, int left, int right,
                                int y, int color, boolean centered) {
        int availableWidth = Math.max(0, right - left);
        if (availableWidth == 0) return;
        int textWidth = font.width(text);
        int offset = GuiTextMarquee.offset(textWidth, availableWidth, Util.getMillis());
        int textX = centered && textWidth <= availableWidth
            ? left + (availableWidth - textWidth) / 2
            : left - offset;
        graphics.enableScissor(left, y - 1, right, y + 10);
        graphics.text(font, text, textX, y, color, false);
        graphics.disableScissor();
    }

    private void renderModal(GuiGraphicsExtractor graphics) {
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xB8101115);
        Box box = modalBox();
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.PANEL_RAISED);
        graphics.outline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        graphics.text(font, Component.translatable(session.page().titleKey()), box.x() + 18, box.y() + 13,
            PortalTheme.TEXT, false);

        int x = box.x() + 18;
        int y = box.y();
        if (session.page() == PortalConfigPage.CREATE_COORDINATE || session.page() == PortalConfigPage.EDIT_DESTINATION) {
            label(graphics, "screen.riftgun.name", x, y + 29);
            label(graphics, "screen.riftgun.x", x, y + 68);
            label(graphics, "screen.riftgun.y", x + (box.width() - 26) / 2, y + 68);
            label(graphics, "screen.riftgun.z", x, y + 107);
            label(graphics, "screen.riftgun.yaw", x + (box.width() - 26) / 2, y + 107);
            label(graphics, "screen.riftgun.group", x, y + 146);
        } else if (session.page() == PortalConfigPage.CREATE_CURRENT) {
            label(graphics, "screen.riftgun.name", x, y + 29);
            label(graphics, "screen.riftgun.coordinates", x, y + 68);
            if (minecraft != null && minecraft.player != null) {
                graphics.text(font, String.format(Locale.ROOT, "%.1f  %.1f  %.1f",
                    minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()),
                    x, y + 81, PortalTheme.TEXT_MUTED, false);
            }
            label(graphics, "screen.riftgun.group", x, y + 99);
        } else if (session.page() == PortalConfigPage.CREATE_GROUP || session.page() == PortalConfigPage.RENAME_GROUP) {
            label(graphics, "screen.riftgun.name", x, y + 32);
        } else if (session.page() == PortalConfigPage.GUN_SETTINGS) {
            graphics.text(font, Component.translatable("screen.riftgun.gun_settings_hint"),
                x, y + 30, PortalTheme.TEXT_MUTED, false);
        } else if (session.page().isGunSettingPage()) {
            drawGunSettingDescription(graphics, box);
        } else if (session.page() == PortalConfigPage.MAP_INTEGRATION_SETTINGS) {
            int statusY = y + 104;
            for (ExternalDestinationSource source : ExternalDestinationSource.values()) {
                var result = ClientMapWaypointIntegration.catalog().readResult(source);
                if (result == null || result.status()
                    == dev.riftgun.external.client.ExternalDestinationReadResult.Status.AVAILABLE) continue;
                graphics.text(font, Component.translatable("screen.riftgun.map.incompatible",
                    source.displayName(), result.installedVersion()), x, statusY,
                    PortalTheme.WARNING, false);
                statusY += 12;
            }
        } else if (session.page() == PortalConfigPage.VISUAL_SETTINGS) {
            label(graphics, "screen.riftgun.portal_visual", x, y + 34);
        } else if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {
            renderVisualOptionsChrome(graphics, box);
        } else if (session.page().isConfirmation()) {
            Component body = session.page() == PortalConfigPage.CONFIRM_CLEAR_FLUID
                ? Component.translatable(session.page().bodyKey(), gunFluidName(), PortalClientState.gun().amount())
                : Component.translatable(session.page().bodyKey());
            graphics.textWithWordWrap(font, body, x, y + 35,
                box.width() - 36, PortalTheme.TEXT_MUTED);
        }
    }

    private void renderVisualOptionsChrome(GuiGraphicsExtractor graphics, Box box) {
        PortalVisualOptions options = PortalVisualPreferences.selected().options();
        if (options.isEmpty()) return;
        int top = visualOptionsTop(box);
        int bottom = visualOptionsBottom(box);
        int headerY = top + 4 - visualOptionsScroll;
        graphics.enableScissor(box.x() + 17, top, box.x() + box.width() - 17, bottom);
        graphics.text(font, Component.translatable(options.sectionTitleKey()), box.x() + 18,
            headerY, PortalTheme.TEXT_MUTED, false);
        graphics.disableScissor();
        renderScrollbar(graphics, box.x() + box.width() - 20, top, bottom,
            visualOptionsScroll, visualOptionsContentHeight, visualOptionsViewportHeight(box));
    }

    private void renderVisualOptionWidgets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (session.page() != PortalConfigPage.SWIRL_ANIMATION_SETTINGS || visualOptionWidgets.isEmpty()) return;
        Box box = modalBox();
        int top = visualOptionsTop(box);
        int bottom = visualOptionsBottom(box);
        int effectiveMouseX = visualDropdownOpen ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = visualDropdownOpen ? Integer.MIN_VALUE : mouseY;
        graphics.enableScissor(box.x() + 17, top, box.x() + box.width() - 17, bottom);
        for (VisualWidgetBinding binding : visualOptionWidgets) {
            if (binding.widget().visible) {
                binding.widget().extractRenderState(graphics, effectiveMouseX, effectiveMouseY, partialTick);
            }
        }
        graphics.disableScissor();
    }

    private void renderPlacementIcons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (mapRefreshButton != null && mapRefreshButton.visible) {
            // The shared helper subtracts 4; these anchors center its 16 px sprite in 19 x 17.
            drawPlayerRefreshIcon(graphics, mapRefreshButton.getX() + 5,
                mapRefreshButton.getY() + 4);
        }
        if (session.page().isDestinationForm() && groupDropdownButton != null) {
            drawDownIcon(graphics, groupDropdownButton.getX() + 6, groupDropdownButton.getY() + 7);
        }
        if (session.page() == PortalConfigPage.NONE && placementModeButton != null) {
            renderMainModuleIcons(graphics);
            if (motionPredictionButton != null) {
                boolean active = PortalClientState.data().settings().predictionMode()
                    != dev.riftgun.data.PortalPredictionMode.OFF;
                drawPredictionIcon(graphics, motionPredictionButton.getX() + 5,
                    motionPredictionButton.getY() + 5, active ? PortalTheme.ICE : PortalTheme.TEXT_MUTED);
            }
            if (functionModeButton != null) {
                drawFunctionModeIcon(graphics, functionModeButton.getX(), functionModeButton.getY(),
                    functionModeButton.getWidth(), functionModeButton.getHeight(),
                    PortalClientState.gun().functionMode() == PortalFunctionMode.PORTAL_PAIRING);
            }
            drawPlacementModeButtonIcon(graphics, placementModeButton.getX(),
                placementModeButton.getY(), PortalClientState.data().settings().placementMode());
            renderGunControls(graphics, mouseX, mouseY);
        }
        if (session.page() == PortalConfigPage.SETTINGS) {
            if (visualSettingsButton != null) {
                drawEyeIcon(graphics, visualSettingsButton.getX() + 5, visualSettingsButton.getY() + 5);
            }
            if (soundSettingsButton != null) {
                drawSoundIcon(graphics, soundSettingsButton.getX() + 5, soundSettingsButton.getY() + 5);
            }
        }
        if (session.page() == PortalConfigPage.GUN_SETTINGS) {
            renderGunSettingEntries(graphics);
            renderBackButton(graphics, gunSettingsBackButton);
        }
        if (session.page().isGunSettingPage()) {
            renderBackButton(graphics, moduleSettingBackButton);
            if (session.page() == PortalConfigPage.ENTITY_TRANSIT_SETTINGS) {
                renderEntityTransitButtons(graphics);
            } else if (session.page() == PortalConfigPage.PLAYER_TARGET_SETTINGS) {
                renderPlayerTargetButtons(graphics);
            } else if (session.page() == PortalConfigPage.APERTURE_SETTINGS && apertureToggleButton != null) {
                boolean enabled = PortalClientState.gun().expandedApertureEnabled();
                drawApertureIcon(graphics, apertureToggleButton.getX() + 7,
                    apertureToggleButton.getY() + 7, enabled);
            } else if (session.page() == PortalConfigPage.FALL_GUARD_SETTINGS) {
                if (fallGuardToggleButton != null) {
                    boolean enabled = PortalClientState.gun().fallGuardEnabled();
                    drawFallGuardIcon(graphics, fallGuardToggleButton.getX() + 7,
                        fallGuardToggleButton.getY() + 7, enabled);
                }
                if (entityFallGuardToggleButton != null) {
                    boolean enabled = PortalClientState.gun().entityFallGuardEnabled();
                    drawEntityFallGuardIcon(graphics, entityFallGuardToggleButton.getX() + 5,
                        entityFallGuardToggleButton.getY() + 5, enabled);
                }
            } else if (session.page() == PortalConfigPage.ENTITY_RELOCATION_SETTINGS) {
                if (entityRelocationEnabledButton != null) {
                    boolean enabled = PortalClientState.gun().entityRelocationEnabled();
                    drawEntityRelocationIcon(graphics, entityRelocationEnabledButton.getX() + 7,
                        entityRelocationEnabledButton.getY() + 7, enabled);
                }
                if (entityRelocationSmartButton != null) {
                    boolean enabled = PortalClientState.gun().entityRelocationSmartRouting();
                    drawEntityRelocationSmartIcon(graphics, entityRelocationSmartButton.getX() + 7,
                        entityRelocationSmartButton.getY() + 7, enabled);
                }
            }
        }
        if (session.page() == PortalConfigPage.VISUAL_SETTINGS) {
            if (visualBackButton != null) {
                drawCompactBackButtonIcon(graphics, visualBackButton.getX(), visualBackButton.getY());
            }
            if (visualDropdownButton != null) {
                drawDownIcon(graphics, visualDropdownButton.getX() + 6, visualDropdownButton.getY() + 7);
            }
            if (!visualDropdownOpen && visualAnimationSettingsButton != null) {
                drawSwirlIcon(graphics, visualAnimationSettingsButton.getX() + 5,
                    visualAnimationSettingsButton.getY() + 5, PortalTheme.ICE);
            }
        }
        if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {
            if (swirlAnimationBackButton != null) {
                drawCompactBackButtonIcon(graphics, swirlAnimationBackButton.getX(),
                    swirlAnimationBackButton.getY());
            }
            if (visualResetButton != null && visualResetButton.visible) {
                drawResetIcon(graphics, visualResetButton.getX() + 5, visualResetButton.getY() + 4,
                    visualResetButton.active ? PortalTheme.ICE : PortalTheme.TEXT_MUTED);
            }
        }
        if (session.page() == PortalConfigPage.SOUND_SETTINGS) {
            if (soundBackButton != null) {
                drawCompactBackButtonIcon(graphics, soundBackButton.getX(), soundBackButton.getY());
            }
            for (ThemedButton dropdown : soundDropdownButtons.values()) {
                drawDownIcon(graphics, dropdown.getX() + 6, dropdown.getY() + 7);
            }
        }
    }

    /** Rendered last so icons never cover tooltip text. */
    private void renderPlacementTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (session.page().isDestinationForm() && groupDropdownButton != null) {
            // no tooltip
        }
        if (session.page() == PortalConfigPage.NONE && placementModeButton != null) {
            if (gunSettingsButton != null && gunSettingsButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.configure_gun")), mouseX, mouseY);
            }
            if (moduleBayButton != null && moduleBayButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.open_modules")), mouseX, mouseY);
            }
            if (closePortalsButton != null && closePortalsButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.close_portals")), mouseX, mouseY);
            }
            if (dimensionalTraversalButton != null && dimensionalTraversalButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    "screen.riftgun.dimensional_navigation")), mouseX, mouseY);
            }
            if (motionPredictionButton != null) {
                dev.riftgun.data.PortalPredictionMode mode =
                    PortalClientState.data().settings().predictionMode();
                if (motionPredictionButton.isHovered()) {
                    String modeKey = "screen.riftgun.prediction."
                        + mode.name().toLowerCase(Locale.ROOT);
                    graphics.setComponentTooltipForNextFrame(font, List.of(
                        Component.translatable("screen.riftgun.motion_prediction_tooltip",
                            Component.translatable(modeKey)),
                        Component.translatable(modeKey + ".description")
                    ), mouseX, mouseY);
                }
            }
            if (placementModeButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.placement_mode_tooltip",
                    Component.translatable("screen.riftgun.placement_mode."
                        + PortalClientState.data().settings().placementMode().name().toLowerCase(Locale.ROOT)))), mouseX, mouseY);
            }
            if (functionModeButton != null && functionModeButton.isHovered()) {
                boolean pairing = PortalClientState.gun().functionMode()
                    == PortalFunctionMode.PORTAL_PAIRING;
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    "screen.riftgun.pairing_mode_tooltip",
                    Component.translatable(pairing ? "screen.riftgun.on" : "screen.riftgun.off"))),
                    mouseX, mouseY);
            }
            if (openPortalButton != null && openPortalButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.open_front_tooltip")), mouseX, mouseY);
            }
            if (randomRiftButton != null && randomRiftButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font,
                    randomRiftTooltip(), mouseX, mouseY);
            }
            renderGunControlTooltips(graphics, mouseX, mouseY);
        }
        if (session.page() == PortalConfigPage.SETTINGS) {
            if (visualSettingsButton != null && visualSettingsButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.visual_settings")), mouseX, mouseY);
            }
            if (soundSettingsButton != null && soundSettingsButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.sound_settings")), mouseX, mouseY);
            }
        }
        if (session.page() == PortalConfigPage.GUN_SETTINGS) {
            renderGunSettingTooltips(graphics, mouseX, mouseY);
            renderBackButtonTooltip(graphics, gunSettingsBackButton, mouseX, mouseY,
                "screen.riftgun.back");
        }
        if (session.page().isGunSettingPage()) {
            renderBackButtonTooltip(graphics, moduleSettingBackButton, mouseX, mouseY,
                "screen.riftgun.back_to_gun_settings");
            if (session.page() == PortalConfigPage.ENTITY_TRANSIT_SETTINGS) {
                renderEntityTransitTooltips(graphics, mouseX, mouseY);
            } else if (session.page() == PortalConfigPage.PLAYER_TARGET_SETTINGS) {
                renderPlayerTargetTooltips(graphics, mouseX, mouseY);
            } else if (session.page() == PortalConfigPage.APERTURE_SETTINGS && apertureToggleButton != null) {
                boolean enabled = PortalClientState.gun().expandedApertureEnabled();
                entityTooltip(graphics, apertureToggleButton,
                    "screen.riftgun.aperture", enabled, mouseX, mouseY);
            } else if (session.page() == PortalConfigPage.FALL_GUARD_SETTINGS) {
                if (fallGuardToggleButton != null) {
                    boolean enabled = PortalClientState.gun().fallGuardEnabled();
                    entityTooltip(graphics, fallGuardToggleButton,
                        "screen.riftgun.fall_guard", enabled, mouseX, mouseY);
                }
                if (entityFallGuardToggleButton != null) {
                    boolean enabled = PortalClientState.gun().entityFallGuardEnabled();
                    entityTooltip(graphics, entityFallGuardToggleButton,
                        "screen.riftgun.fall_guard_entities", enabled, mouseX, mouseY);
                }
            } else if (session.page() == PortalConfigPage.ENTITY_RELOCATION_SETTINGS) {
                if (entityRelocationEnabledButton != null) {
                    entityTooltip(graphics, entityRelocationEnabledButton,
                        "screen.riftgun.entity_relocation_enabled",
                        PortalClientState.gun().entityRelocationEnabled(), mouseX, mouseY);
                }
                if (entityRelocationSmartButton != null) {
                    entityTooltip(graphics, entityRelocationSmartButton,
                        "screen.riftgun.entity_relocation_smart",
                        PortalClientState.gun().entityRelocationSmartRouting(), mouseX, mouseY);
                }
            } else if (session.page() == PortalConfigPage.PORTAL_PAIRING_SETTINGS && pairingFallbackButton != null
                && !remoteInstalled() && pairingFallbackButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font,
                    List.of(Component.translatable("message.riftgun.remote_module_required")), mouseX, mouseY);
            }
        }
        if (session.page() == PortalConfigPage.VISUAL_SETTINGS) {
            if (visualBackButton != null && visualBackButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.back_to_settings")), mouseX, mouseY);
            }
            if (!visualDropdownOpen && visualSelector != null && visualSelector.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                    PortalVisualPreferences.selected().descriptionKey())), mouseX, mouseY);
            }
            if (!visualDropdownOpen && visualAnimationSettingsButton != null
                && visualAnimationSettingsButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.visual.swirl_animation_settings")), mouseX, mouseY);
            }
        }
        if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {
            if (swirlAnimationBackButton != null && swirlAnimationBackButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.visual.back_to_visuals")), mouseX, mouseY);
            }
            if (visualResetButton != null && visualResetButton.visible && visualResetButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(PortalVisualPreferences.selected().options().resetTooltipKey())), mouseX, mouseY);
            }
        }
        if (session.page() == PortalConfigPage.SOUND_SETTINGS && soundBackButton != null && soundBackButton.isHovered()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.back_to_settings")), mouseX, mouseY);
        }
        if (session.page() == PortalConfigPage.EDIT_DESTINATION && !coordinateEditingUnlocked()) {
            for (EditBox field : coordinateEditFields) {
                if (field.isHovered()) {
                    graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(
                        "screen.riftgun.coordinate_read_only")), mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void renderMainModuleIcons(GuiGraphicsExtractor graphics) {
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
        if (dimensionalTraversalButton != null) {
            drawDimensionalTraversalIcon(graphics, dimensionalTraversalButton.getX(),
                dimensionalTraversalButton.getY(), dimensionalTraversalButton.getWidth(),
                dimensionalTraversalButton.getHeight());
        }
        if (randomRiftButton != null) {
            drawRandomRiftIcon(graphics, randomRiftButton.getX(), randomRiftButton.getY(),
                randomRiftButton.getWidth(), randomRiftButton.getHeight(), randomRiftButton.active);
        }
    }

    private void renderGunSettingEntries(GuiGraphicsExtractor graphics) {
        if (portalDurationSettingsButton != null) {
            drawPortalDurationIcon(graphics, portalDurationSettingsButton.getX() + 7,
                portalDurationSettingsButton.getY() + 7);
        }
        if (smartRangeSettingsButton != null) {
            drawSmartDistanceIcon(graphics, smartRangeSettingsButton.getX() + 7,
                smartRangeSettingsButton.getY() + 7, PortalTheme.ICE);
        }
        if (remoteSettingsButton != null) {
            PortalGuiIcons.drawPlacementModeIcon(graphics, remoteSettingsButton.getX() + 5,
                remoteSettingsButton.getY() + 5, PortalPlacementMode.REMOTE);
        }
        if (entityTransitSettingsButton != null) {
            drawEntityAccessIcon(graphics, entityTransitSettingsButton.getX() + 7,
                entityTransitSettingsButton.getY() + 7, PortalTheme.PORTAL);
        }
        if (playerTargetSettingsButton != null) {
            boolean enabled = PortalClientState.gun().playerTargetEnabled();
            drawPlayerTargetIcon(graphics, playerTargetSettingsButton.getX() + 7,
                playerTargetSettingsButton.getY() + 7,
                enabled ? PortalTheme.ICE : PortalTheme.TEXT_MUTED);
        }
        if (apertureSettingsButton != null) {
            boolean enabled = PortalClientState.gun().expandedApertureEnabled();
            drawApertureIcon(graphics, apertureSettingsButton.getX() + 7,
                apertureSettingsButton.getY() + 7, enabled);
        }
        if (fallGuardSettingsButton != null) {
            boolean enabled = PortalClientState.gun().fallGuardEnabled();
            drawFallGuardIcon(graphics, fallGuardSettingsButton.getX() + 7,
                fallGuardSettingsButton.getY() + 7, enabled);
        }
        if (entityRelocationSettingsButton != null) {
            boolean enabled = PortalClientState.gun().entityRelocationEnabled();
            drawEntityRelocationConfigIcon(graphics, entityRelocationSettingsButton.getX() + 7,
                entityRelocationSettingsButton.getY() + 7, enabled);
        }
        if (portalPairingSettingsButton != null) {
            drawFunctionModeIcon(graphics, portalPairingSettingsButton.getX(),
                portalPairingSettingsButton.getY(), portalPairingSettingsButton.getWidth(),
                portalPairingSettingsButton.getHeight(), true);
        }
    }

    private void renderGunSettingTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        settingTooltip(graphics, portalDurationSettingsButton,
            "screen.riftgun.portal_duration", mouseX, mouseY);
        settingTooltip(graphics, smartRangeSettingsButton,
            "screen.riftgun.smart_range", mouseX, mouseY);
        settingTooltip(graphics, remoteSettingsButton,
            "screen.riftgun.remote.settings", mouseX, mouseY);
        settingTooltip(graphics, entityTransitSettingsButton,
            "screen.riftgun.entity_transit", mouseX, mouseY);
        settingTooltip(graphics, playerTargetSettingsButton,
            "screen.riftgun.player_target", mouseX, mouseY);
        settingTooltip(graphics, apertureSettingsButton,
            "screen.riftgun.aperture", mouseX, mouseY);
        settingTooltip(graphics, fallGuardSettingsButton,
            "screen.riftgun.fall_guard", mouseX, mouseY);
        settingTooltip(graphics, entityRelocationSettingsButton,
            "screen.riftgun.entity_relocation", mouseX, mouseY);
        settingTooltip(graphics, portalPairingSettingsButton,
            "screen.riftgun.pairing.settings", mouseX, mouseY);
    }

    private void renderEntityTransitButtons(GuiGraphicsExtractor graphics) {
        if (passiveTransitButton != null) {
            boolean enabled = PortalClientState.gun().passiveTransitEnabled();
            drawPigIcon(graphics, passiveTransitButton.getX() + 6, passiveTransitButton.getY() + 7,
                enabled ? 0xFFA7D79B : PortalTheme.TEXT_MUTED);
        }
        if (hostileTransitButton != null) {
            boolean enabled = PortalClientState.gun().hostileTransitEnabled();
            drawZombieIcon(graphics, hostileTransitButton.getX() + 7, hostileTransitButton.getY() + 7,
                enabled ? 0xFFD98264 : PortalTheme.TEXT_MUTED);
        }
        if (bossTransitButton != null) {
            boolean enabled = PortalClientState.gun().bossTransitEnabled();
            drawDragonIcon(graphics, bossTransitButton.getX() + 6, bossTransitButton.getY() + 7,
                enabled ? 0xFFB38AD8 : PortalTheme.TEXT_MUTED);
        }
        if (projectileTransitButton != null) {
            boolean enabled = PortalClientState.gun().projectileTransitEnabled();
            drawProjectileTransitIcon(graphics,
                projectileTransitButton.getX() + 5, projectileTransitButton.getY() + 5, enabled);
        }
    }

    private void renderEntityTransitTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (passiveTransitButton != null) {
            boolean enabled = PortalClientState.gun().passiveTransitEnabled();
            entityTooltip(graphics, passiveTransitButton, "screen.riftgun.passive_transit", enabled, mouseX, mouseY);
        }
        if (hostileTransitButton != null) {
            boolean enabled = PortalClientState.gun().hostileTransitEnabled();
            entityTooltip(graphics, hostileTransitButton, "screen.riftgun.hostile_transit", enabled, mouseX, mouseY);
        }
        if (bossTransitButton != null) {
            boolean enabled = PortalClientState.gun().bossTransitEnabled();
            entityTooltip(graphics, bossTransitButton, "screen.riftgun.boss_transit", enabled, mouseX, mouseY);
        }
        if (projectileTransitButton != null) {
            boolean enabled = PortalClientState.gun().projectileTransitEnabled();
            entityTooltip(graphics, projectileTransitButton,
                "screen.riftgun.projectile_transit", enabled, mouseX, mouseY);
        }
    }

    private void renderPlayerTargetButtons(GuiGraphicsExtractor graphics) {
        if (playerTargetButton != null) {
            boolean enabled = PortalClientState.gun().playerTargetEnabled();
            drawPlayerTargetIcon(graphics, playerTargetButton.getX() + 7, playerTargetButton.getY() + 7,
                enabled ? 0xFF5CC8D9 : PortalTheme.TEXT_MUTED);
        }
        if (playerExcludeButton != null) {
            int mode = PortalClientState.gun().playerExcludeMode().id();
            drawPlayerExcludeIcon(graphics, playerExcludeButton.getX() + 7, playerExcludeButton.getY() + 7,
                mode == 0 ? PortalTheme.TEXT_MUTED : 0xFF5CC8D9);
        }
    }

    private void renderPlayerTargetTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (playerTargetButton != null) {
            boolean enabled = PortalClientState.gun().playerTargetEnabled();
            entityTooltip(graphics, playerTargetButton, "screen.riftgun.player_target", enabled, mouseX, mouseY);
        }
        if (playerExcludeButton != null) {
            int mode = PortalClientState.gun().playerExcludeMode().id();
            String key = switch (mode) {
                case 0 -> "screen.riftgun.player_exclude_off";
                case 1 -> "screen.riftgun.player_exclude_entry_exit";
                default -> "screen.riftgun.player_exclude_exit_only";
            };
            if (playerExcludeButton.isHovered()) {
                graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(key)), mouseX, mouseY);
            }
        }
    }

    private void settingTooltip(GuiGraphicsExtractor graphics, @Nullable ThemedButton button,
                                String key, int mouseX, int mouseY) {
        if (button != null && button.isHovered()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(key)), mouseX, mouseY);
        }
    }

    private void entityTooltip(GuiGraphicsExtractor graphics, @Nullable ThemedButton button, String key,
                               boolean enabled, int mouseX, int mouseY) {
        if (button != null && button.isHovered()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(key).append(": ").append(
                Component.translatable(enabled ? "screen.riftgun.on" : "screen.riftgun.off"))), mouseX, mouseY);
        }
    }

    private void renderBackButton(GuiGraphicsExtractor graphics, @Nullable ThemedButton button) {
        if (button == null) return;
        drawCompactBackButtonIcon(graphics, button.getX(), button.getY());
    }

    private void renderBackButtonTooltip(GuiGraphicsExtractor graphics, @Nullable ThemedButton button,
                                         int mouseX, int mouseY, String tooltipKey) {
        if (button != null && button.isHovered()) graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable(tooltipKey)), mouseX, mouseY);
    }

    private void renderGunControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderFuelGauge(graphics, mouseX, mouseY);
        if (bucketModeButton != null) {
            int x = bucketModeButton.getX() + 4;
            int y = bucketModeButton.getY() + 4;
            int iconColor = PortalClientState.gun().bucketMode()
                ? PortalTheme.ICE : PortalTheme.TEXT_MUTED;
            drawBucketIcon(graphics, x, y, iconColor);
        }
        if (clearFluidButton != null) {
            drawDrainIcon(graphics, clearFluidButton.getX() + 5, clearFluidButton.getY() + 4,
                clearFluidButton.active ? PortalTheme.DANGER : PortalTheme.TEXT_MUTED);
        }
    }

    private void renderGunControlTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        renderFuelGaugeTooltip(graphics, mouseX, mouseY);
        if (bucketModeButton != null && bucketModeButton.isHovered()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.bucket_mode_simple",
                Component.translatable(PortalClientState.gun().bucketMode()
                    ? "screen.riftgun.on" : "screen.riftgun.off"))), mouseX, mouseY);
        }
        if (clearFluidButton != null && clearFluidButton.isHovered()) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("screen.riftgun.clear_fluid_tooltip")), mouseX, mouseY);
        }
    }

    private void renderFuelGauge(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int amount = PortalClientState.gun().amount();
        int capacity = Math.max(1, PortalClientState.gun().capacity());
        boolean infinite = PortalClientState.gun().infiniteFuel();
        boolean overfilled = amount > capacity;
        int rgb = PortalClientState.gun().fluidRgb();
        int fluidColor = 0xFF000000 | (rgb == 0 ? 0x34363D : rgb);
        graphics.fill(fuelGaugeX, fuelGaugeY, fuelGaugeX + FUEL_GAUGE_WIDTH, fuelGaugeY + 19,
            PortalTheme.FIELD);
        int fillWidth = infinite ? FUEL_GAUGE_WIDTH - 4 : Mth.clamp((int) Math.ceil(Math.min(1.0, amount / (double) capacity)
            * (FUEL_GAUGE_WIDTH - 4)), 0, FUEL_GAUGE_WIDTH - 4);
        graphics.fill(fuelGaugeX + 2, fuelGaugeY + 14,
            fuelGaugeX + FUEL_GAUGE_WIDTH - 2, fuelGaugeY + 17, 0xFF292B31);
        if (fillWidth > 0) graphics.fill(fuelGaugeX + 2, fuelGaugeY + 14,
            fuelGaugeX + 2 + fillWidth, fuelGaugeY + 17, fluidColor);
        graphics.outline(fuelGaugeX, fuelGaugeY, FUEL_GAUGE_WIDTH, 19,
            overfilled ? 0xFFFFAA00 : PortalTheme.BORDER);
        String shortAmount = infinite ? "∞" : shortFluidAmount(amount);
        graphics.centeredText(font, shortAmount, fuelGaugeX + FUEL_GAUGE_WIDTH / 2,
            fuelGaugeY + 3, amount == 0 && !infinite ? PortalTheme.TEXT_MUTED : PortalTheme.TEXT);
    }

    private void renderFuelGaugeTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int amount = PortalClientState.gun().amount();
        int capacity = Math.max(1, PortalClientState.gun().capacity());
        boolean infinite = PortalClientState.gun().infiniteFuel();
        boolean overfilled = amount > capacity;
        if (mouseX >= fuelGaugeX && mouseX < fuelGaugeX + FUEL_GAUGE_WIDTH
            && mouseY >= fuelGaugeY && mouseY < fuelGaugeY + 19) {
            List<Component> tooltip = new ArrayList<>();
            Component fluidName = gunFluidName();
            if (PortalClientState.gun().unstableFuel()) {
                fluidName = fluidName.copy().append(" ").append(
                    Component.translatable("tooltip.riftgun.unstable")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }
            tooltip.add(fluidName);
            tooltip.add(infinite
                ? Component.translatable("screen.riftgun.zero_point_fuel_active")
                : Component.literal(amount + "/" + capacity + " mB"));
            if (overfilled) tooltip.add(Component.translatable("screen.riftgun.overfilled")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private void renderGroupDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<UUID> groups = orderedGroupIds(PortalClientState.data());
        Box box = dropdownBox(groups.size());
        graphics.nextStratum();
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.outline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
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
            graphics.text(font, trim(groupName(id), box.width() - 12), box.x() + 6, rowY + 5,
                id.equals(formGroup) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
    }

    private void renderVisualDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<PortalVisualType> types = PortalVisualRegistry.values();
        Box box = visualDropdownBox(types.size());
        Identifier selected = PortalVisualPreferences.selectedId();
        graphics.nextStratum();
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.outline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        for (int index = 0; index < types.size(); index++) {
            PortalVisualType type = types.get(index);
            int rowY = box.y() + 2 + index * ROW_HEIGHT;
            boolean hover = mouseX >= box.x() + 2 && mouseX < box.x() + box.width() - 2
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hover || index == visualDropdownIndex) {
                graphics.fill(box.x() + 2, rowY, box.x() + box.width() - 2, rowY + ROW_HEIGHT,
                    type.id().equals(selected) ? 0x773F7180 : 0x5530333A);
            }
            graphics.text(font, visualName(type), box.x() + 6, rowY + 5,
                type.id().equals(selected) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
    }

    private void renderSoundDropdown(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PortalSoundChannel channel = soundDropdownChannel;
        ThemedButton selector = channel == null ? null : soundSelectors.get(channel);
        if (channel == null || selector == null) return;
        List<PortalSoundChoice> choices = PortalSoundRegistry.values(channel);
        Box box = selectorDropdownBox(selector, choices.size());
        Identifier selected = PortalClientState.data().settings().portalSounds().selected(channel);
        graphics.nextStratum();
        graphics.fill(box.x() + 3, box.y() + 3, box.x() + box.width() + 3,
            box.y() + box.height() + 3, 0xCC000000);
        graphics.fill(box.x(), box.y(), box.x() + box.width(), box.y() + box.height(), PortalTheme.FIELD);
        graphics.outline(box.x(), box.y(), box.width(), box.height(), PortalTheme.BORDER_FOCUS);
        for (int index = 0; index < choices.size(); index++) {
            PortalSoundChoice choice = choices.get(index);
            int rowY = box.y() + 2 + index * ROW_HEIGHT;
            boolean hover = mouseX >= box.x() + 2 && mouseX < box.x() + box.width() - 2
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            if (hover || index == soundDropdownIndex) {
                graphics.fill(box.x() + 2, rowY, box.x() + box.width() - 2, rowY + ROW_HEIGHT,
                    choice.id().equals(selected) ? 0x773F7180 : 0x5530333A);
            }
            graphics.text(font, Component.translatable(choice.nameKey()), box.x() + 6, rowY + 5,
                choice.id().equals(selected) ? PortalTheme.ICE : PortalTheme.TEXT, false);
        }
    }

    private List<Row> buildRows() {
        PortalPlayerData data = PortalClientState.data();
        List<ExternalSection> externalSections = new ArrayList<>();
        for (ExternalDestinationSource source : ExternalDestinationSource.values()) {
            externalSections.add(new ExternalSection(source,
                ClientMapWaypointIntegration.installed(source)
                    && ClientMapWaypointIntegration.enabled(source),
                ClientMapWaypointIntegration.catalog().isGroupVisible(source),
                expandedExternalGroups.contains(source),
                ClientMapWaypointIntegration.catalog().destinations(source)));
        }
        List<PlayerEntry> players = PlayerListState.players().stream()
            .map(entry -> new PlayerEntry(entry.id(), entry.name(), entry.pinned(), entry.serverOrder()))
            .toList();
        PortalConfigRows.Result result = PortalConfigRows.build(data, query, this::groupName,
            this::distanceSquared, externalSections,
            new PlayerSection(playerTargets.visible(), playerTargets.expanded(), players));
        externalRows.clear();
        externalRows.putAll(result.externalRows());
        return result.rows();
    }

    private void drawGunSettingDescription(GuiGraphicsExtractor graphics, Box box) {
        Component description = gunSettingDescription();
        if (description == null) return;
        graphics.textWithWordWrap(font, description, box.x() + 18, box.y() + 30,
            box.width() - 36, PortalTheme.TEXT_MUTED);
    }

    private Component gunSettingDescription() {
        String key = gunSettingDescriptionKey(session.page());
        return key == null ? null : Component.translatable(key);
    }

    private int gunSettingDescriptionExtraHeight(int boxWidth) {
        Component description = gunSettingDescription();
        if (description == null) return 0;
        int lineCount = font.split(description, Math.max(1, boxWidth - 36)).size();
        return Math.max(0, lineCount - 1) * font.lineHeight;
    }

    private int gunSettingControlTop(Box box) {
        return box.y() + 45 + gunSettingDescriptionExtraHeight(box.width());
    }

    private double distanceSquared(Destination destination) {
        if (minecraft == null || minecraft.player == null
            || !minecraft.player.level().dimension().equals(destination.dimension())) return Double.POSITIVE_INFINITY;
        return minecraft.player.position().distanceToSqr(destination.position());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (soundDropdownChannel != null) {
            if (event.button() == 0) clickSoundDropdown(event.x(), event.y());
            soundDropdownChannel = null;
            return true;
        }
        if (visualDropdownOpen) {
            if (event.button() == 0) clickVisualDropdown(event.x(), event.y());
            visualDropdownOpen = false;
            return true;
        }
        if (groupDropdownOpen) {
            if (event.button() == 0) clickGroupDropdown(event.x(), event.y());
            groupDropdownOpen = false;
            return true;
        }
        if (session.page() != PortalConfigPage.NONE && session.page().isDestinationForm() && groupSelector != null
            && (event.button() == 0 || event.button() == 1) && event.x() >= groupSelector.getX()
            && event.x() < groupSelector.getX() + groupSelector.getWidth()
            && event.y() >= groupSelector.getY() && event.y() < groupSelector.getY() + groupSelector.getHeight()) {
            UUID before = formGroup;
            shiftFormGroup(event.button() == 0 ? 1 : -1);
            if (!before.equals(formGroup) && minecraft != null) {
                groupSelector.playDownSound(minecraft.getSoundManager());
            }
            setFocused(groupSelector);
            return true;
        }
        if (session.page() == PortalConfigPage.VISUAL_SETTINGS && visualSelector != null
            && (event.button() == 0 || event.button() == 1) && event.x() >= visualSelector.getX()
            && event.x() < visualSelector.getX() + visualSelector.getWidth()
            && event.y() >= visualSelector.getY() && event.y() < visualSelector.getY() + visualSelector.getHeight()) {
            Identifier before = PortalVisualPreferences.selectedId();
            shiftVisual(event.button() == 0 ? 1 : -1);
            if (!before.equals(PortalVisualPreferences.selectedId()) && minecraft != null) {
                visualSelector.playDownSound(minecraft.getSoundManager());
            }
            setFocused(visualSelector);
            return true;
        }
        if (session.page() == PortalConfigPage.SOUND_SETTINGS && (event.button() == 0 || event.button() == 1)) {
            for (var entry : soundSelectors.entrySet()) {
                ThemedButton selector = entry.getValue();
                if (event.x() < selector.getX() || event.x() >= selector.getX() + selector.getWidth()
                    || event.y() < selector.getY() || event.y() >= selector.getY() + selector.getHeight()) continue;
                PortalSoundChannel channel = entry.getKey();
                Identifier before = selectedSound(channel);
                shiftSound(channel, event.button() == 0 ? 1 : -1);
                if (!before.equals(selectedSound(channel)) && minecraft != null) {
                    selector.playDownSound(minecraft.getSoundManager());
                }
                setFocused(soundSelectors.get(channel));
                return true;
            }
        }
        if (session.page() != PortalConfigPage.NONE) return super.mouseClicked(event, doubleClick);
        if (event.button() == 0 && event.x() >= panelX + listWidth && event.x() < panelX + panelWidth
            && event.y() >= listTop && event.y() < listBottom) {
            if (clickDetail(event.x(), event.y())) return true;
        }
        if (event.button() == 0 && event.x() >= panelX && event.x() < panelX + listWidth
            && event.y() >= listTop && event.y() < listBottom) {
            for (Row row : hitRows) {
                if (event.y() < row.y() || event.y() >= row.y() + ROW_HEIGHT) continue;
                focusRow(row);
                int right = panelX + listWidth - 6;
                if (row.kind() == RowKind.PLAYER_SECTION) {
                    if (event.x() >= right - 27 && event.x() < right - 11) {
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
                        if (event.x() >= starLeft && event.x() < starLeft + ROW_ACTION_SIZE) {
                            togglePlayerPin(entry.id(), entry.pinned());
                        } else {
                            selectPlayer(entry.id());
                        }
                    }
                    return true;
                }
                if (row.kind() == RowKind.EXTERNAL_GROUP) {
                    ExternalDestinationSource source = externalSource(row.id());
                    if (source != null) toggleExternalGroup(source);
                    return true;
                }
                if (row.kind() == RowKind.EXTERNAL_DESTINATION) {
                    selectExternalDestination(row.id());
                    return true;
                }
                if (row.kind() == RowKind.DESTINATION) {
                    if (event.x() >= panelX + 7 && event.x() < panelX + 20) {
                        draggingDestination = row.id();
                        destinationDragActive = false;
                        dragStartX = event.x();
                        dragStartY = event.y();
                        return true;
                    }
                    int deleteLeft = right - ROW_ACTION_SIZE;
                    int starLeft = deleteLeft - ROW_ACTION_SIZE - 2;
                    if (event.x() >= starLeft && event.x() < starLeft + ROW_ACTION_SIZE) {
                        togglePin(row.id());
                    } else if (event.x() >= deleteLeft) {
                        requestDeleteDestination(row.id());
                    } else {
                        selectDestination(row.id());
                    }
                    return true;
                }
                boolean custom = !row.id().equals(PortalPlayerData.DEFAULT_GROUP_ID)
                    && !row.id().equals(PortalPlayerData.SHARED_SECTION_ID);
                if (custom && event.x() >= right - 30 && event.x() < right - 16) {
                    openForm(PortalConfigPage.RENAME_GROUP, row.id());
                } else if (custom && event.x() >= right - 14) {
                    requestDeleteGroup(row.id());
                } else if (custom && event.x() < panelX + 16) {
                    draggingGroup = row.id();
                    dragStartY = event.y();
                } else {
                    toggleGroup(row.id());
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
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
            openForm(PortalConfigPage.EDIT_DESTINATION, viewedDestination);
            return true;
        }
        int visibleShareY = detailShareY;
        if (viewed() != null && mouseY >= visibleShareY && mouseY < visibleShareY + 18) {
            openForm(PortalConfigPage.SHARE_DESTINATION, viewedDestination);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == 0 && draggingDestination != null) {
            if (Math.hypot(event.x() - dragStartX, event.y() - dragStartY) >= 5.0) {
                destinationDragActive = true;
            }
            return true;
        }
        if (draggingDetailScrollbar && event.button() == 0) {
            updateDetailScrollbar(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingDetailScrollbar = false;
        if (event.button() == 0 && draggingDestination != null) {
            UUID moving = draggingDestination;
            boolean active = destinationDragActive;
            draggingDestination = null;
            destinationDragActive = false;
            UUID targetGroup = active ? destinationDropGroupAt(event.x(), event.y()) : null;
            if (targetGroup != null && !targetGroup.equals(destinationGroup(moving))) {
                moveDestinationToGroup(moving, targetGroup);
            } else if (!active) {
                selectDestination(moving);
            }
            return true;
        }
        if (event.button() == 0 && draggingGroup != null) {
            UUID moving = draggingGroup;
            draggingGroup = null;
            if (Math.abs(event.y() - dragStartY) >= 5.0) {
                Row target = hitRows.stream().filter(row -> row.kind() == RowKind.GROUP)
                    .min(Comparator.comparingDouble(row -> Math.abs(event.y() - (row.y() + ROW_HEIGHT / 2.0))))
                    .orElse(null);
                if (target != null) moveGroupTo(moving, groupOrderIndex(target.id()));
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (visualDropdownOpen) return true;
        if (groupDropdownOpen) {
            int visible = Math.min(7, orderedGroupIds(PortalClientState.data()).size());
            groupDropdownScroll = Mth.clamp(groupDropdownScroll - (int) Math.signum(vertical), 0,
                Math.max(0, orderedGroupIds(PortalClientState.data()).size() - visible));
            return true;
        }
        if (session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS && !visualOptionWidgets.isEmpty()) {
            Box box = modalBox();
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
        if (session.page() == PortalConfigPage.NONE && mouseY >= listTop && mouseY < listBottom) {
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
    public boolean keyPressed(KeyEvent event) {
        if (soundDropdownChannel != null) return soundDropdownKeyPressed(event.key());
        if (visualDropdownOpen) return visualDropdownKeyPressed(event.key());
        if (groupDropdownOpen) return dropdownKeyPressed(event.key());
        if (session.page() != PortalConfigPage.NONE && session.page().isDestinationForm() && groupSelector != null
            && groupSelector.isFocused() && (event.key() == 263 || event.key() == 262)) {
            shiftFormGroup(event.key() == 263 ? -1 : 1);
            return true;
        }
        if (session.page() == PortalConfigPage.VISUAL_SETTINGS && visualSelector != null && visualSelector.isFocused()
            && (event.key() == 263 || event.key() == 262)) {
            shiftVisual(event.key() == 263 ? -1 : 1);
            return true;
        }
        if (session.page() == PortalConfigPage.SOUND_SETTINGS && (event.key() == 263 || event.key() == 262)) {
            for (var entry : soundSelectors.entrySet()) {
                if (!entry.getValue().isFocused()) continue;
                shiftSound(entry.getKey(), event.key() == 263 ? -1 : 1);
                return true;
            }
        }
        if (event.key() == 256 && session.page() == PortalConfigPage.SWIRL_ANIMATION_SETTINGS) {
            backToVisualSettings();
            return true;
        }
        if (event.key() == 256 && session.page().isGunSettingPage()) {
            backToGunSettings();
            return true;
        }
        if (event.key() == 256 && session.page() == PortalConfigPage.VISUAL_SETTINGS) {
            backToSettings();
            return true;
        }
        if (event.key() == 256 && session.page() == PortalConfigPage.SOUND_SETTINGS) {
            backToSettings();
            return true;
        }
        if (event.key() == 256 && (session.page() == PortalConfigPage.CONFIRM_SETTINGS
            || session.page() == PortalConfigPage.MAP_INTEGRATION_SETTINGS)) {
            backToSettings();
            return true;
        }
        if (event.key() == 256 && session.page() != PortalConfigPage.NONE) {
            requestCloseModal();
            return true;
        }
        if ((event.key() == 257 || event.key() == 335) && session.page() != PortalConfigPage.NONE) {
            if (session.page().isConfirmation()) acceptConfirmation();
            else submitModal();
            return true;
        }
        if (session.page() == PortalConfigPage.NONE && event.key() == 258) {
            if (searchBox != null && searchBox.isFocused() && !Minecraft.getInstance().hasShiftDown()) {
                focusFirstRow();
                return true;
            }
            if (listFocused) {
                listFocused = false;
                setFocused(Minecraft.getInstance().hasShiftDown() ? searchBox : firstCreateButton);
                return true;
            }
        }
        if (session.page() == PortalConfigPage.NONE && listFocused && listKeyPressed(event.key())) return true;
        return super.keyPressed(event);
    }

    private boolean listKeyPressed(int keyCode) {
        if ((keyCode == 265 || keyCode == 264) && Minecraft.getInstance().hasAltDown() && focusedRowKind == RowKind.GROUP
            && focusedRowId != null && !focusedRowId.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && !focusedRowId.equals(PortalPlayerData.SHARED_SECTION_ID)) {
            moveGroup(focusedRowId, keyCode == 265 ? -1 : 1);
            return true;
        }
        if (keyCode == 265 || keyCode == 264) {
            moveRowFocus(keyCode == 265 ? -1 : 1);
            return true;
        }
        if ((keyCode == 257 || keyCode == 335) && focusedRowId != null) {
            if (focusedRowKind == RowKind.DESTINATION) selectDestination(focusedRowId);
            else if (focusedRowKind == RowKind.EXTERNAL_DESTINATION) selectExternalDestination(focusedRowId);
            else if (focusedRowKind == RowKind.EXTERNAL_GROUP) {
                ExternalDestinationSource source = externalSource(focusedRowId);
                if (source != null) toggleExternalGroup(source);
            }
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
            openForm(PortalConfigPage.EDIT_DESTINATION, focusedRowId);
            return true;
        }
        if (keyCode == 82 && focusedRowKind == RowKind.GROUP && focusedRowId != null
            && !focusedRowId.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && !focusedRowId.equals(PortalPlayerData.SHARED_SECTION_ID)) {
            openForm(PortalConfigPage.RENAME_GROUP, focusedRowId);
            return true;
        }
        if (keyCode == 261 && focusedRowId != null) {
            if (focusedRowKind == RowKind.DESTINATION) requestDeleteDestination(focusedRowId);
            else if (focusedRowKind == RowKind.GROUP) requestDeleteGroup(focusedRowId);
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
        playerTargets.clearSelection();
        clearExternalSelection(false);
        if (!id.equals(previous)) detailScroll = 0;
        if (!id.equals(previous)) {
            pendingSelection = id;
            selectionDueTick = clientTicks + 6L;
        }
        updateOpenPortalButton();
    }

    private void selectExternalDestination(UUID rowId) {
        ExternalDestination destination = externalRows.get(rowId);
        if (destination == null) return;
        viewedExternalRow = rowId;
        viewedDestination = null;
        selectedGroup = null;
        detailScroll = 0;
        focusedRowId = rowId;
        focusedRowKind = RowKind.EXTERNAL_DESTINATION;
        ensureVisibleId = rowId;
        if (destination.selectable()) {
            flushSelection();
            playerTargets.clearSelection();
            PortalClientState.data().selectedDestinationId(null);
            selectedExternalRow = rowId;
            ClientMapWaypointIntegration.select(destination);
            ExternalDestinationSelection selection = ClientMapWaypointIntegration.selected();
            PortalNetworking.sendRequest(PortalAction.SELECT_EXTERNAL_DESTINATION,
                tag -> tag.merge(ExternalDestinationRequest.encode(selection)));
        }
        updateOpenPortalButton();
    }

    private void shareViewed(PortalAction action) {
        UUID id = session.target();
        if (id == null) return;
        PortalNetworking.sendRequest(action, tag -> Nbt.putUUID(tag, "Destination", id));
        closeModalNow();
    }

    private void selectPlayer(UUID id) {
        playerTargets.select(id);
        viewedDestination = null;
        selectedGroup = null;
        pendingSelection = null;
        selectionDueTick = -1L;
        clearExternalSelection(false);
        detailScroll = 0;
        focusedRowId = id;
        focusedRowKind = RowKind.PLAYER;
        ensureVisibleId = id;
        updateOpenPortalButton();
    }

    private void togglePlayerPin(UUID id, boolean pinned) {
        playerTargets.togglePin(id, pinned);
    }

    private void requestPlayerListRefresh() {
        playerTargets.requestList();
    }

    private void flushSelection() {
        UUID id = pendingSelection;
        pendingSelection = null;
        selectionDueTick = -1L;
        if (id == null || minecraft == null || minecraft.getConnection() == null) return;
        PortalNetworking.sendRequest(PortalAction.SELECT_DESTINATION, tag -> Nbt.putUUID(tag, "Destination", id));
    }

    private void generatePortal() {
        if (playerTargets.selectedId() != null) {
            playerTargets.openSelected();
            return;
        }
        if (selectedExternalRow != null && selectedExternalRow.equals(viewedExternalRow)) {
            PortalNetworking.sendRequest(PortalAction.OPEN_EXTERNAL_DESTINATION,
                tag -> tag.putString("PlacementMode",
                    PortalClientState.data().settings().placementMode().name()));
            return;
        }
        if (viewedDestination == null) return;
        flushSelection();
        PortalNetworking.sendRequest(PortalAction.OPEN_PORTAL,
            tag -> Nbt.putUUID(tag, "Destination", viewedDestination));
    }

    private void requestRandomRift() {
        PortalNetworking.sendRequest(PortalAction.OPEN_RANDOM_RIFT);
        if (minecraft != null) minecraft.setScreen(null);
    }

    private List<Component> randomRiftTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.riftgun.random_rift"));
        if (Nbt.getBoolean(PortalClientState.randomRift(), "Searching")) {
            lines.add(Component.translatable("screen.riftgun.random_rift.searching"));
            return lines;
        }
        int cooldownTicks = PortalClientState.randomRiftCooldownTicks();
        if (cooldownTicks > 0) {
            lines.add(Component.translatable("screen.riftgun.random_rift.cooldown",
                (cooldownTicks + 19) / 20));
        } else {
            lines.add(Component.translatable("screen.riftgun.random_rift.description"));
        }
        return lines;
    }

    private void updateRandomRiftButton() {
        if (randomRiftButton == null) return;
        randomRiftButton.active = !Nbt.getBoolean(PortalClientState.randomRift(), "Searching")
            && PortalClientState.randomRiftCooldownTicks() <= 0;
    }

    private void togglePin(UUID id) {
        focusedRowId = id;
        focusedRowKind = RowKind.DESTINATION;
        ensureVisibleId = id;
        PortalNetworking.sendRequest(PortalAction.TOGGLE_PIN, tag -> Nbt.putUUID(tag, "Destination", id));
    }

    private void requestDeleteDestination(UUID id) {
        if (PortalClientState.data().settings().confirmDeletion()) {
            openForm(PortalConfigPage.CONFIRM_DELETE_DESTINATION, id);
        } else {
            PortalNetworking.sendRequest(PortalAction.DELETE_DESTINATION, tag -> Nbt.putUUID(tag, "Destination", id));
        }
    }

    private void requestDeleteGroup(UUID id) {
        if (id.equals(PortalPlayerData.DEFAULT_GROUP_ID)) return;
        if (PortalClientState.data().settings().confirmDeletion()) {
            openForm(PortalConfigPage.CONFIRM_DELETE_GROUP, id);
        } else {
            PortalNetworking.sendRequest(PortalAction.DELETE_GROUP, tag -> Nbt.putUUID(tag, "Group", id));
        }
    }

    private void requestClearFluid() {
        if (PortalClientState.gun().amount() <= 0) return;
        if (PortalClientState.data().settings().confirmClearFluid()) {
            openForm(PortalConfigPage.CONFIRM_CLEAR_FLUID, null);
        } else {
            PortalNetworking.sendRequest(PortalAction.CLEAR_GUN_FLUID);
        }
    }

    private void toggleGroup(UUID id) {
        selectedGroup = id;
        boolean expanded = !PortalClientState.data().expandedGroups().contains(id);
        PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
            Nbt.putUUID(tag, "Group", id);
            tag.putBoolean("Expanded", expanded);
        });
    }

    private void togglePlayerSection() {
        playerTargets.toggleExpanded();
    }

    private void cycleSort() {
        sendSettings(PortalConfigSettings.cycleSort(PortalClientState.data().settings()));
    }

    private void cyclePlacementMode() {
        PortalPlayerSettings old = PortalClientState.data().settings();
        PortalPlayerSettings next = PortalConfigSettings.cyclePlacementMode(old,
            PortalClientState.gun().entityRelocationEnabled(), remoteInstalled());
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void toggleRememberScrollPosition() {
        boolean enabled = !rememberScrollPosition();
        ClientConfig.VALUES.rememberGuiScrollPosition.set(enabled);
        ClientConfig.SPEC.save();
        if (!enabled) PortalGuiScrollMemory.clear();
        rebuildWidgets();
    }

    private void saveScrollPosition() {
        PortalGuiScrollMemory.remember(rememberScrollPosition(), listScroll, detailScroll);
    }

    private static boolean rememberScrollPosition() {
        return ClientConfig.VALUES.rememberGuiScrollPosition.get();
    }

    private void cycleMotionPrediction() {
        PortalPlayerSettings old = PortalClientState.data().settings();
        PortalPlayerSettings next = PortalConfigSettings.cyclePredictionMode(old);
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void openGunSettings() {
        session.navigate(PortalConfigPage.GUN_SETTINGS);
        rebuildWidgets();
    }

    private void openGunSetting(PortalConfigPage page) {
        if (!page.isGunSettingPage()) return;
        session.navigate(page);
        rebuildWidgets();
    }

    private void backToGunSettings() {
        session.navigate(PortalConfigPage.GUN_SETTINGS);
        rebuildWidgets();
    }

    private void openVisualSettings() {
        session.navigate(PortalConfigPage.VISUAL_SETTINGS);
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void openSoundSettings() {
        session.navigate(PortalConfigPage.SOUND_SETTINGS);
        soundDropdownChannel = null;
        rebuildWidgets();
    }

    private void openSwirlAnimationSettings() {
        if (PortalVisualPreferences.selected().options().isEmpty()) return;
        session.navigate(PortalConfigPage.SWIRL_ANIMATION_SETTINGS);
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void backToVisualSettings() {
        flushVisualSettings();
        session.navigate(PortalConfigPage.VISUAL_SETTINGS);
        visualOptionsScroll = 0;
        rebuildWidgets();
    }

    private void backToSettings() {
        flushVisualSettings();
        session.navigate(PortalConfigPage.SETTINGS);
        visualDropdownOpen = false;
        soundDropdownChannel = null;
        rebuildWidgets();
    }

    private boolean coordinateOverrideUnlocked() {
        return PortalClientState.gun().coordinateOverride();
    }

    private boolean coordinateEditingUnlocked() {
        return coordinateOverrideUnlocked()
            || PortalClientState.gun().dimensionalTraversalInstalled()
                && PortalClientState.gun().dimensionalTraversalEnabled();
    }

    private boolean pairingInstalled() {
        return PortalClientState.gun().pairingInstalled();
    }

    private boolean remoteInstalled() {
        return PortalClientState.gun().remoteInstalled();
    }

    private int footerHeight() {
        return FOOTER_HEIGHT;
    }

    private boolean hasCrossDimensionFuel() {
        return PortalClientState.gun().infiniteFuel()
            || PortalClientState.gun().crossDimensionFuel();
    }

    private int moduleCount(String kind) {
        return PortalClientState.gun().moduleCount(PortalModuleKind.valueOf(kind));
    }

    private boolean hasEntityTransitModule() {
        return moduleCount("PASSIVE_TRANSIT") > 0 || moduleCount("HOSTILE_TRANSIT") > 0
            || moduleCount("BOSS_TRANSIT") > 0;
    }

    private void toggleGunBoolean(BooleanSetting setting) {
        boolean enabled = applyGunBooleanToggle(setting);
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting.wireName());
            tag.putBoolean("Enabled", enabled);
        });
    }

    private void toggleExternalGroup(ExternalDestinationSource source) {
        boolean expanded = !expandedExternalGroups.contains(source);
        if (expanded) expandedExternalGroups.add(source);
        else expandedExternalGroups.remove(source);
        UUID sectionId = externalSectionId(source);
        PortalNetworking.sendRequest(PortalAction.SET_GROUP_EXPANDED, tag -> {
            Nbt.putUUID(tag, "Group", sectionId);
            tag.putBoolean("Expanded", expanded);
        });
    }

    private void syncExternalGroupExpansion(PortalPlayerData data) {
        expandedExternalGroups.clear();
        for (ExternalDestinationSource source : ExternalDestinationSource.values()) {
            if (data.expandedGroups().contains(externalSectionId(source))) {
                expandedExternalGroups.add(source);
            }
        }
    }

    private boolean applyGunBooleanToggle(BooleanSetting setting) {
        boolean enabled = !PortalGunViewStateReducer.booleanValue(
            PortalClientState.gun(), setting);
        PortalClientState.updateGun(state ->
            PortalGunViewStateReducer.withBoolean(state, setting, enabled));
        rebuildWidgets();
        return enabled;
    }

    private Component fallbackLabel(String labelKey, FallbackSetting setting) {
        PortalFloatingFallback value = PortalGunViewStateReducer.fallbackValue(
            PortalClientState.gun(), setting);
        String key = value == PortalFloatingFallback.REMOTE ? "screen.riftgun.placement_mode.remote"
            : "screen.riftgun.placement_mode.front";
        return Component.translatable(labelKey,
            Component.translatable(key));
    }

    private void cyclePairingFallback(FallbackSetting setting) {
        PortalFloatingFallback current = PortalGunViewStateReducer.fallbackValue(
            PortalClientState.gun(), setting);
        PortalFloatingFallback next = current == PortalFloatingFallback.REMOTE
            ? PortalFloatingFallback.FRONT : PortalFloatingFallback.REMOTE;
        PortalClientState.updateGun(state ->
            PortalGunViewStateReducer.withFallback(state, setting, next));
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting.wireName());
            tag.putString("Value", next.name());
        });
        rebuildWidgets();
    }

    private void cyclePlayerExclude() {
        int mode = Math.floorMod(PortalClientState.gun().playerExcludeMode().id() + 1, 3);
        PortalClientState.updateGun(state -> PortalGunViewStateReducer.stepPlayerExclude(state, 1));
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", "PlayerExclude");
            tag.putInt("Step", 1);
        });
    }

    private void updateGunDistance(DistanceSetting setting, int value) {
        PortalClientState.updateGun(state ->
            PortalGunViewStateReducer.withDistance(state, setting, value));
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", setting.wireName());
            tag.putInt("Value", value);
        });
    }

    private void moveGroup(UUID group, int delta) {
        PortalNetworking.sendRequest(PortalAction.MOVE_GROUP, tag -> {
            Nbt.putUUID(tag, "Group", group);
            tag.putInt("Delta", delta);
        });
    }

    private void moveGroupTo(UUID group, int index) {
        PortalNetworking.sendRequest(PortalAction.MOVE_GROUP, tag -> {
            Nbt.putUUID(tag, "Group", group);
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
            Nbt.putUUID(tag, "Destination", destination);
            Nbt.putUUID(tag, "Group", group);
        });
    }

    private @Nullable UUID destinationDropGroupAt(double mouseX, double mouseY) {
        if (mouseX < panelX + 4 || mouseX >= panelX + listWidth - 4
            || mouseY < listTop || mouseY >= listBottom) return null;
        for (Row row : hitRows) {
            if (mouseY < row.y() || mouseY >= row.y() + ROW_HEIGHT) continue;
            UUID group = row.kind() == RowKind.GROUP ? row.id() : destinationGroup(row.id());
            return PortalPlayerData.SHARED_SECTION_ID.equals(group) ? null : group;
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

    private void updateSetting(Toggle setting) {
        PortalPlayerSettings old = PortalClientState.data().settings();
        PortalPlayerSettings next = PortalConfigSettings.toggle(old, setting);
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private void sendSettings(PortalPlayerSettings settings) {
        PortalNetworking.sendRequest(PortalAction.SET_SETTINGS, tag -> tag.merge(settings.save()));
    }

    private void openForm(PortalConfigPage next, @Nullable UUID target) {
        session.open(next, target);
        groupDropdownOpen = false;
        visualDropdownOpen = false;
        formName = "";
        formX = formY = formZ = formYaw = "";
        formGroup = creationGroup();
        if (next == PortalConfigPage.CREATE_COORDINATE && minecraft != null && minecraft.player != null) {
            formX = Double.toString(minecraft.player.getX());
            formY = Double.toString(minecraft.player.getY());
            formZ = Double.toString(minecraft.player.getZ());
            formYaw = Float.toString(minecraft.player.getYRot());
        } else if (next == PortalConfigPage.EDIT_DESTINATION && target != null) {
            Destination destination = PortalClientState.data().destination(target).orElse(null);
            if (destination != null) {
                formName = destination.name();
                formX = Double.toString(destination.x());
                formY = Double.toString(destination.y());
                formZ = Double.toString(destination.z());
                formYaw = Float.toString(destination.yaw());
                formGroup = destination.groupId();
            }
        } else if (next == PortalConfigPage.RENAME_GROUP && target != null) {
            formName = PortalClientState.data().group(target).map(DestinationGroup::name).orElse("");
        }
        rebuildWidgets();
    }

    private void submitModal() {
        switch (session.page()) {
            case CREATE_CURRENT -> sendDestinationForm(PortalAction.CREATE_CURRENT, false);
            case CREATE_COORDINATE -> sendDestinationForm(PortalAction.CREATE_COORDINATE, true);
            case EDIT_DESTINATION -> sendDestinationForm(PortalAction.EDIT_DESTINATION, true);
            case CREATE_GROUP -> PortalNetworking.sendRequest(PortalAction.CREATE_GROUP,
                tag -> tag.putString("Name", formName));
            case RENAME_GROUP -> PortalNetworking.sendRequest(PortalAction.RENAME_GROUP, tag -> {
                if (session.target() != null) Nbt.putUUID(tag, "Group", session.target());
                tag.putString("Name", formName);
            });
            default -> { return; }
        }
        closeModalNow();
    }

    private void sendDestinationForm(PortalAction action, boolean coordinates) {
        PortalNetworking.sendRequest(action, tag -> {
            tag.putString("Name", formName);
            Nbt.putUUID(tag, "Group", formGroup);
            if (action == PortalAction.EDIT_DESTINATION && session.target() != null) {
                Nbt.putUUID(tag, "Destination", session.target());
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
        if (selectedGroup != null && !selectedGroup.equals(PortalPlayerData.SHARED_SECTION_ID)) return selectedGroup;
        Destination current = viewed();
        return current == null || current.groupId().equals(PortalPlayerData.SHARED_SECTION_ID)
            ? PortalPlayerData.DEFAULT_GROUP_ID : current.groupId();
    }

    private void requestCloseModal() {
        if (session.page() == PortalConfigPage.CONFIRM_DIRTY) {
            cancelConfirmation();
        } else if (session.page().hasInputs() && session.dirty()
            && PortalClientState.data().settings().confirmDiscardedChanges()) {
            session.requestClose();
            groupDropdownOpen = false;
            visualDropdownOpen = false;
            rebuildWidgets();
        } else {
            closeModalNow();
        }
    }

    private void acceptConfirmation() {
        if (session.page() == PortalConfigPage.CONFIRM_DIRTY) {
            closeModalNow();
        } else if (session.page() == PortalConfigPage.CONFIRM_DELETE_DESTINATION && session.target() != null) {
            UUID id = session.target();
            PortalNetworking.sendRequest(PortalAction.DELETE_DESTINATION, tag -> Nbt.putUUID(tag, "Destination", id));
            closeModalNow();
        } else if (session.page() == PortalConfigPage.CONFIRM_DELETE_GROUP && session.target() != null) {
            UUID id = session.target();
            PortalNetworking.sendRequest(PortalAction.DELETE_GROUP, tag -> Nbt.putUUID(tag, "Group", id));
            closeModalNow();
        } else if (session.page() == PortalConfigPage.CONFIRM_CLEAR_FLUID) {
            PortalNetworking.sendRequest(PortalAction.CLEAR_GUN_FLUID);
            closeModalNow();
        }
    }

    private void cancelConfirmation() {
        session.cancelConfirmation();
        groupDropdownOpen = false;
        visualDropdownOpen = false;
        rebuildWidgets();
    }

    private void closeModalNow() {
        session.close();
        groupDropdownOpen = false;
        visualDropdownOpen = false;
        rebuildWidgets();
    }

    private void openGroupDropdown() {
        groupDropdownOpen = true;
        List<UUID> groups = orderedGroupIds(PortalClientState.data());
        groupDropdownIndex = Math.max(0, groups.indexOf(formGroup));
        groupDropdownScroll = Mth.clamp(groupDropdownIndex - 3, 0, Math.max(0, groups.size() - 7));
        setFocused(groupSelector);
    }

    private void shiftFormGroup(int delta) {
        List<UUID> groups = orderedGroupIds(PortalClientState.data());
        UUID next = GroupSelection.cycle(groups, formGroup, delta);
        if (!next.equals(formGroup)) selectFormGroup(next);
    }

    private void selectFormGroup(UUID id) {
        formGroup = id;
        session.markDirty();
        if (groupSelector != null) groupSelector.setMessage(
            Component.translatable("screen.riftgun.group_value", groupName(formGroup)));
    }

    private boolean clickGroupDropdown(double mouseX, double mouseY) {
        List<UUID> groups = orderedGroupIds(PortalClientState.data());
        Box box = dropdownBox(groups.size());
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
        List<UUID> groups = orderedGroupIds(PortalClientState.data());
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
        Identifier selected = PortalVisualPreferences.selectedId();
        visualDropdownIndex = 0;
        for (int index = 0; index < types.size(); index++) {
            if (types.get(index).id().equals(selected)) {
                visualDropdownIndex = index;
                break;
            }
        }
        setFocused(visualSelector);
    }

    private void openSoundDropdown(PortalSoundChannel channel) {
        soundDropdownChannel = channel;
        List<PortalSoundChoice> choices = PortalSoundRegistry.values(channel);
        Identifier selected = selectedSound(channel);
        soundDropdownIndex = 0;
        for (int index = 0; index < choices.size(); index++) {
            if (choices.get(index).id().equals(selected)) {
                soundDropdownIndex = index;
                break;
            }
        }
        setFocused(soundSelectors.get(channel));
    }

    private void shiftSound(PortalSoundChannel channel, int direction) {
        Identifier next = PortalSoundRegistry.cycle(channel, selectedSound(channel), direction);
        selectSound(channel, next);
    }

    private void selectSound(PortalSoundChannel channel, Identifier id) {
        PortalSoundSettings current = PortalClientState.data().settings().portalSounds();
        updatePortalSounds(current.withSelection(channel, id));
    }

    private void toggleSplashSound() {
        PortalSoundSettings current = PortalClientState.data().settings().portalSounds();
        updatePortalSounds(current.withSplashEnabled(!current.splashEnabled()));
    }

    private void updatePortalSounds(PortalSoundSettings sounds) {
        PortalPlayerSettings next = PortalClientState.data().settings().withPortalSounds(sounds);
        PortalClientState.data().settings(next);
        sendSettings(next);
        rebuildWidgets();
    }

    private Identifier selectedSound(PortalSoundChannel channel) {
        return PortalClientState.data().settings().portalSounds().selected(channel);
    }

    private boolean clickSoundDropdown(double mouseX, double mouseY) {
        PortalSoundChannel channel = soundDropdownChannel;
        ThemedButton selector = channel == null ? null : soundSelectors.get(channel);
        if (channel == null || selector == null) return false;
        List<PortalSoundChoice> choices = PortalSoundRegistry.values(channel);
        Box box = selectorDropdownBox(selector, choices.size());
        if (mouseX < box.x() || mouseX >= box.x() + box.width()
            || mouseY < box.y() || mouseY >= box.y() + box.height()) return false;
        if (mouseY < box.y() + 2 || mouseY >= box.y() + 2 + choices.size() * ROW_HEIGHT) return true;
        int index = (int) ((mouseY - box.y() - 2) / ROW_HEIGHT);
        if (index >= 0 && index < choices.size()) selectSound(channel, choices.get(index).id());
        soundDropdownChannel = null;
        return true;
    }

    private boolean soundDropdownKeyPressed(int keyCode) {
        PortalSoundChannel channel = soundDropdownChannel;
        if (channel == null) return false;
        List<PortalSoundChoice> choices = PortalSoundRegistry.values(channel);
        if (keyCode == 256) {
            soundDropdownChannel = null;
            return true;
        }
        if (keyCode == 265 || keyCode == 264) {
            soundDropdownIndex = Mth.clamp(
                soundDropdownIndex + (keyCode == 265 ? -1 : 1), 0, choices.size() - 1);
            return true;
        }
        if (keyCode == 257 || keyCode == 335) {
            selectSound(channel, choices.get(soundDropdownIndex).id());
            soundDropdownChannel = null;
            return true;
        }
        return true;
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
        Box box = visualDropdownBox(types.size());
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
        return maximumScroll(listContentHeight, listViewportHeight());
    }

    private int detailMaxScroll() {
        return maximumScroll(detailContentHeight, listViewportHeight());
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int x, int top, int bottom, int scroll,
                                 int contentHeight, int viewportHeight) {
        int max = Math.max(0, contentHeight - viewportHeight);
        if (max <= 0) return;
        int thumb = scrollbarThumbHeight(top, bottom, contentHeight, viewportHeight);
        int thumbY = scrollbarThumbY(top, bottom, scroll, contentHeight, viewportHeight);
        graphics.fill(x, thumbY, x + 2, thumbY + thumb, PortalTheme.ICE);
    }

    public void refreshFromServer(Set<UUID> ignoredInvalidatedSafety) {
        UUID previousSelectedPlayer = playerTargets.selectedId();
        playerTargets.sync(PortalClientState.data());
        syncExternalGroupExpansion(PortalClientState.data());
        UUID serverSelectedPlayer = PortalClientState.data().selectedPlayerId();
        if (serverSelectedPlayer != null) {
            if (!serverSelectedPlayer.equals(previousSelectedPlayer)) {
                viewedDestination = null;
                detailScroll = 0;
                ensureVisibleId = serverSelectedPlayer;
                updateOpenPortalButton();
            }
        } else if (previousSelectedPlayer != null) {
            updateOpenPortalButton();
        }
        if (pendingSelection != null) {
            PortalClientState.data().selectedDestinationId(pendingSelection);
            PortalClientState.data().lastViewedDestinationId(pendingSelection);
        }
        UUID selected = PortalClientState.data().selectedDestinationId();
        if (playerTargets.selectedId() == null && selected != null && !selected.equals(viewedDestination)) {
            viewedDestination = selected;
            focusedRowId = selected;
            focusedRowKind = RowKind.DESTINATION;
            detailScroll = 0;
            ensureVisibleId = selected;
        } else if (playerTargets.selectedId() == null && viewedDestination != null
            && PortalClientState.data().destination(viewedDestination).isEmpty()) {
            viewedDestination = selected;
            detailScroll = 0;
        }
        if (selectedGroup != null && !selectedGroup.equals(PortalPlayerData.DEFAULT_GROUP_ID)
            && PortalClientState.data().group(selectedGroup).isEmpty()) selectedGroup = null;
        if (session.page() == PortalConfigPage.NONE) rebuildWidgets();
    }

    public void onPortalOpened() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    private void updateOpenPortalButton() {
        if (openPortalButton == null) return;
        ExternalDestination external = viewedExternal();
        openPortalButton.active = viewed() != null || playerTargets.selectedId() != null
            || external != null && external.selectable() && viewedExternalRow.equals(selectedExternalRow);
        openPortalButton.setMessage(Component.translatable("screen.riftgun.generate"));
    }

    /** Called when the server refreshes the online player roster. */
    public void onPlayerListRefresh() {
        if (playerTargets.clearUnavailableSelection()) {
            updateOpenPortalButton();
        }
        if (session.page() == PortalConfigPage.NONE) rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openCoordinateEditorForQa() {
        openForm(PortalConfigPage.CREATE_COORDINATE, null);
    }

    /** Used only by the opt-in visual QA harness. */
    public void openGroupDropdownForQa() {
        if (session.page().isDestinationForm()) openGroupDropdown();
    }

    /** Used only by the opt-in visual QA harness. */
    public void closeGroupDropdownForQa() {
        groupDropdownOpen = false;
    }

    /** Used only by the opt-in visual QA harness. */
    public void openPlacementSettingsForQa() {
        session.navigate(PortalConfigPage.SMART_DISTANCE_SETTINGS);
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openVisualSettingsForQa() {
        session.navigate(PortalConfigPage.VISUAL_SETTINGS);
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
        if (session.page() == PortalConfigPage.VISUAL_SETTINGS) openSwirlAnimationSettings();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openVisualDropdownForQa() {
        if (session.page() == PortalConfigPage.VISUAL_SETTINGS) openVisualDropdown();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openGunSettingsForQa() {
        visualDropdownOpen = false;
        openGunSettings();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openSmartRangeSettingsForQa() {
        session.navigate(PortalConfigPage.SMART_DISTANCE_SETTINGS);
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openRemoteSettingsForQa() {
        session.navigate(PortalConfigPage.REMOTE_SETTINGS);
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openPairingSettingsForQa() {
        session.navigate(PortalConfigPage.PORTAL_PAIRING_SETTINGS);
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void toggleRemoteRadialSliderForQa() {
        applyGunBooleanToggle(BooleanSetting.REMOTE_RADIAL_SLIDER);
    }

    /** Used only by the opt-in visual QA harness. */
    public void openPortalDurationSettingsForQa() {
        session.navigate(PortalConfigPage.PORTAL_DURATION_SETTINGS);
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openEntityTransitSettingsForQa() {
        session.navigate(PortalConfigPage.ENTITY_TRANSIT_SETTINGS);
        rebuildWidgets();
    }

    /** Used only by the opt-in visual QA harness. */
    public void openApertureSettingsForQa() {
        session.navigate(PortalConfigPage.APERTURE_SETTINGS);
        rebuildWidgets();
    }

    private @Nullable Destination viewed() {
        return viewedDestination == null ? null : PortalClientState.data().destination(viewedDestination).orElse(null);
    }

    private void openSettingsPage(PortalConfigPage page) {
        session.navigate(page);
        rebuildWidgets();
    }

    private void toggleMapSource(ExternalDestinationSource source) {
        boolean enabled = !ClientMapWaypointIntegration.enabled(source);
        switch (source) {
            case JOURNEYMAP -> ClientConfig.VALUES.journeyMapWaypointsEnabled.set(enabled);
            case XAERO_MINIMAP -> ClientConfig.VALUES.xaeroWaypointsEnabled.set(enabled);
        }
        ClientConfig.SPEC.save();
        if (!enabled && ClientMapWaypointIntegration.selected() != null
            && ClientMapWaypointIntegration.selected().source() == source) {
            clearExternalSelection(true);
        }
        refreshExternalDestinations(true);
        rebuildWidgets();
    }

    private @Nullable ExternalDestination viewedExternal() {
        return viewedExternalRow == null ? null : findExternal(viewedExternalRow);
    }

    private @Nullable ExternalDestination findExternal(UUID rowId) {
        ExternalDestination cached = externalRows.get(rowId);
        if (cached != null) return cached;
        for (ExternalDestinationSource source : ExternalDestinationSource.values()) {
            for (ExternalDestination destination : ClientMapWaypointIntegration.catalog().destinations(source)) {
                if (externalRowId(source, destination.stableId()).equals(rowId)) return destination;
            }
        }
        return null;
    }

    private void refreshExternalDestinations(boolean manual) {
        if (!manual && externalDestinationsInitialized) return;
        if (minecraft == null || minecraft.getConnection() == null) return;
        externalDestinationsInitialized = true;
        Set<String> dimensions = minecraft.getConnection().levels().stream()
            .map(key -> key.identifier().toString()).collect(java.util.stream.Collectors.toSet());
        ClientMapWaypointIntegration.refresh(dimensions, ClientConfig.VALUES.maximumMapWaypoints.get());
        reconcileExternalSelection();
    }

    private void refreshJourneyMapIfDirty() {
        if (!ClientMapWaypointIntegration.journeyMapDirty()
            || minecraft == null || minecraft.getConnection() == null) return;
        Set<String> dimensions = minecraft.getConnection().levels().stream()
            .map(key -> key.identifier().toString()).collect(java.util.stream.Collectors.toSet());
        if (ClientMapWaypointIntegration.refreshJourneyMapIfDirty(dimensions,
            ClientConfig.VALUES.maximumMapWaypoints.get())) reconcileExternalSelection();
    }

    private void reconcileExternalSelection() {
        boolean removed = ClientMapWaypointIntegration.reconcileSelection();
        if (!removed && !(selectedExternalRow != null
            && ClientMapWaypointIntegration.selected() == null)) return;
        selectedExternalRow = null;
        viewedExternalRow = null;
        if (removed) PortalNetworking.sendRequest(PortalAction.CLEAR_EXTERNAL_DESTINATION);
        updateOpenPortalButton();
    }

    private void clearExternalSelection(boolean notifyServer) {
        selectedExternalRow = null;
        viewedExternalRow = null;
        ClientMapWaypointIntegration.clearSelection();
        if (notifyServer) PortalNetworking.sendRequest(PortalAction.CLEAR_EXTERNAL_DESTINATION);
    }

    private String groupName(UUID id) {
        if (id.equals(PortalPlayerData.DEFAULT_GROUP_ID)) {
            return Component.translatable("screen.riftgun.default_group").getString();
        }
        if (id.equals(PortalPlayerData.SHARED_SECTION_ID)) {
            return Component.translatable("screen.riftgun.shared_group").getString();
        }
        return PortalClientState.data().group(id).map(DestinationGroup::name)
            .orElseGet(() -> Component.translatable("screen.riftgun.default_group").getString());
    }

    private Component gunFluidName() {
        return Component.translatable(fluidTranslationKey(PortalClientState.gun().fluidId()));
    }

    private void label(GuiGraphicsExtractor graphics, String key, int x, int y) {
        graphics.text(font, Component.translatable(key), x, y, PortalTheme.TEXT_MUTED, false);
    }

    private String trim(String value, int maxWidth) {
        if (maxWidth <= 8) return "";
        return font.width(value) <= maxWidth ? value
            : font.plainSubstrByWidth(value, maxWidth - 8) + "…";
    }

    private static String displayDimension(String dimensionId) {
        return DimensionLabelState.label(dimensionId).orElseGet(() -> friendlyDimension(
            dimensionId.substring(dimensionId.lastIndexOf(':') + 1)));
    }

    private static Component visualName(PortalVisualType type) {
        return Component.translatable(type.nameKey());
    }

    private static Component soundName(PortalSoundChannel channel) {
        PortalSoundChoice choice = PortalSoundRegistry.resolve(
            channel, PortalClientState.data().settings().portalSounds().selected(channel));
        return Component.translatable(channel.labelKey()).append(": ")
            .append(Component.translatable(choice.nameKey()));
    }

    private Box modalBox() {
        int boxWidth = Math.min(340, panelWidth - 16);
        return PortalConfigLayout.modalBox(session.page(), width, height, panelWidth,
            gunSettingDescriptionExtraHeight(boxWidth));
    }

    private int visualOptionsMaxScroll(Box box) {
        return maximumScroll(visualOptionsContentHeight, visualOptionsViewportHeight(box));
    }

    private Box dropdownBox(int groupCount) {
        return PortalConfigLayout.dropdownBox(modalBox(), groupSelectorX, groupSelectorY,
            groupSelectorWidth, groupCount, 7);
    }

    private Box visualDropdownBox(int typeCount) {
        return PortalConfigLayout.selectorDropdownBox(modalBox(), visualSelectorX,
            visualSelectorY, visualSelectorWidth, typeCount);
    }

    private Box selectorDropdownBox(ThemedButton selector, int choiceCount) {
        return PortalConfigLayout.selectorDropdownBox(modalBox(), selector.getX(), selector.getY(),
            selector.getWidth() + 22, choiceCount);
    }

    private record VisualWidgetBinding(AbstractWidget widget, int contentOffset) {}
    private record VisualToggleBinding(ThemedButton widget, PortalVisualOption.Toggle option) {}


    private final class MapWaypointLimitSlider extends AbstractSliderButton {
        private MapWaypointLimitSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(),
                (ClientConfig.VALUES.maximumMapWaypoints.get() - 1) / 999.0);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.riftgun.map.maximum_waypoints", amount()));
        }

        @Override
        protected void applyValue() {
            updateMessage();
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            ClientConfig.VALUES.maximumMapWaypoints.set(amount());
            ClientConfig.SPEC.save();
            refreshExternalDestinations(true);
        }

        private int amount() {
            return 1 + (int) Math.round(value * 999.0);
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
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            flushVisualSettings();
        }

        private void refreshFromOption() {
            value = option.normalizedValue();
            active = option.active();
            updateMessage();
        }
    }

    private final class GunDistanceSlider extends AbstractSliderButton {
        private final DistanceSetting setting;
        private final String labelKey;
        private final int minimum;
        private final int maximum;
        private final double displayDivisor;
        private final int permanentValue;
        private final String permanentLabelKey;
        private int committedValue;

        private GunDistanceSlider(int x, int y, int width, int height, DistanceSetting setting,
                                  String labelKey, int minimum, int maximum, int distance) {
            this(x, y, width, height, setting, labelKey, minimum, maximum, distance, 1.0);
        }

        private GunDistanceSlider(int x, int y, int width, int height, DistanceSetting setting,
                                  String labelKey, int minimum, int maximum, int distance,
                                  double displayDivisor) {
            this(x, y, width, height, setting, labelKey, minimum, maximum, distance,
                displayDivisor, 0, null);
        }

        private GunDistanceSlider(int x, int y, int width, int height, DistanceSetting setting,
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
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            commit();
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            boolean handled = super.keyPressed(event);
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
