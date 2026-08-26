package dev.riftgun.client.screen;

import dev.riftgun.client.ModeRadialInput;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.data.PortalPredictionMode;
import dev.riftgun.math.RadialModeGeometry;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

    public ModeRadialScreen() {
        super(Component.translatable("screen.riftgun.mode_radial.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    public void commitAndClose() {
        if (!cancelled && selection >= 0) {
            Object mode = options().get(selection);
            PortalNetworking.sendShortcutRequest(PortalAction.SET_RADIAL_MODE, tag -> {
                tag.putString("Page", page.name());
                tag.putString("Mode", ((Enum<?>) mode).name());
            });
        }
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1) {
            page = page == Page.PLACEMENT ? Page.PREDICTION : Page.PLACEMENT;
            selection = -1;
            lastAudibleSelection = -1;
            playUi(0.9F);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
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
        if (!PortalClientState.gun().getBoolean("EntityRelocationEnabled").orElse(false)) {
            modes.remove(PortalPlacementMode.ENTITY_RELOCATION);
        }
        return modes;
    }

    private void drawRing(GuiGraphicsExtractor graphics, int count, float animation) {
        int centerX = width / 2;
        int centerY = height / 2;
        int outer = Math.max(INNER_RADIUS + 1, Math.round(OUTER_RADIUS * animation));
        for (int y = -outer; y <= outer; y += SAMPLE) {
            for (int x = -outer; x <= outer; x += SAMPLE) {
                int distanceSquared = x * x + y * y;
                if (distanceSquared < INNER_RADIUS * INNER_RADIUS || distanceSquared > outer * outer) continue;
                int index = RadialModeGeometry.selectionIndex(x, y, count, INNER_RADIUS).orElse(-1);
                int color = index == selection ? 0xDC416775 : (index & 1) == 0 ? 0xD825272D : 0xD830333A;
                graphics.fill(centerX + x, centerY + y, centerX + x + SAMPLE, centerY + y + SAMPLE, color);
            }
        }
    }

    private void drawOptions(GuiGraphicsExtractor graphics, List<?> options) {
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

    private void drawCenter(GuiGraphicsExtractor graphics) {
        int centerX = width / 2;
        int centerY = height / 2;
        Component pageLabel = Component.translatable(page == Page.PLACEMENT
            ? "screen.riftgun.mode_radial.placement" : "screen.riftgun.mode_radial.prediction");
        centeredText(graphics, pageLabel, centerX, centerY - 10, PortalTheme.ICE);
        Object current = selection >= 0 ? options().get(selection)
            : page == Page.PLACEMENT ? PortalClientState.data().settings().placementMode()
                : PortalClientState.data().settings().predictionMode();
        centeredWrappedText(graphics, label(current), centerX, centerY + 3, 80, PortalTheme.TEXT);
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

    private void centeredText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.text(font, text, x - font.width(text) / 2, y, color, false);
    }

    private void centeredWrappedText(GuiGraphicsExtractor graphics, Component text, int x, int y,
                                     int maximumWidth, int color) {
        if (font.width(text) <= maximumWidth) {
            centeredText(graphics, text, x, y, color);
            return;
        }
        List<FormattedCharSequence> lines = font.split(text, maximumWidth);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            graphics.text(font, line, x - font.width(line) / 2, y + index * 10, color, false);
        }
    }

    private void playUi(float pitch) {
        if (minecraft != null && PortalClientState.data().settings().soundsEnabled()) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }

    private enum Page { PLACEMENT, PREDICTION }
}
