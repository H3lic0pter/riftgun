package dev.riftgun.client.screen;

import dev.riftgun.client.ModeRadialInput;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.math.RadialModeGeometry;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.pairing.PortalFunctionMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

public final class ModeRadialScreen extends Screen {
    private static final int INNER_RADIUS = 42;
    private static final int OUTER_RADIUS = 100;
    private static final int LABEL_RADIUS = 73;
    private static final int SAMPLE = 3;
    private static final int PLACEMENT_ART_HALF_SIZE = 5;
    private Page page = Page.PLACEMENT;
    private int selection = -1;
    private int lastAudibleSelection = -1;
    private final long openedNanos = System.nanoTime();
    private boolean cancelled;
    private PortalFunctionMode functionMode;

    public ModeRadialScreen() {
        super(Component.translatable("screen.riftgun.mode_radial.title"));
        functionMode = parseFunctionMode(Nbt.getString(PortalClientState.gun(), "FunctionMode"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 1.21.1 Screen.render applies the background blur. Run it first so the
        // blur never samples the radial UI drawn below.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x78101115);
        List<?> options = options();
        OptionalInt hovered = RadialModeGeometry.selectionIndex(
            mouseX - width / 2.0, mouseY - height / 2.0, options.size(), INNER_RADIUS);
        selection = hovered.orElse(-1);
        if (selection != lastAudibleSelection) {
            if (selection >= 0) playUi(1.25F);
            lastAudibleSelection = selection;
        }
        float animation = PortalClientState.data().settings().animationsEnabled()
            ? Math.min(1.0F, (System.nanoTime() - openedNanos) / 120_000_000.0F) : 1.0F;
        drawRing(graphics, options.size(), animation);
        drawOptions(graphics, options);
        drawCenter(graphics);
    }

    public void commitAndClose() {
        if (!cancelled) {
            PortalNetworking.sendShortcutRequest(PortalAction.SET_RADIAL_MODE, tag -> {
                tag.putString("FunctionMode", functionMode.name());
                if (selection >= 0) {
                    Object mode = options().get(selection);
                    tag.putString("Page", page.name());
                    tag.putString("Mode", ((Enum<?>) mode).name());
                }
            });
        }
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && PortalClientState.gun().getBoolean("PortalPairingInstalled")) {
            functionMode = functionMode.toggle();
            playUi(functionMode == PortalFunctionMode.PORTAL_PAIRING ? 1.15F : 0.85F);
        } else if (button == 1) {
            page = page == Page.PLACEMENT ? Page.PREDICTION : Page.PLACEMENT;
            selection = -1;
            lastAudibleSelection = -1;
            playUi(0.9F);
        }
        return true;
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
        cancelled = true;
        ModeRadialInput.cancelFromScreen();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<?> options() {
        if (page == Page.PREDICTION) return Arrays.asList(PortalPredictionMode.values());
        List<PortalPlacementMode> modes = new ArrayList<>(Arrays.asList(PortalPlacementMode.values()));
        if (!PortalClientState.gun().getBoolean("EntityRelocationEnabled")) {
            modes.remove(PortalPlacementMode.ENTITY_RELOCATION);
        }
        if (!PortalClientState.gun().getBoolean("PortalPairingInstalled")) {
            modes.remove(PortalPlacementMode.REMOTE);
        }
        return modes;
    }

    private void drawRing(GuiGraphics graphics, int count, float animation) {
        int centerX = width / 2;
        int centerY = height / 2;
        int outer = Math.max(INNER_RADIUS + 1, Math.round(OUTER_RADIUS * animation));
        for (int y = -outer; y <= outer; y += SAMPLE) {
            for (int x = -outer; x <= outer; x += SAMPLE) {
                int distanceSquared = x * x + y * y;
                if (distanceSquared < INNER_RADIUS * INNER_RADIUS || distanceSquared > outer * outer) continue;
                int index = RadialModeGeometry.selectionIndex(x, y, count, INNER_RADIUS).orElse(-1);
                int selected = functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? 0xDC84502D : 0xDC416775;
                int baseA = functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? 0xD82F2925 : 0xD825272D;
                int baseB = functionMode == PortalFunctionMode.PORTAL_PAIRING
                    ? 0xD83A3028 : 0xD830333A;
                int color = index == selection ? selected : (index & 1) == 0 ? baseA : baseB;
                graphics.fill(centerX + x, centerY + y, centerX + x + SAMPLE, centerY + y + SAMPLE, color);
            }
        }
    }

    private void drawOptions(GuiGraphics graphics, List<?> options) {
        int centerX = width / 2;
        int centerY = height / 2;
        for (int index = 0; index < options.size(); index++) {
            double angle = Math.toRadians(-90.0 + index * 360.0 / options.size());
            int x = centerX + (int) Math.round(Math.cos(angle) * LABEL_RADIUS);
            int y = centerY + (int) Math.round(Math.sin(angle) * LABEL_RADIUS);
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
        int centerX = width / 2;
        int centerY = height / 2;
        Component pageLabel = Component.translatable(page == Page.PLACEMENT
            ? "screen.riftgun.mode_radial.placement" : "screen.riftgun.mode_radial.prediction");
        centeredText(graphics, Component.translatable("screen.riftgun.function_mode."
            + functionMode.name().toLowerCase(Locale.ROOT)), centerX, centerY - 18,
            functionMode == PortalFunctionMode.PORTAL_PAIRING ? PortalTheme.AMBER : PortalTheme.ICE);
        centeredText(graphics, pageLabel, centerX, centerY - 7,
            functionMode == PortalFunctionMode.PORTAL_PAIRING ? PortalTheme.AMBER : PortalTheme.ICE);
        Object current = selection >= 0 ? options().get(selection)
            : page == Page.PLACEMENT ? PortalClientState.data().settings().placementMode()
                : PortalClientState.data().settings().predictionMode();
        centeredWrappedText(graphics, label(current), centerX, centerY + 6, 80, PortalTheme.TEXT);
        centeredText(graphics, Component.translatable(page == Page.PLACEMENT
            ? "screen.riftgun.mode_radial.switch_prediction" : "screen.riftgun.mode_radial.switch_placement"),
            centerX, centerY + OUTER_RADIUS + 12, PortalTheme.TEXT_MUTED);
    }

    private Component label(Object mode) {
        if (mode instanceof PortalPlacementMode placement) {
            return Component.translatable("screen.riftgun.placement_mode."
                + placement.name().toLowerCase(Locale.ROOT));
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

    private enum Page { PLACEMENT, PREDICTION }
}
