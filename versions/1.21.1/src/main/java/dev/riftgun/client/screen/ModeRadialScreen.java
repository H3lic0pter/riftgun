package dev.riftgun.client.screen;

import dev.riftgun.client.ModeRadialInput;
import dev.riftgun.client.ModeRadialController;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.client.PortalInputLabels;
import dev.riftgun.client.render.PortalPlacementPreview;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.input.SurfaceFacePreviewState;
import dev.riftgun.input.ModeRadialPointerAction;
import dev.riftgun.math.RadialModeGeometry;
import dev.riftgun.math.RadialOptionLabelLayout;
import dev.riftgun.math.RadialRingSpans;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.PrecisionPlacementRequest;
import dev.riftgun.portal.PortalOrientation;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingLabels;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

public final class ModeRadialScreen extends Screen {
    private static final int INNER_RADIUS = 42;
    private static final int OUTER_RADIUS = 100;
    private static final int LABEL_RADIUS = 73;
    private static final int SURFACE_INNER_RADIUS = 35;
    private static final int SURFACE_OUTER_RADIUS = 66;
    private static final int SURFACE_LABEL_RADIUS = 48;
    private static final int SURFACE_EDGE_GAP = 8;
    private static final int SURFACE_TOP_MARGIN = 26;
    private static final int SURFACE_BOTTOM_MARGIN = 31;
    private static final int SURFACE_RING_BACKGROUND_A = 0xA80A0D10;
    private static final int SURFACE_RING_BACKGROUND_B = 0xA812171D;
    private static final int SURFACE_CENTER_BACKGROUND = 0x78333B43;
    private static final int SURFACE_TEXT_BACKGROUND = 0xA012171D;
    private static final int SURFACE_SELECTED_COLOR = 0x906F9AA8;
    private static final int SAMPLE = 3;
    private static final int PLACEMENT_SPRITE_HALF_SIZE = 8;
    private static final int RANGE_SLIDER_WIDTH = 160;
    private static final int RANGE_SLIDER_MIN_Y = 15;
    private static final int RANGE_SLIDER_RING_GAP = 8;
    private static final int RANGE_EMPTY_COLOR = 0xD8383B40;
    private static final int RANGE_FILLED_COLOR = 0xD86E7278;
    private static final int[][] FACE_WIREFRAME_POINTS = {
        {-12, -8}, {12, -8}, {12, 16}, {-12, 16},
        {-4, -16}, {20, -16}, {20, 8}, {-4, 8}
    };
    private static final int[][] FACE_WIREFRAME_EDGES = {
        {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6},
        {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
    };
    private static final int[] FACE_NORTH = {0, 1, 2, 3};
    private static final int[] FACE_SOUTH = {4, 5, 6, 7};
    private static final int[] FACE_UP = {4, 5, 1, 0};
    private static final int[] FACE_DOWN = {3, 2, 6, 7};
    private static final int[] FACE_WEST = {4, 0, 3, 7};
    private static final int[] FACE_EAST = {1, 5, 6, 2};
    private final long openedNanos = System.nanoTime();
    private final ModeRadialController controller;

    public ModeRadialScreen() {
        this((PrecisionPlacementRequest) null);
    }

    public ModeRadialScreen(PrecisionPlacementRequest precisionRequest) {
        super(Component.translatable("screen.riftgun.mode_radial.title"));
        Direction playerHeading = Minecraft.getInstance().player == null
            ? Direction.NORTH : Minecraft.getInstance().player.getDirection();
        controller = new ModeRadialController(
            precisionRequest == null ? null : precisionRequest.toIntent(), playerHeading,
            RiftConfigs.client().surfaceFaceRadialOrder());
        refreshFromServer();
    }

    public void refreshFromServer() {
        controller.refresh(PortalClientState.gun());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.21.1 Screen.render applies the background blur. Run it first so the
        // blur never samples the radial UI drawn below.
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!controller.precisionPreviewOnly()) graphics.fill(0, 0, width, height, 0x78101115);
        List<?> options = options();
        if (controller.select(selectionAt(mouseX, mouseY, options.size()),
            PortalClientState.gun())) playUi(1.25F);
        float animation = PortalClientState.data().settings().animationsEnabled()
            ? Math.min(1.0F, (System.nanoTime() - openedNanos) / 120_000_000.0F) : 1.0F;
        drawRing(graphics, options.size(), animation);
        drawOptions(graphics, options);
        drawCenter(graphics, options);
        drawRangeSlider(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!controller.precisionPreviewOnly()) super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public void commitSelection() {
        commitPrecisionSelection(false);
    }

    public void commitPairingShortcut() {
        commitPrecisionSelection(true);
    }

    private void commitPrecisionSelection(boolean pairingShortcut) {
        if (controller.cancelled() || !controller.precisionPreviewOnly()) return;
        PrecisionPlacementRequest request = PrecisionPlacementRequest.fromIntent(
            controller.selectedPrecisionIntent(PortalPlacementPreview.currentPlacement(), pairingShortcut));
        boolean endpointA = ModeRadialInput.sneakDown();
        PortalNetworking.sendShortcutRequest(PortalAction.OPEN_SELECTED_PRECISION, tag -> {
            request.writeTo(tag);
            tag.putBoolean("EndpointA", endpointA);
            tag.putBoolean("PairingShortcut", pairingShortcut);
        });
    }

    public void commitAndClose() {
        sendRange(true);
        if (!controller.cancelled()) {
            ModeRadialController.RadialSelection selected =
                controller.selectedRadialMode(PortalClientState.gun());
            PortalNetworking.sendShortcutRequest(PortalAction.SET_RADIAL_MODE, tag -> {
                tag.putString("FunctionMode", controller.functionMode().name());
                if (selected != null) {
                    tag.putString("Page", selected.page().name());
                    tag.putString("Mode", selected.mode().name());
                }
            });
        }
        if (minecraft != null) minecraft.setScreen(null);
    }

    public void closeFromShortcutRelease() {
        controller.cancel(false);
        if (minecraft != null) minecraft.setScreen(null);
    }

    public void rejectAndClose() {
        controller.cancel(true);
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!ModeRadialInput.ready()) return true;
        switch (ModeRadialPointerAction.resolve(button, controller.precisionPreviewOnly(),
            overRangeSlider(mouseX, mouseY), controller.page() == ModeRadialController.Page.SURFACE_FACE,
            PortalClientState.gun().pairingInstalled())) {
            case COMMIT_SELECTION -> commitSelection();
            case START_RANGE_DRAG -> {
                controller.draggingRange(true);
                updateRange(mouseX, false);
            }
            case TOGGLE_FUNCTION -> {
                PortalFunctionMode mode = controller.toggleFunctionMode();
                playUi(mode == PortalFunctionMode.PORTAL_PAIRING ? 1.15F : 0.85F);
            }
            case TOGGLE_FACE_FRAME -> {
                playUi(controller.toggleFaceFrame() == SurfaceFacePreviewState.Frame.ABSOLUTE
                    ? 1.1F : 0.9F);
            }
            case SWITCH_PAGE -> {
                controller.switchPage();
                playUi(0.9F);
            }
            case NONE -> {}
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!ModeRadialInput.ready()) return true;
        if (controller.draggingRange() && button == 0) {
            updateRange(mouseX, false);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!ModeRadialInput.ready()) return true;
        if (controller.draggingRange() && button == 0) {
            updateRange(mouseX, true);
            controller.draggingRange(false);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            controller.cancel(false);
            ModeRadialInput.cancelFromScreen();
            onClose();
        }
        return true;
    }

    @Override
    public void onClose() {
        if (controller.shouldSendFinalRange()) sendRange(true);
        controller.cancel(false);
        ModeRadialInput.cancelFromScreen();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean surfaceFacePreviewOpen() { return controller.surfaceFacePreviewOpen(); }
    public boolean floatingOrientationPreviewOpen() {
        return controller.floatingOrientationPreviewOpen();
    }
    public PortalOrientation selectedFloatingOrientation() { return controller.selectedOrientation(); }
    public BlockPos surfaceAnchor() { return controller.surfaceAnchor(); }
    public Direction selectedSurfaceFace() { return controller.selectedSurfaceFace(); }

    private List<?> options() {
        return controller.options(PortalClientState.gun());
    }

    private void drawRing(GuiGraphics graphics, int count, float animation) {
        int centerX = centerX();
        int centerY = centerY();
        int inner = innerRadius();
        int outer = Math.max(inner + 1, Math.round(outerRadius() * animation));
        int sample = controller.precisionPreviewOnly() ? 1 : SAMPLE;
        int selected = controller.precisionPreviewOnly() ? SURFACE_SELECTED_COLOR
            : controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? 0xDC84502D : 0xDC416775;
        int baseA = controller.precisionPreviewOnly() ? SURFACE_RING_BACKGROUND_A
            : controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? 0xD82F2925 : 0xD825272D;
        int baseB = controller.precisionPreviewOnly() ? SURFACE_RING_BACKGROUND_B
            : controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? 0xD83A3028 : 0xD830333A;
        RadialRingSpans.forEach(inner, outer, sample, controller.precisionPreviewOnly(), count,
            (xFrom, y, xTo, height, index) -> graphics.fill(
                centerX + xFrom, centerY + y, centerX + xTo, centerY + y + height,
                index == controller.selection() ? selected : (index & 1) == 0 ? baseA : baseB));
    }

    private void drawOptions(GuiGraphics graphics, List<?> options) {
        int centerX = centerX();
        int centerY = centerY();
        for (int index = 0; index < options.size(); index++) {
            Object option = options.get(index);
            Component label = label(option);
            RadialOptionLabelLayout.Placement layout = RadialOptionLabelLayout.resolve(
                index, options.size(), centerX, centerY, labelRadius(), outerRadius(),
                controller.precisionPreviewOnly()
                    && controller.page() == ModeRadialController.Page.FLOATING_ORIENTATION,
                font.width(label));
            int x = layout.x();
            int y = layout.y();
            int color = index == controller.selection() ? PortalTheme.TEXT : PortalTheme.TEXT_MUTED;
            if (option instanceof PortalPlacementMode mode) {
                PortalGuiIcons.drawPlacementModeIcon(graphics,
                    x - PLACEMENT_SPRITE_HALF_SIZE, y - PLACEMENT_SPRITE_HALF_SIZE, mode);
                centeredWrappedText(graphics, label, x, y + 8, layout.maximumWidth(), color);
            } else {
                centeredWrappedText(graphics, label, x, y - 4, layout.maximumWidth(), color);
            }
        }
    }

    private void drawCenter(GuiGraphics graphics, List<?> options) {
        int centerX = centerX();
        int centerY = centerY();
        if (controller.page() == ModeRadialController.Page.SURFACE_FACE) {
            drawFacePreview(graphics, centerX, centerY);
            return;
        }
        if (controller.page() == ModeRadialController.Page.FLOATING_ORIENTATION) {
            drawFloatingPreview(graphics, centerX, centerY);
            return;
        }
        Component pageLabel = Component.translatable(controller.page() == ModeRadialController.Page.PLACEMENT
            ? "screen.riftgun.mode_radial.placement" : "screen.riftgun.mode_radial.prediction");
        boolean pairing = controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING;
        centeredText(graphics, Component.translatable(pairing
            ? "screen.riftgun.mode_radial.pairing" : "screen.riftgun.mode_radial.coordinate"),
            centerX, centerY - 18,
            controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? PortalTheme.AMBER : PortalTheme.ICE);
        centeredText(graphics, pageLabel, centerX, centerY - 7,
            controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? PortalTheme.AMBER : PortalTheme.ICE);
        Object current = controller.selection() >= 0 ? options.get(controller.selection())
            : controller.page() == ModeRadialController.Page.PLACEMENT
                ? PortalClientState.data().settings().placementMode()
                : PortalClientState.data().settings().predictionMode();
        centeredWrappedText(graphics, label(current), centerX, centerY + 6, 80, PortalTheme.TEXT);
        boolean pairingInstalled = PortalClientState.gun().pairingInstalled();
        int hintReserve = pairingInstalled ? 22 : 12;
        int hintY = Math.min(centerY + outerRadius() + 12, height - hintReserve);
        centeredText(graphics, Component.translatable(controller.page() == ModeRadialController.Page.PLACEMENT
            ? "screen.riftgun.mode_radial.switch_prediction" : "screen.riftgun.mode_radial.switch_placement"),
            centerX, hintY, PortalTheme.TEXT_MUTED);
        if (pairingInstalled) {
            centeredText(graphics, Component.translatable(controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING
                    ? "screen.riftgun.mode_radial.switch_to_coordinate"
                    : "screen.riftgun.mode_radial.switch_to_pairing"),
                centerX, hintY + 10, PortalTheme.TEXT_MUTED);
        }
    }

    private void drawRangeSlider(GuiGraphics graphics) {
        if (!rangeSliderEnabled()) return;
        int x = rangeSliderX();
        int y = rangeSliderY();
        centeredText(graphics, Component.translatable("screen.riftgun.mode_radial.remote_distance",
            controller.remoteDistance(), controller.maximumSurfaceRange()), width / 2, y - 12,
            PortalTheme.TEXT_MUTED);
        graphics.fill(x, y, x + RANGE_SLIDER_WIDTH, y + 4, RANGE_EMPTY_COLOR);
        int filled = controller.maximumSurfaceRange() <= 1 ? RANGE_SLIDER_WIDTH
            : Math.round((controller.remoteDistance() - 1.0F)
                / (controller.maximumSurfaceRange() - 1.0F) * RANGE_SLIDER_WIDTH);
        graphics.fill(x, y, x + filled, y + 4, RANGE_FILLED_COLOR);
        int thumbX = Math.clamp(x + filled, x + 1, x + RANGE_SLIDER_WIDTH - 1);
        graphics.fill(thumbX - 1, y - 2, thumbX + 2, y + 6, PortalTheme.TEXT);
    }

    private void updateRange(double mouseX, boolean forceSend) {
        double fraction = Math.clamp((mouseX - rangeSliderX()) / RANGE_SLIDER_WIDTH, 0.0, 1.0);
        int remoteDistance = controller.updateRange(fraction);
        PortalClientState.updateGun(state -> state.withPlacement(
            state.placement().withRemoteDistance(remoteDistance)));
        sendRange(forceSend);
    }

    private void sendRange(boolean force) {
        long now = System.nanoTime();
        if (!controller.rangeSendDue(force, ModeRadialInput.ready(), rangeSliderEnabled(), now)) return;
        int value = controller.remoteDistance();
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", "RemoteDistance");
            tag.putInt("Value", value);
        });
        controller.rangeSent(now);
    }

    private boolean overRangeSlider(double mouseX, double mouseY) {
        return rangeSliderEnabled() && mouseX >= rangeSliderX()
            && mouseX <= rangeSliderX() + RANGE_SLIDER_WIDTH
            && mouseY >= rangeSliderY() - 4 && mouseY <= rangeSliderY() + 8;
    }

    private int centerY() {
        if (!controller.precisionPreviewOnly()) return height / 2;
        int minimum = SURFACE_OUTER_RADIUS + SURFACE_TOP_MARGIN;
        int maximum = Math.max(minimum, height - SURFACE_OUTER_RADIUS - SURFACE_BOTTOM_MARGIN);
        return Math.clamp(height / 2 + RiftConfigs.client().surfaceFaceRadialOffsetY(),
            minimum, maximum);
    }

    private int centerX() {
        if (!controller.precisionPreviewOnly()) return width / 2;
        int minimum = SURFACE_OUTER_RADIUS + SURFACE_EDGE_GAP;
        int maximum = Math.max(minimum, width - SURFACE_OUTER_RADIUS - SURFACE_EDGE_GAP);
        return Math.clamp(width / 2 + RiftConfigs.client().surfaceFaceRadialOffsetX(),
            minimum, maximum);
    }

    private int innerRadius() {
        return controller.precisionPreviewOnly() ? SURFACE_INNER_RADIUS : INNER_RADIUS;
    }

    private int outerRadius() {
        return controller.precisionPreviewOnly() ? SURFACE_OUTER_RADIUS : OUTER_RADIUS;
    }

    private int labelRadius() {
        return controller.precisionPreviewOnly() ? SURFACE_LABEL_RADIUS : LABEL_RADIUS;
    }

    private int rangeSliderX() {
        return width / 2 - RANGE_SLIDER_WIDTH / 2;
    }

    private int rangeSliderY() {
        return Math.max(RANGE_SLIDER_MIN_Y, centerY() - OUTER_RADIUS - RANGE_SLIDER_RING_GAP);
    }

    private boolean remoteInstalled() {
        return PortalClientState.gun().remoteInstalled();
    }

    private boolean rangeSliderEnabled() {
        return !controller.precisionPreviewOnly() && remoteInstalled()
            && PortalClientState.gun().remoteRadialSliderEnabled();
    }

    private int selectionAt(int mouseX, int mouseY, int optionCount) {
        OptionalInt hovered = overRangeSlider(mouseX, mouseY) || controller.draggingRange()
            ? OptionalInt.empty() : RadialModeGeometry.selectionIndex(
                mouseX - centerX(), mouseY - centerY(), optionCount, innerRadius());
        return hovered.orElse(-1);
    }

    private void drawFacePreview(GuiGraphics graphics, int centerX, int centerY) {
        drawSurfaceCenterBackdrop(graphics, centerX, centerY);
        Component heading = Component.translatable("screen.riftgun.mode_radial.surface_face");
        Component frame = Component.translatable(
            controller.facePreview().frame() == SurfaceFacePreviewState.Frame.RELATIVE
                ? "screen.riftgun.mode_radial.surface_face_relative"
                : "screen.riftgun.mode_radial.surface_face_absolute");
        int headingY = centerY - outerRadius() - 23;
        drawTextBackdrop(graphics, centerX, headingY, heading, frame);
        centeredText(graphics, heading, centerX, headingY, PortalTheme.ICE);
        centeredText(graphics, frame, centerX, headingY + 9, PortalTheme.TEXT);
        drawFaceWireframe(graphics, centerX - 4, centerY - 4,
            controller.facePreview().selectedFace());
        centeredText(graphics, label(controller.facePreview().selectedChoice()), centerX, centerY + 22,
            PortalTheme.TEXT);
        int hintY = Math.min(centerY + outerRadius() + 4,
            height - (controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? 29 : 20));
        Component switchHint = Component.translatable(
            controller.facePreview().frame() == SurfaceFacePreviewState.Frame.RELATIVE
                ? "screen.riftgun.mode_radial.surface_face_switch_absolute"
                : "screen.riftgun.mode_radial.surface_face_switch_relative");
        if (controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING) {
            Component actionB = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_action_pair_b",
                PortalPairingLabels.second());
            Component actionA = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_action_pair_a",
                PortalInputLabels.sneakKey(),
                PortalPairingLabels.first());
            drawTextBackdrop(graphics, centerX, hintY, switchHint, actionB, actionA);
            centeredText(graphics, switchHint, centerX, hintY, PortalTheme.TEXT_MUTED);
            centeredText(graphics, actionB, centerX, hintY + 9, PortalTheme.TEXT_MUTED);
            centeredText(graphics, actionA, centerX, hintY + 18, PortalTheme.TEXT_MUTED);
        } else {
            Component action = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_action");
            drawTextBackdrop(graphics, centerX, hintY, switchHint, action);
            centeredText(graphics, switchHint, centerX, hintY, PortalTheme.TEXT_MUTED);
            centeredText(graphics, action, centerX, hintY + 9, PortalTheme.TEXT_MUTED);
        }
    }

    private void drawFloatingPreview(GuiGraphics graphics, int centerX, int centerY) {
        drawSurfaceCenterBackdrop(graphics, centerX, centerY);
        PortalPlacementMode floatingMode = floatingPlacementMode();
        Component heading = Component.translatable(floatingMode == PortalPlacementMode.REMOTE
            ? "screen.riftgun.mode_radial.portal_direction"
            : "screen.riftgun.mode_radial.portal_position");
        Component kind = label(floatingMode);
        int headingY = centerY - outerRadius() - 23;
        drawTextBackdrop(graphics, centerX, headingY, heading, kind);
        centeredText(graphics, heading, centerX, headingY, PortalTheme.ICE);
        centeredText(graphics, kind, centerX, headingY + 9, PortalTheme.TEXT);

        PrecisionRadialSprites.draw(graphics, centerX, centerY, floatingMode,
            controller.selectedOrientation());
        centeredText(graphics, label(controller.selectedOrientation()), centerX, centerY + 22,
            PortalTheme.TEXT);
        drawActionHints(graphics, centerX);
    }

    private void drawActionHints(GuiGraphics graphics, int centerX) {
        int hintY = Math.min(centerY() + outerRadius() + 4,
            height - (controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING ? 20 : 11));
        if (controller.functionMode() == PortalFunctionMode.PORTAL_PAIRING) {
            Component actionB = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_action_pair_b", PortalPairingLabels.second());
            Component actionA = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_action_pair_a",
                PortalInputLabels.sneakKey(), PortalPairingLabels.first());
            drawTextBackdrop(graphics, centerX, hintY, actionB, actionA);
            centeredText(graphics, actionB, centerX, hintY, PortalTheme.TEXT_MUTED);
            centeredText(graphics, actionA, centerX, hintY + 9, PortalTheme.TEXT_MUTED);
        } else {
            Component action = Component.translatable("screen.riftgun.mode_radial.surface_face_action");
            drawTextBackdrop(graphics, centerX, hintY, action);
            centeredText(graphics, action, centerX, hintY, PortalTheme.TEXT_MUTED);
        }
    }

    private void drawSurfaceCenterBackdrop(GuiGraphics graphics, int centerX, int centerY) {
        int radius = innerRadius() + 1;
        for (int y = -radius; y < radius; y++) {
            double radialY = y + 0.5;
            int maximumX = (int) Math.floor(
                Math.sqrt(radius * radius - radialY * radialY) - 0.5);
            graphics.fill(centerX - maximumX - 1, centerY + y,
                centerX + maximumX + 1, centerY + y + 1, SURFACE_CENTER_BACKGROUND);
        }
    }

    private void drawTextBackdrop(GuiGraphics graphics, int centerX, int topY,
                                  Component... lines) {
        int textWidth = 0;
        for (Component line : lines) textWidth = Math.max(textWidth, font.width(line));
        graphics.fill(centerX - textWidth / 2 - 4, topY - 3,
            centerX + (textWidth + 1) / 2 + 4, topY + lines.length * 9 + 3,
            SURFACE_TEXT_BACKGROUND);
    }

    private void drawFaceWireframe(GuiGraphics graphics, int centerX, int centerY, Direction face) {
        int[] selected = switch (face) {
            case NORTH -> FACE_NORTH;
            case SOUTH -> FACE_SOUTH;
            case UP -> FACE_UP;
            case DOWN -> FACE_DOWN;
            case WEST -> FACE_WEST;
            case EAST -> FACE_EAST;
        };
        for (int[] edge : FACE_WIREFRAME_EDGES) {
            line(graphics, centerX, centerY, FACE_WIREFRAME_POINTS[edge[0]],
                FACE_WIREFRAME_POINTS[edge[1]], 0xD0D9DDE0);
        }
        for (int index = 0; index < selected.length; index++) {
            int[] from = FACE_WIREFRAME_POINTS[selected[index]];
            int[] to = FACE_WIREFRAME_POINTS[selected[(index + 1) % selected.length]];
            line(graphics, centerX, centerY, from, to, 0xFF9CD4E5);
        }
    }

    private void line(GuiGraphics graphics, int centerX, int centerY,
                      int[] from, int[] to, int color) {
        int x = from[0];
        int y = from[1];
        int dx = Math.abs(to[0] - x);
        int sx = x < to[0] ? 1 : -1;
        int dy = -Math.abs(to[1] - y);
        int sy = y < to[1] ? 1 : -1;
        int error = dx + dy;
        while (true) {
            graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            if (x == to[0] && y == to[1]) return;
            int twiceError = error * 2;
            if (twiceError >= dy) {
                error += dy;
                x += sx;
            }
            if (twiceError <= dx) {
                error += dx;
                y += sy;
            }
        }
    }

    private Component label(Object mode) {
        if (mode instanceof PortalPlacementMode placement) {
            return Component.translatable("screen.riftgun.placement_mode."
                + placement.name().toLowerCase(Locale.ROOT));
        }
        if (mode instanceof Direction direction) {
            return Component.translatable("screen.riftgun.surface_face."
                + direction.getName());
        }
        if (mode instanceof PortalOrientation orientation) {
            String prefix = floatingPlacementMode() == PortalPlacementMode.REMOTE
                ? "screen.riftgun.portal_orientation.remote."
                : "screen.riftgun.portal_orientation.front.";
            return Component.translatable(prefix + orientation.name().toLowerCase(Locale.ROOT));
        }
        if (mode instanceof SurfaceFacePreviewState.Choice choice) {
            if (controller.facePreview().frame() == SurfaceFacePreviewState.Frame.ABSOLUTE
                || choice == SurfaceFacePreviewState.Choice.UP
                || choice == SurfaceFacePreviewState.Choice.DOWN) {
                return label(controller.facePreview().resolve(choice));
            }
            return Component.translatable("screen.riftgun.surface_face.relative."
                + choice.name().toLowerCase(Locale.ROOT));
        }
        PortalPredictionMode prediction = (PortalPredictionMode) mode;
        return Component.translatable("screen.riftgun.prediction."
            + prediction.name().toLowerCase(Locale.ROOT));
    }

    private void centeredText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x - font.width(text) / 2, y, color, false);
    }

    private void centeredWrappedText(GuiGraphics graphics, Component text, int x, int y,
                                     int maximumWidth, int color) {
        if (font.width(text) <= maximumWidth) {
            centeredText(graphics, text, x, y, color);
            return;
        }
        List<FormattedCharSequence> lines = font.split(text, maximumWidth);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            graphics.drawString(font, line, x - font.width(line) / 2, y + index * 10, color, false);
        }
    }

    private void playUi(float pitch) {
        if (minecraft != null && PortalClientState.data().settings().soundsEnabled()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    private PortalPlacementMode floatingPlacementMode() {
        return controller.floatingPlacementMode(
            PortalClientState.data().settings().placementMode(), PortalClientState.gun());
    }
}
