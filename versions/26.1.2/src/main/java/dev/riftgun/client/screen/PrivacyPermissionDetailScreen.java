package dev.riftgun.client.screen;

import dev.riftgun.client.PrivacyTerminalState;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PlayerPermissionProfile;
import dev.riftgun.data.PortalPermissionPolicy;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Scrollable per-requester permission editor opened from the Privacy Terminal. */
public final class PrivacyPermissionDetailScreen extends Screen {
    private static final int ROW_HEIGHT = 28;
    private final PrivacyTerminalState.PlayerRef player;
    private final Screen parent;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int rowsTop;
    private int rowsBottom;
    private int scroll;
    private @Nullable Identifier expandedPermission;

    PrivacyPermissionDetailScreen(PrivacyTerminalState.PlayerRef player, Screen parent) {
        super(Component.translatable("screen.riftgun.privacy_detail", player.name()));
        this.player = player;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(440, width - 12);
        panelHeight = Math.min(286, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        rowsTop = panelY + 44;
        rowsBottom = panelY + panelHeight - 12;
        addRenderableWidget(new ThemedButton(panelX + panelWidth - 24, panelY + 6, 18, 18,
            Component.literal("X"), false, ignored -> onClose()));
    }

    public void refreshFromServer() {
        expandedPermission = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.text(font, title, panelX + 12, panelY + 10, PortalTheme.TEXT, false);
        renderRows(graphics);
        for (net.minecraft.client.gui.components.Renderable renderable : renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        renderExpanded(graphics);
    }

    private void renderRows(GuiGraphicsExtractor graphics) {
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        int maxScroll = Math.max(0, permissions.size() * ROW_HEIGHT - (rowsBottom - rowsTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        PlayerPermissionProfile profile = PrivacyTerminalState.data().permissionProfile(player.id());
        graphics.enableScissor(panelX + 8, rowsTop, panelX + panelWidth - 8, rowsBottom);
        for (int index = 0; index < permissions.size(); index++) {
            PrivacyTerminalState.PermissionRef permission = permissions.get(index);
            int y = rowsTop - scroll + index * ROW_HEIGHT;
            if (y + ROW_HEIGHT < rowsTop || y > rowsBottom) continue;
            graphics.fill(panelX + 8, y, panelX + panelWidth - 8, y + ROW_HEIGHT - 2, 0xFF25272D);
            graphics.text(font, Component.translatable(permission.translationKey()),
                panelX + 14, y + 9, PortalTheme.TEXT, false);
            PortalPermissionPolicy policy = profile.configured(permission.id());
            PrivacyTerminalScreen.drawPolicySelector(graphics,
                panelX + panelWidth - 120, y + 5, 106, policy, true,
                permission.id().equals(expandedPermission));
        }
        graphics.disableScissor();
    }

    private void renderExpanded(GuiGraphicsExtractor graphics) {
        if (expandedPermission == null) return;
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        int index = indexOf(permissions, expandedPermission);
        if (index < 0) return;
        int x = panelX + panelWidth - 120;
        int y = rowsTop - scroll + index * ROW_HEIGHT + 22;
        PrivacyTerminalScreen.drawPolicyMenu(
            graphics, x, y, 106, permissions.get(index).supportsAsk(), true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (expandedPermission != null) {
            if (handleExpanded(event.x(), event.y(), event.button())) return true;
            expandedPermission = null;
            return true;
        }
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        for (int index = 0; index < permissions.size(); index++) {
            int y = rowsTop - scroll + index * ROW_HEIGHT + 5;
            int x = panelX + panelWidth - 120;
            if (!PrivacyTerminalScreen.inside(event.x(), event.y(), x, y, 106, 16)) continue;
            PrivacyTerminalState.PermissionRef permission = permissions.get(index);
            if (event.x() >= x + 90) {
                expandedPermission = permission.id().equals(expandedPermission) ? null : permission.id();
            } else {
                PortalPermissionPolicy current = PrivacyTerminalState.data()
                    .permissionProfile(player.id()).configured(permission.id());
                PortalPermissionPolicy next = event.button() == 1
                    ? current.previous(permission.supportsAsk()) : current.next(permission.supportsAsk());
                send(permission.id(), next);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleExpanded(double mouseX, double mouseY, int button) {
        if (expandedPermission == null || button != 0) return false;
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        int index = indexOf(permissions, expandedPermission);
        if (index < 0) return false;
        PrivacyTerminalState.PermissionRef permission = permissions.get(index);
        List<PortalPermissionPolicy> options = PrivacyTerminalScreen.policyOptions(
            permission.supportsAsk(), true);
        int x = panelX + panelWidth - 120;
        int y = rowsTop - scroll + index * ROW_HEIGHT + 22;
        if (!PrivacyTerminalScreen.inside(mouseX, mouseY, x, y, 106, options.size() * 18)) return false;
        send(permission.id(), options.get((int) (mouseY - y) / 18));
        expandedPermission = null;
        return true;
    }

    private void send(Identifier permission, PortalPermissionPolicy policy) {
        PortalNetworking.sendRequest(PortalAction.SET_PRIVACY_OVERRIDE, tag -> {
            Nbt.putUUID(tag, "Target", player.id());
            tag.putString("Permission", permission.toString());
            tag.putString("Policy", policy.name());
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        if (mouseY >= rowsTop && mouseY < rowsBottom) {
            scroll -= (int) Math.round(verticalAmount) * ROW_HEIGHT;
            expandedPermission = null;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private static int indexOf(List<PrivacyTerminalState.PermissionRef> permissions,
                               Identifier id) {
        for (int index = 0; index < permissions.size(); index++) {
            if (permissions.get(index).id().equals(id)) return index;
        }
        return -1;
    }
}
