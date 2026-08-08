package dev.riftgun.client.screen;

import dev.riftgun.client.PrivacyTerminalState;
import dev.riftgun.data.PlayerPermissionOverride;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.TargetPrivacy;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Configures Player Portal privacy: target privacy, transit privacy, and per-player overrides. */
public final class PrivacyTerminalScreen extends Screen {
    private static final int HEADER_HEIGHT = 48;
    private static final int ROW_HEIGHT = 20;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int listTop;
    private int listBottom;
    private int listScroll;
    private int listContentHeight;
    private @Nullable ThemedButton privacyButton;
    private @Nullable ThemedButton transitButton;
    private @Nullable ThemedButton refreshButton;
    private @Nullable ThemedButton closeButton;

    public PrivacyTerminalScreen() {
        super(Component.translatable("screen.riftgun.privacy_terminal"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 12);
        panelHeight = Math.min(320, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listWidth = Math.max(156, panelWidth * 57 / 100);
        listTop = panelY + HEADER_HEIGHT;
        listBottom = panelY + panelHeight - 12;

        closeButton = button(panelX + panelWidth - 24, panelY + 6, 18, 18,
            Component.literal("X"), false, ignored -> onClose());

        PortalPlayerData data = PrivacyTerminalState.data();
        int settingsX = panelX + listWidth + 16;
        int settingsWidth = panelX + panelWidth - 8 - settingsX;
        privacyButton = button(settingsX, panelY + 64, settingsWidth, 20,
            targetLabel(data.targetPrivacy()), false, ignored -> cyclePrivacy());
        transitButton = button(settingsX, panelY + 92, settingsWidth, 20,
            transitLabel(data.transitPrivacyEnabled()), false, ignored -> toggleTransit());
        refreshButton = button(panelX + listWidth - 22, listTop + 2, 16, 16,
            Component.empty(), false,
            ignored -> PortalNetworking.sendRequest(PortalAction.REQUEST_PRIVACY_PLAYERS));
    }

    private ThemedButton button(int x, int y, int width, int height, Component label, boolean portalAction,
                                java.util.function.Consumer<ThemedButton> action) {
        return addRenderableWidget(new ThemedButton(x, y, width, height, label, portalAction, action));
    }

    private Component targetLabel(TargetPrivacy privacy) {
        return Component.translatable("screen.riftgun.privacy_target",
            Component.translatable("screen.riftgun.privacy." + privacy.name().toLowerCase()));
    }

    private Component transitLabel(boolean enabled) {
        return Component.translatable(enabled
            ? "screen.riftgun.privacy_transit_on" : "screen.riftgun.privacy_transit_off");
    }

    /** Rebuilds button labels after a server snapshot arrives. */
    public void refreshFromServer() {
        if (privacyButton != null) {
            privacyButton.setMessage(targetLabel(PrivacyTerminalState.data().targetPrivacy()));
        }
        if (transitButton != null) {
            transitButton.setMessage(transitLabel(PrivacyTerminalState.data().transitPrivacyEnabled()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.fill(panelX, panelY + HEADER_HEIGHT - 1, panelX + panelWidth,
            panelY + HEADER_HEIGHT, PortalTheme.BORDER);
        graphics.fill(panelX + listWidth, panelY + HEADER_HEIGHT, panelX + listWidth + 1,
            panelY + panelHeight - 12, PortalTheme.BORDER);
        graphics.drawString(font, title, panelX + 12, panelY + 10, PortalTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.riftgun.privacy_override_header"),
            panelX + 16, listTop + 6, PortalTheme.TEXT_MUTED, false);

        renderOverrideRows(graphics, mouseX, mouseY);

        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        drawRefreshIcon(graphics);
        if (privacyButton != null && privacyButton.isHovered()) {
            graphics.renderTooltip(font, Component.translatable("screen.riftgun.privacy_target_hint"),
                mouseX, mouseY);
        }
        if (transitButton != null && transitButton.isHovered()) {
            boolean enabled = PrivacyTerminalState.data().transitPrivacyEnabled();
            graphics.renderTooltip(font, Component.translatable(enabled
                ? "screen.riftgun.privacy_transit_on_hint"
                : "screen.riftgun.privacy_transit_off_hint"), mouseX, mouseY);
        }
    }

    private void drawRefreshIcon(GuiGraphics graphics) {
        if (refreshButton == null) return;
        PortalGuiSprites.draw(graphics, PortalGuiSprites.PLAYER_REFRESH,
            refreshButton.getX(), refreshButton.getY());
    }

    private void renderOverrideRows(GuiGraphics graphics, int mouseX, int mouseY) {
        List<PrivacyTerminalState.PlayerRef> players = PrivacyTerminalState.players();
        int rowsTop = listTop + 20;
        listContentHeight = players.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, listContentHeight - (listBottom - rowsTop));
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        graphics.enableScissor(panelX + 8, rowsTop, panelX + listWidth - 8, listBottom);
        PortalPlayerData data = PrivacyTerminalState.data();
        for (int index = 0; index < players.size(); index++) {
            PrivacyTerminalState.PlayerRef player = players.get(index);
            int rowY = rowsTop - listScroll + index * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < rowsTop || rowY > listBottom) continue;
            boolean hover = mouseX >= panelX + 8 && mouseX < panelX + listWidth - 8
                && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            graphics.fill(panelX + 8, rowY, panelX + listWidth - 8, rowY + ROW_HEIGHT,
                hover ? 0xFF30333A : 0xFF25272D);
            graphics.drawString(font, Component.literal(player.name()),
                panelX + 16, rowY + 6, PortalTheme.TEXT, false);
            PlayerPermissionOverride mode = data.privacyOverride(player.id());
            String modeKey = "screen.riftgun.privacy.mode." + mode.name().toLowerCase();
            graphics.drawString(font, Component.translatable(modeKey),
                panelX + listWidth - 14 - font.width(Component.translatable(modeKey)),
                rowY + 6, modeColor(mode), false);
        }
        graphics.disableScissor();
    }

    private int modeColor(PlayerPermissionOverride mode) {
        return switch (mode) {
            case ALLOW -> PortalTheme.PORTAL;
            case DENY -> PortalTheme.DANGER;
            case DEFAULT -> PortalTheme.TEXT_MUTED;
        };
    }

    private void cyclePrivacy() {
        PortalPlayerData data = PrivacyTerminalState.data();
        sendPrivacy(data.targetPrivacy().next(), data.transitPrivacyEnabled());
    }

    private void toggleTransit() {
        PortalPlayerData data = PrivacyTerminalState.data();
        sendPrivacy(data.targetPrivacy(), !data.transitPrivacyEnabled());
    }

    private void sendPrivacy(TargetPrivacy privacy, boolean transit) {
        PortalNetworking.sendRequest(PortalAction.SET_PRIVACY, tag -> {
            tag.putString("Privacy", privacy.name());
            tag.putBoolean("TransitPrivacy", transit);
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rowsTop = listTop + 20;
        if (button == 0 && mouseX >= panelX + 8 && mouseX < panelX + listWidth - 8
            && mouseY >= rowsTop && mouseY < listBottom) {
            int index = (int) (mouseY - rowsTop + listScroll) / ROW_HEIGHT;
            List<PrivacyTerminalState.PlayerRef> players = PrivacyTerminalState.players();
            if (index >= 0 && index < players.size()) {
                cycleOverride(players.get(index));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void cycleOverride(PrivacyTerminalState.PlayerRef player) {
        PortalPlayerData data = PrivacyTerminalState.data();
        PlayerPermissionOverride next = data.privacyOverride(player.id()).next();
        PortalNetworking.sendRequest(PortalAction.SET_PRIVACY_OVERRIDE, tag -> {
            tag.putUUID("Target", player.id());
            tag.putString("Mode", next.name());
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int rowsTop = listTop + 20;
        if (mouseX >= panelX && mouseX < panelX + listWidth
            && mouseY >= rowsTop && mouseY < listBottom) {
            listScroll -= (int) Math.round(verticalAmount) * ROW_HEIGHT;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
