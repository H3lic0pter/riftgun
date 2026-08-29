package dev.riftgun.client.screen;

import dev.riftgun.client.ModeRadialInput;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.input.SurfaceFacePreviewState;
import dev.riftgun.math.RadialModeGeometry;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.network.SurfaceFaceRequest;
import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.pairing.PortalFunctionMode;
import dev.riftgun.pairing.PortalPairingLabels;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final int PLACEMENT_ART_HALF_SIZE = 5;
    private static final int RANGE_SLIDER_WIDTH = 160;
    private static final int RANGE_SLIDER_MIN_Y = 15;
    private static final int RANGE_SLIDER_RING_GAP = 8;
    private static final int RANGE_EMPTY_COLOR = 0xD8383B40;
    private static final int RANGE_FILLED_COLOR = 0xD86E7278;
    private static final long RANGE_SEND_INTERVAL_NANOS = 100_000_000L;
    private Page page = Page.PLACEMENT;
    private int selection = -1;
    private int lastAudibleSelection = -1;
    private final long openedNanos = System.nanoTime();
    private boolean cancelled;
    private boolean suppressFinalRange;
    private PortalFunctionMode functionMode;
    private int surfaceRange;
    private int maximumSurfaceRange;
    private boolean draggingRange;
    private int lastSentRange;
    private long lastRangeSendNanos;
    private final SurfaceFacePreviewState facePreview;
    private final boolean surfacePreviewOnly;
    private final BlockPos surfaceAnchor;

    public ModeRadialScreen() {
        this(null);
    }

    public ModeRadialScreen(SurfaceFaceRequest surfaceRequest) {
        super(Component.translatable("screen.riftgun.mode_radial.title"));
        surfacePreviewOnly = surfaceRequest != null;
        surfaceAnchor = surfaceRequest == null ? null : surfaceRequest.anchor();
        Direction referenceFace = surfaceRequest == null ? Direction.NORTH : surfaceRequest.face();
        Direction playerHeading = Minecraft.getInstance().player == null
            ? Direction.NORTH : Minecraft.getInstance().player.getDirection();
        facePreview = new SurfaceFacePreviewState(referenceFace, playerHeading,
            RiftConfigs.client().surfaceFaceRadialOrder());
        if (surfaceRequest != null) {
            page = Page.SURFACE_FACE;
        }
        refreshFromServer();
    }

    public void refreshFromServer() {
        functionMode = parseFunctionMode(Nbt.getString(PortalClientState.gun(), "FunctionMode"));
        maximumSurfaceRange = Math.max(1,
            PortalClientState.gun().getInt("MaximumSurfaceRange"));
        surfaceRange = Math.clamp(PortalClientState.gun().getInt("SurfaceRange"),
            1, maximumSurfaceRange);
        lastSentRange = surfaceRange;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.21.1 Screen.render applies the background blur. Run it first so the
        // blur never samples the radial UI drawn below.
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!surfacePreviewOnly) graphics.fill(0, 0, width, height, 0x78101115);
        List<?> options = options();
        selection = selectionAt(mouseX, mouseY, options.size());
        if (page == Page.SURFACE_FACE && selection >= 0) {
            facePreview.select((SurfaceFacePreviewState.Choice) options.get(selection));
        }
        if (selection != lastAudibleSelection) {
            if (selection >= 0) playUi(1.25F);
            lastAudibleSelection = selection;
        }
        float animation = PortalClientState.data().settings().animationsEnabled()
            ? Math.min(1.0F, (System.nanoTime() - openedNanos) / 120_000_000.0F) : 1.0F;
        drawRing(graphics, options.size(), animation);
        drawOptions(graphics, options);
        drawCenter(graphics);
        drawRangeSlider(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!surfacePreviewOnly) super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    public void commitAndClose() {
        sendRange(true);
        if (!cancelled && surfacePreviewOnly && surfaceAnchor != null) {
            SurfaceFaceRequest request = new SurfaceFaceRequest(
                surfaceAnchor, facePreview.selectedFace());
            PortalNetworking.sendShortcutRequest(PortalAction.OPEN_SELECTED_SURFACE_FACE,
                request::writeTo);
            if (minecraft != null) minecraft.setScreen(null);
            return;
        }
        if (!cancelled) {
            PortalNetworking.sendShortcutRequest(PortalAction.SET_RADIAL_MODE, tag -> {
                tag.putString("FunctionMode", functionMode.name());
                if (page == Page.SURFACE_FACE) {
                    tag.putString("Page", Page.PLACEMENT.name());
                    tag.putString("Mode", PortalPlacementMode.SURFACE.name());
                } else if (selection >= 0) {
                    Object mode = options().get(selection);
                    tag.putString("Page", page.name());
                    tag.putString("Mode", ((Enum<?>) mode).name());
                }
            });
        }
        if (minecraft != null) minecraft.setScreen(null);
    }

    public void rejectAndClose() {
        cancelled = true;
        suppressFinalRange = true;
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!ModeRadialInput.ready()) return true;
        if (button == 0 && overRangeSlider(mouseX, mouseY)) {
            draggingRange = true;
            updateRange(mouseX, false);
        } else if (button == 0 && page != Page.SURFACE_FACE
            && PortalClientState.gun().getBoolean("PortalPairingInstalled")) {
            functionMode = functionMode.toggle();
            playUi(functionMode == PortalFunctionMode.PORTAL_PAIRING ? 1.15F : 0.85F);
        } else if (button == 1 && page == Page.SURFACE_FACE) {
            facePreview.toggleFrame();
            selection = -1;
            lastAudibleSelection = -1;
            playUi(facePreview.frame() == SurfaceFacePreviewState.Frame.ABSOLUTE ? 1.1F : 0.9F);
        } else if (button == 1 && !surfacePreviewOnly) {
            page = page == Page.SURFACE_FACE ? Page.PLACEMENT
                : page == Page.PLACEMENT ? Page.PREDICTION : Page.PLACEMENT;
            selection = -1;
            lastAudibleSelection = -1;
            playUi(0.9F);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!ModeRadialInput.ready()) return true;
        if (draggingRange && button == 0) {
            updateRange(mouseX, false);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!ModeRadialInput.ready()) return true;
        if (draggingRange && button == 0) {
            updateRange(mouseX, true);
            draggingRange = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            cancelled = true;
            ModeRadialInput.cancelFromScreen();
            onClose();
        }
        return true;
    }

    @Override
    public void onClose() {
        if (!suppressFinalRange) sendRange(true);
        cancelled = true;
        ModeRadialInput.cancelFromScreen();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean surfaceFacePreviewOpen() { return surfacePreviewOnly && surfaceAnchor != null; }
    public BlockPos surfaceAnchor() { return surfaceAnchor; }
    public Direction selectedSurfaceFace() { return facePreview.selectedFace(); }

    private List<?> options() {
        if (page == Page.SURFACE_FACE) return facePreview.choices();
        if (page == Page.PREDICTION) return Arrays.asList(PortalPredictionMode.values());
        List<PortalPlacementMode> modes = new ArrayList<>(Arrays.asList(PortalPlacementMode.values()));
        if (!PortalClientState.gun().getBoolean("EntityRelocationEnabled")) {
            modes.remove(PortalPlacementMode.ENTITY_RELOCATION);
        }
        if (!remoteInstalled()) {
            modes.remove(PortalPlacementMode.REMOTE);
        }
        return modes;
    }

    private void drawRing(GuiGraphics graphics, int count, float animation) {
        int centerX = centerX();
        int centerY = centerY();
        int inner = innerRadius();
        int outer = Math.max(inner + 1, Math.round(outerRadius() * animation));
        int sample = surfacePreviewOnly ? 1 : SAMPLE;
        int maximumCoordinate = surfacePreviewOnly ? outer - 1 : outer;
        for (int y = -outer; y <= maximumCoordinate; y += sample) {
            for (int x = -outer; x <= maximumCoordinate; x += sample) {
                double radialX = surfacePreviewOnly ? x + 0.5 : x;
                double radialY = surfacePreviewOnly ? y + 0.5 : y;
                double distanceSquared = radialX * radialX + radialY * radialY;
                if (distanceSquared < inner * inner || distanceSquared > outer * outer) continue;
                int index = RadialModeGeometry.selectionIndex(
                    radialX, radialY, count, inner).orElse(-1);
                int selected = surfacePreviewOnly ? SURFACE_SELECTED_COLOR
                    : functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? 0xDC84502D : 0xDC416775;
                int baseA = surfacePreviewOnly ? SURFACE_RING_BACKGROUND_A
                    : functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? 0xD82F2925 : 0xD825272D;
                int baseB = surfacePreviewOnly ? SURFACE_RING_BACKGROUND_B
                    : functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? 0xD83A3028 : 0xD830333A;
                int color = index == selection ? selected : (index & 1) == 0 ? baseA : baseB;
                graphics.fill(centerX + x, centerY + y,
                    centerX + x + sample, centerY + y + sample, color);
            }
        }
    }

    private void drawOptions(GuiGraphics graphics, List<?> options) {
        int centerX = centerX();
        int centerY = centerY();
        for (int index = 0; index < options.size(); index++) {
            double angle = Math.toRadians(-90.0 + index * 360.0 / options.size());
            int x = centerX + (int) Math.round(Math.cos(angle) * labelRadius());
            int y = centerY + (int) Math.round(Math.sin(angle) * labelRadius());
            Object option = options.get(index);
            Component label = label(option);
            int color = index == selection ? PortalTheme.TEXT : PortalTheme.TEXT_MUTED;
            if (option instanceof PortalPlacementMode mode) {
                PortalGuiIcons.drawPlacementModeIcon(graphics,
                    x - PLACEMENT_ART_HALF_SIZE, y - PLACEMENT_ART_HALF_SIZE, mode);
                centeredWrappedText(graphics, label, x, y + 8, 72, color);
            } else {
                centeredWrappedText(graphics, label, x, y - 4, 72, color);
            }
        }
    }

    private void drawCenter(GuiGraphics graphics) {
        int centerX = centerX();
        int centerY = centerY();
        if (page == Page.SURFACE_FACE) {
            drawFacePreview(graphics, centerX, centerY);
            return;
        }
        Component pageLabel = Component.translatable(page == Page.PLACEMENT
            ? "screen.riftgun.mode_radial.placement" : "screen.riftgun.mode_radial.prediction");
        boolean pairing = functionMode == PortalFunctionMode.PORTAL_PAIRING;
        centeredText(graphics, Component.translatable(pairing
            ? "screen.riftgun.mode_radial.pairing" : "screen.riftgun.mode_radial.coordinate"),
            centerX, centerY - 18,
            functionMode == PortalFunctionMode.PORTAL_PAIRING ? PortalTheme.AMBER : PortalTheme.ICE);
        centeredText(graphics, pageLabel, centerX, centerY - 7,
            functionMode == PortalFunctionMode.PORTAL_PAIRING ? PortalTheme.AMBER : PortalTheme.ICE);
        Object current = selection >= 0 ? options().get(selection)
            : page == Page.PLACEMENT ? PortalClientState.data().settings().placementMode()
                : PortalClientState.data().settings().predictionMode();
        centeredWrappedText(graphics, label(current), centerX, centerY + 6, 80, PortalTheme.TEXT);
        boolean pairingInstalled = Nbt.getBoolean(PortalClientState.gun(), "PortalPairingInstalled");
        int hintReserve = pairingInstalled ? 22 : 12;
        int hintY = Math.min(centerY + outerRadius() + 12, height - hintReserve);
        centeredText(graphics, Component.translatable(page == Page.PLACEMENT
            ? "screen.riftgun.mode_radial.switch_prediction" : "screen.riftgun.mode_radial.switch_placement"),
            centerX, hintY, PortalTheme.TEXT_MUTED);
        if (pairingInstalled) {
            centeredText(graphics, Component.translatable(functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? "screen.riftgun.mode_radial.switch_to_coordinate"
                    : "screen.riftgun.mode_radial.switch_to_pairing"),
                centerX, hintY + 10, PortalTheme.TEXT_MUTED);
        }
    }

    private void drawRangeSlider(GuiGraphics graphics) {
        if (!rangeSliderEnabled()) return;
        int x = rangeSliderX();
        int y = rangeSliderY();
        centeredText(graphics, Component.translatable("screen.riftgun.mode_radial.surface_range",
            surfaceRange, maximumSurfaceRange), width / 2, y - 12, PortalTheme.TEXT_MUTED);
        graphics.fill(x, y, x + RANGE_SLIDER_WIDTH, y + 4, RANGE_EMPTY_COLOR);
        int filled = maximumSurfaceRange <= 1 ? RANGE_SLIDER_WIDTH
            : Math.round((surfaceRange - 1.0F) / (maximumSurfaceRange - 1.0F) * RANGE_SLIDER_WIDTH);
        graphics.fill(x, y, x + filled, y + 4, RANGE_FILLED_COLOR);
        int thumbX = Math.clamp(x + filled, x + 1, x + RANGE_SLIDER_WIDTH - 1);
        graphics.fill(thumbX - 1, y - 2, thumbX + 2, y + 6, PortalTheme.TEXT);
    }

    private void updateRange(double mouseX, boolean forceSend) {
        double fraction = Math.clamp((mouseX - rangeSliderX()) / RANGE_SLIDER_WIDTH, 0.0, 1.0);
        surfaceRange = 1 + (int) Math.round(fraction * (maximumSurfaceRange - 1));
        PortalClientState.gun().putInt("SurfaceRange", surfaceRange);
        sendRange(forceSend);
    }

    private void sendRange(boolean force) {
        if (!ModeRadialInput.ready() || !rangeSliderEnabled() || surfaceRange == lastSentRange) return;
        long now = System.nanoTime();
        if (!force && now - lastRangeSendNanos < RANGE_SEND_INTERVAL_NANOS) return;
        int value = surfaceRange;
        PortalNetworking.sendRequest(PortalAction.SET_GUN_MODULE_SETTINGS, tag -> {
            tag.putString("Setting", "SurfaceRange");
            tag.putInt("Value", value);
        });
        lastSentRange = value;
        lastRangeSendNanos = now;
    }

    private boolean overRangeSlider(double mouseX, double mouseY) {
        return rangeSliderEnabled() && mouseX >= rangeSliderX()
            && mouseX <= rangeSliderX() + RANGE_SLIDER_WIDTH
            && mouseY >= rangeSliderY() - 4 && mouseY <= rangeSliderY() + 8;
    }

    private int centerY() {
        if (!surfacePreviewOnly) return height / 2;
        int minimum = SURFACE_OUTER_RADIUS + SURFACE_TOP_MARGIN;
        int maximum = Math.max(minimum, height - SURFACE_OUTER_RADIUS - SURFACE_BOTTOM_MARGIN);
        return Math.clamp(height / 2 + RiftConfigs.client().surfaceFaceRadialOffsetY(),
            minimum, maximum);
    }

    private int centerX() {
        if (!surfacePreviewOnly) return width / 2;
        int minimum = SURFACE_OUTER_RADIUS + SURFACE_EDGE_GAP;
        int maximum = Math.max(minimum, width - SURFACE_OUTER_RADIUS - SURFACE_EDGE_GAP);
        return Math.clamp(width / 2 + RiftConfigs.client().surfaceFaceRadialOffsetX(),
            minimum, maximum);
    }

    private int innerRadius() {
        return surfacePreviewOnly ? SURFACE_INNER_RADIUS : INNER_RADIUS;
    }

    private int outerRadius() {
        return surfacePreviewOnly ? SURFACE_OUTER_RADIUS : OUTER_RADIUS;
    }

    private int labelRadius() {
        return surfacePreviewOnly ? SURFACE_LABEL_RADIUS : LABEL_RADIUS;
    }

    private int rangeSliderX() {
        return width / 2 - RANGE_SLIDER_WIDTH / 2;
    }

    private int rangeSliderY() {
        return Math.max(RANGE_SLIDER_MIN_Y, centerY() - OUTER_RADIUS - RANGE_SLIDER_RING_GAP);
    }

    private boolean remoteInstalled() {
        return Nbt.getBoolean(PortalClientState.gun(), "RemoteInstalled");
    }

    private boolean rangeSliderEnabled() {
        return page != Page.SURFACE_FACE && remoteInstalled()
            && Nbt.getBoolean(PortalClientState.gun(), "RemoteRadialSliderEnabled");
    }

    private int selectionAt(int mouseX, int mouseY, int optionCount) {
        OptionalInt hovered = overRangeSlider(mouseX, mouseY) || draggingRange
            ? OptionalInt.empty() : RadialModeGeometry.selectionIndex(
                mouseX - centerX(), mouseY - centerY(), optionCount, innerRadius());
        return hovered.orElse(-1);
    }

    private void drawFacePreview(GuiGraphics graphics, int centerX, int centerY) {
        drawSurfaceCenterBackdrop(graphics, centerX, centerY);
        Component heading = Component.translatable("screen.riftgun.mode_radial.surface_face");
        Component frame = Component.translatable(
            facePreview.frame() == SurfaceFacePreviewState.Frame.RELATIVE
                ? "screen.riftgun.mode_radial.surface_face_relative"
                : "screen.riftgun.mode_radial.surface_face_absolute");
        int headingY = centerY - outerRadius() - 23;
        drawTextBackdrop(graphics, centerX, headingY, heading, frame);
        centeredText(graphics, heading, centerX, headingY, PortalTheme.ICE);
        centeredText(graphics, frame, centerX, headingY + 9, PortalTheme.TEXT);
        drawFaceWireframe(graphics, centerX - 4, centerY - 4, facePreview.selectedFace());
        centeredText(graphics, label(facePreview.selectedChoice()), centerX, centerY + 22,
            PortalTheme.TEXT);
        int hintY = Math.min(centerY + outerRadius() + 4,
            height - (functionMode == PortalFunctionMode.PORTAL_PAIRING ? 29 : 20));
        Component switchHint = Component.translatable(
            facePreview.frame() == SurfaceFacePreviewState.Frame.RELATIVE
                ? "screen.riftgun.mode_radial.surface_face_switch_absolute"
                : "screen.riftgun.mode_radial.surface_face_switch_relative");
        if (functionMode == PortalFunctionMode.PORTAL_PAIRING) {
            Component releaseB = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_release_pair_b",
                PortalPairingLabels.second());
            Component releaseA = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_release_pair_a",
                PortalPairingLabels.first());
            drawTextBackdrop(graphics, centerX, hintY, switchHint, releaseB, releaseA);
            centeredText(graphics, switchHint, centerX, hintY, PortalTheme.TEXT_MUTED);
            centeredText(graphics, releaseB, centerX, hintY + 9, PortalTheme.TEXT_MUTED);
            centeredText(graphics, releaseA, centerX, hintY + 18, PortalTheme.TEXT_MUTED);
        } else {
            Component release = Component.translatable(
                "screen.riftgun.mode_radial.surface_face_release");
            drawTextBackdrop(graphics, centerX, hintY, switchHint, release);
            centeredText(graphics, switchHint, centerX, hintY, PortalTheme.TEXT_MUTED);
            centeredText(graphics, release, centerX, hintY + 9, PortalTheme.TEXT_MUTED);
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
        int[][] points = {
            {-12, -8}, {12, -8}, {12, 16}, {-12, 16},
            {-4, -16}, {20, -16}, {20, 8}, {-4, 8}
        };
        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6},
            {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        int[] selected = switch (face) {
            case NORTH -> new int[] {0, 1, 2, 3};
            case SOUTH -> new int[] {4, 5, 6, 7};
            case UP -> new int[] {4, 5, 1, 0};
            case DOWN -> new int[] {3, 2, 6, 7};
            case WEST -> new int[] {4, 0, 3, 7};
            case EAST -> new int[] {1, 5, 6, 2};
        };
        for (int[] edge : edges) {
            line(graphics, centerX, centerY, points[edge[0]], points[edge[1]], 0xD0D9DDE0);
        }
        for (int index = 0; index < selected.length; index++) {
            int[] from = points[selected[index]];
            int[] to = points[selected[(index + 1) % selected.length]];
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
        if (mode instanceof SurfaceFacePreviewState.Choice choice) {
            if (facePreview.frame() == SurfaceFacePreviewState.Frame.ABSOLUTE
                || choice == SurfaceFacePreviewState.Choice.UP
                || choice == SurfaceFacePreviewState.Choice.DOWN) {
                return label(facePreview.resolve(choice));
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

    private static PortalFunctionMode parseFunctionMode(String value) {
        try {
            return PortalFunctionMode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return PortalFunctionMode.COORDINATE_TRAVEL;
        }
    }

    private enum Page { PLACEMENT, PREDICTION, SURFACE_FACE }
}
