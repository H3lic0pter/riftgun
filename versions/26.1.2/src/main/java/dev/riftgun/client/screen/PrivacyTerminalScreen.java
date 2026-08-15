package dev.riftgun.client.screen;

import dev.riftgun.client.PrivacyTerminalState;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PlayerPermissionProfileMode;
import dev.riftgun.data.PortalPermissionPolicy;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.network.PortalAction;
import dev.riftgun.network.PortalNetworking;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

/** Privacy overview: requester profiles on the left and the owner's global defaults on the right. */
public final class PrivacyTerminalScreen extends Screen {
    private static final int HEADER_HEIGHT = 48;
    private static final int ROW_HEIGHT = 20;
    private static final int GLOBAL_ROW_HEIGHT = 34;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int listTop;
    private int listBottom;
    private int listScroll;
    private int globalScroll;
    private @Nullable Identifier expandedPermission;
    private @Nullable ThemedButton refreshButton;

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
        button(panelX + panelWidth - 24, panelY + 6, 18, 18,
            Component.literal("X"), ignored -> onClose());
        refreshButton = button(panelX + listWidth - 22, listTop + 2, 16, 16,
            Component.empty(), ignored ->
                PortalNetworking.sendRequest(PortalAction.REQUEST_PRIVACY_PLAYERS));
    }

    private ThemedButton button(int x, int y, int width, int height, Component label,
                                java.util.function.Consumer<ThemedButton> action) {
        return addRenderableWidget(new ThemedButton(x, y, width, height, label, false, action));
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
        graphics.fill(panelX, panelY + HEADER_HEIGHT - 1, panelX + panelWidth,
            panelY + HEADER_HEIGHT, PortalTheme.BORDER);
        graphics.fill(panelX + listWidth, panelY + HEADER_HEIGHT, panelX + listWidth + 1,
            panelY + panelHeight - 12, PortalTheme.BORDER);
        graphics.text(font, title, panelX + 12, panelY + 10, PortalTheme.TEXT, false);
        graphics.text(font, Component.translatable("screen.riftgun.privacy_override_header"),
            panelX + 16, listTop + 6, PortalTheme.TEXT_MUTED, false);
        graphics.text(font, Component.translatable("screen.riftgun.privacy_global_header"),
            panelX + listWidth + 10, listTop + 6, PortalTheme.TEXT_MUTED, false);

        renderPlayerRows(graphics, mouseX, mouseY);
        renderGlobalRows(graphics, mouseX, mouseY);
        for (Renderable renderable : renderables) renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (refreshButton != null) PortalGuiSprites.draw(graphics, PortalGuiSprites.PLAYER_REFRESH,
            refreshButton.getX(), refreshButton.getY());
        renderExpandedGlobal(graphics, mouseX, mouseY);
    }

    private void renderPlayerRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int rowsTop = listTop + 20;
        List<PrivacyTerminalState.PlayerRef> players = PrivacyTerminalState.players();
        int maxScroll = Math.max(0, players.size() * ROW_HEIGHT - (listBottom - rowsTop));
        listScroll = Mth.clamp(listScroll, 0, maxScroll);
        graphics.enableScissor(panelX + 8, rowsTop, panelX + listWidth - 8, listBottom);
        PortalPlayerData data = PrivacyTerminalState.data();
        for (int index = 0; index < players.size(); index++) {
            PrivacyTerminalState.PlayerRef player = players.get(index);
            int y = rowsTop - listScroll + index * ROW_HEIGHT;
            if (y + ROW_HEIGHT < rowsTop || y > listBottom) continue;
            boolean hover = inside(mouseX, mouseY, panelX + 8, y, listWidth - 16, ROW_HEIGHT);
            graphics.fill(panelX + 8, y, panelX + listWidth - 8, y + ROW_HEIGHT,
                hover ? 0xFF30333A : 0xFF25272D);
            graphics.text(font, Component.literal(player.name()), panelX + 16, y + 6,
                PortalTheme.TEXT, false);
            PlayerPermissionProfileMode mode = data.permissionProfile(player.id()).mode();
            Component label = profileLabel(mode);
            int detailX = panelX + listWidth - 28;
            graphics.text(font, label, detailX - 6 - font.width(label), y + 6,
                profileColor(mode), false);
            drawDetailIcon(graphics, detailX, y + 2, hover);
        }
        graphics.disableScissor();
    }

    private void renderGlobalRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = panelX + listWidth + 8;
        int top = listTop + 20;
        int width = panelX + panelWidth - 8 - x;
        int selectorX = x + width - 104;
        int textLeft = x + 5;
        int textRight = x + width - 5;
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        int maxScroll = Math.max(0, permissions.size() * GLOBAL_ROW_HEIGHT - (listBottom - top));
        globalScroll = Mth.clamp(globalScroll, 0, maxScroll);
        graphics.enableScissor(x, top, x + width, listBottom);
        PortalPlayerData data = PrivacyTerminalState.data();
        for (int index = 0; index < permissions.size(); index++) {
            PrivacyTerminalState.PermissionRef permission = permissions.get(index);
            int y = top - globalScroll + index * GLOBAL_ROW_HEIGHT;
            if (y + GLOBAL_ROW_HEIGHT < top || y > listBottom) continue;
            graphics.fill(x, y, x + width, y + GLOBAL_ROW_HEIGHT - 2, 0xFF25272D);
            Component title = Component.translatable(permission.translationKey());
            int titleOffset = GuiTextMarquee.offset(font.width(title), textRight - textLeft,
                Util.getMillis());
            graphics.enableScissor(textLeft, y + 2, textRight, y + 14);
            graphics.text(font, title, textLeft - titleOffset, y + 4,
                PortalTheme.TEXT, false);
            graphics.disableScissor();
            drawPolicySelector(graphics, selectorX, y + 15, 100,
                data.globalPermission(permission.id()), false,
                expandedPermission != null && expandedPermission.equals(permission.id()));
        }
        graphics.disableScissor();
    }

    private void renderExpandedGlobal(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (expandedPermission == null) return;
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        int index = indexOf(permissions, expandedPermission);
        if (index < 0) return;
        int x = panelX + panelWidth - 112;
        int y = listTop + 20 - globalScroll + index * GLOBAL_ROW_HEIGHT + 33;
        drawPolicyMenu(graphics, x, y, 100, permissions.get(index).supportsAsk(), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (expandedPermission != null) {
            if (handleExpandedGlobal(event.x(), event.y(), event.button())) return true;
            expandedPermission = null;
            return true;
        }
        int rowsTop = listTop + 20;
        if (inside(event.x(), event.y(), panelX + 8, rowsTop, listWidth - 16, listBottom - rowsTop)) {
            int index = (int) (event.y() - rowsTop + listScroll) / ROW_HEIGHT;
            List<PrivacyTerminalState.PlayerRef> players = PrivacyTerminalState.players();
            if (index >= 0 && index < players.size()) {
                PrivacyTerminalState.PlayerRef player = players.get(index);
                int detailX = panelX + listWidth - 28;
                int rowY = rowsTop - listScroll + index * ROW_HEIGHT;
                if (inside(event.x(), event.y(), detailX, rowY + 2, 16, 16)) {
                    minecraft.setScreen(new PrivacyPermissionDetailScreen(player, this));
                } else if (event.button() == 0) {
                    cycleProfile(player);
                }
                return true;
            }
        }
        if (handleGlobalSelector(event.x(), event.y(), event.button())) return true;
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handleGlobalSelector(double mouseX, double mouseY, int button) {
        int x = panelX + listWidth + 8;
        int top = listTop + 20;
        int width = panelX + panelWidth - 8 - x;
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        for (int index = 0; index < permissions.size(); index++) {
            int y = top - globalScroll + index * GLOBAL_ROW_HEIGHT + 15;
            int selectorX = x + width - 104;
            if (!inside(mouseX, mouseY, selectorX, y, 100, 16)) continue;
            PrivacyTerminalState.PermissionRef permission = permissions.get(index);
            if (mouseX >= selectorX + 84) {
                expandedPermission = permission.id().equals(expandedPermission) ? null : permission.id();
            } else {
                PortalPermissionPolicy current = PrivacyTerminalState.data().globalPermission(permission.id());
                PortalPermissionPolicy next = button == 1
                    ? previousGlobal(current, permission.supportsAsk())
                    : nextGlobal(current, permission.supportsAsk());
                sendGlobal(permission.id(), next);
            }
            return true;
        }
        return false;
    }

    private boolean handleExpandedGlobal(double mouseX, double mouseY, int button) {
        if (expandedPermission == null || button != 0) return false;
        List<PrivacyTerminalState.PermissionRef> permissions = PrivacyTerminalState.permissions();
        int index = indexOf(permissions, expandedPermission);
        if (index < 0) return false;
        PrivacyTerminalState.PermissionRef permission = permissions.get(index);
        List<PortalPermissionPolicy> options = policyOptions(permission.supportsAsk(), false);
        int x = panelX + panelWidth - 112;
        int y = listTop + 20 - globalScroll + index * GLOBAL_ROW_HEIGHT + 33;
        if (!inside(mouseX, mouseY, x, y, 100, options.size() * 18)) return false;
        int option = (int) (mouseY - y) / 18;
        sendGlobal(permission.id(), options.get(option));
        expandedPermission = null;
        return true;
    }

    private void cycleProfile(PrivacyTerminalState.PlayerRef player) {
        PlayerPermissionProfileMode next = PrivacyTerminalState.data()
            .permissionProfile(player.id()).mode().nextPreset();
        PortalNetworking.sendRequest(PortalAction.SET_PRIVACY_OVERRIDE, tag -> {
            Nbt.putUUID(tag, "Target", player.id());
            tag.putString("ProfileMode", next.name());
        });
    }

    private void sendGlobal(Identifier permission, PortalPermissionPolicy policy) {
        PortalNetworking.sendRequest(PortalAction.SET_PRIVACY, tag -> {
            tag.putString("Permission", permission.toString());
            tag.putString("Policy", policy.name());
        });
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount,
                                 double verticalAmount) {
        int rowsTop = listTop + 20;
        if (mouseY >= rowsTop && mouseY < listBottom) {
            if (mouseX < panelX + listWidth) listScroll -= (int) Math.round(verticalAmount) * ROW_HEIGHT;
            else globalScroll -= (int) Math.round(verticalAmount) * GLOBAL_ROW_HEIGHT;
            expandedPermission = null;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void drawDetailIcon(GuiGraphicsExtractor graphics, int x, int y, boolean hovered) {
        graphics.fill(x, y, x + 16, y + 16, hovered ? 0xFF454951 : 0xFF353840);
        int color = PortalTheme.TEXT_MUTED;
        graphics.fill(x + 4, y + 4, x + 12, y + 6, color);
        graphics.fill(x + 6, y + 7, x + 12, y + 9, color);
        graphics.fill(x + 4, y + 10, x + 12, y + 12, color);
    }

    static void drawPolicySelector(GuiGraphicsExtractor graphics, int x, int y, int width,
                                   PortalPermissionPolicy policy, boolean inherited,
                                   boolean expanded) {
        graphics.fill(x, y, x + width, y + 16, expanded ? 0xFF3A3E47 : 0xFF30333A);
        graphics.outline(x, y, width, 16, PortalTheme.BORDER);
        Component label = policyLabel(policy, inherited);
        graphics.text(net.minecraft.client.Minecraft.getInstance().font, label,
            x + 5, y + 4, policyColor(policy), false);
        graphics.fill(x + width - 16, y, x + width - 15, y + 16, PortalTheme.BORDER);
        int arrow = PortalTheme.TEXT_MUTED;
        graphics.fill(x + width - 11, y + 6, x + width - 5, y + 7, arrow);
        graphics.fill(x + width - 10, y + 7, x + width - 6, y + 8, arrow);
        graphics.fill(x + width - 9, y + 8, x + width - 7, y + 9, arrow);
    }

    static void drawPolicyMenu(GuiGraphicsExtractor graphics, int x, int y, int width,
                               boolean supportsAsk, boolean includeFollow) {
        List<PortalPermissionPolicy> options = policyOptions(supportsAsk, includeFollow);
        graphics.fill(x - 1, y - 1, x + width + 1, y + options.size() * 18 + 1, PortalTheme.BORDER);
        for (int index = 0; index < options.size(); index++) {
            PortalPermissionPolicy option = options.get(index);
            int rowY = y + index * 18;
            graphics.fill(x, rowY, x + width, rowY + 18, 0xFF25272D);
            graphics.text(net.minecraft.client.Minecraft.getInstance().font,
                policyLabel(option, false), x + 5, rowY + 5, policyColor(option), false);
        }
    }

    static List<PortalPermissionPolicy> policyOptions(boolean supportsAsk, boolean includeFollow) {
        java.util.ArrayList<PortalPermissionPolicy> values = new java.util.ArrayList<>();
        if (includeFollow) values.add(PortalPermissionPolicy.FOLLOW_GLOBAL);
        values.add(PortalPermissionPolicy.ALLOW);
        if (supportsAsk) values.add(PortalPermissionPolicy.ASK);
        values.add(PortalPermissionPolicy.DENY);
        return values;
    }

    static PortalPermissionPolicy nextGlobal(PortalPermissionPolicy current, boolean supportsAsk) {
        return switch (current) {
            case ALLOW -> supportsAsk ? PortalPermissionPolicy.ASK : PortalPermissionPolicy.DENY;
            case ASK -> PortalPermissionPolicy.DENY;
            default -> PortalPermissionPolicy.ALLOW;
        };
    }

    static PortalPermissionPolicy previousGlobal(PortalPermissionPolicy current, boolean supportsAsk) {
        return switch (current) {
            case ALLOW -> PortalPermissionPolicy.DENY;
            case ASK -> PortalPermissionPolicy.ALLOW;
            default -> supportsAsk ? PortalPermissionPolicy.ASK : PortalPermissionPolicy.ALLOW;
        };
    }

    static Component policyLabel(PortalPermissionPolicy policy, boolean inherited) {
        String suffix = inherited && policy == PortalPermissionPolicy.FOLLOW_GLOBAL
            ? "follow_global" : policy.name().toLowerCase();
        return Component.translatable("screen.riftgun.privacy.policy." + suffix);
    }

    private static Component profileLabel(PlayerPermissionProfileMode mode) {
        return Component.translatable("screen.riftgun.privacy.profile." + mode.name().toLowerCase());
    }

    static int policyColor(PortalPermissionPolicy policy) {
        return switch (policy) {
            case ALLOW -> PortalTheme.PORTAL;
            case ASK -> 0xFFE0B85A;
            case DENY -> PortalTheme.DANGER;
            case FOLLOW_GLOBAL -> PortalTheme.TEXT_MUTED;
        };
    }

    private static int profileColor(PlayerPermissionProfileMode mode) {
        return switch (mode) {
            case ALLOW_ALL -> PortalTheme.PORTAL;
            case DENY_ALL -> PortalTheme.DANGER;
            case FOLLOW_GLOBAL, CUSTOM -> PortalTheme.TEXT_MUTED;
        };
    }

    static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static int indexOf(List<PrivacyTerminalState.PermissionRef> permissions,
                               Identifier id) {
        for (int index = 0; index < permissions.size(); index++) {
            if (permissions.get(index).id().equals(id)) return index;
        }
        return -1;
    }
}
