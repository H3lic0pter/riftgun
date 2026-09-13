package dev.riftgun.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.client.PortalClientState;
import dev.riftgun.appearance.client.PortalGunAppearanceSession;
import dev.riftgun.appearance.client.PortalGunSkinCatalog;
import dev.riftgun.appearance.client.PortalGunSkinDefinition;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** A per-item preview editor. Only Apply sends a mutation to the server. */
public final class PortalGunAppearanceScreen extends Screen {
    private static final int ROW_HEIGHT = 26;
    private final Screen parent;
    private final PortalGunAppearanceSession session;
    private List<PortalGunSkinDefinition> skins = List.of();
    private Map<String, PortalGunSkinDefinition> catalog = Map.of();
    private ThemedButton apply;
    private int panelX, panelY, panelWidth, panelHeight, listWidth, rows, offset;
    private float yaw, pitch;
    private boolean dragging;

    public PortalGunAppearanceScreen(Screen parent) {
        super(Component.translatable("screen.riftgun.appearance"));
        this.parent = parent;
        CompoundTag context = new CompoundTag();
        PortalClientState.writeGunReference(context);
        session = new PortalGunAppearanceSession(context);
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 12);
        panelHeight = Math.min(320, height - 12);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listWidth = Math.max(106, panelWidth * 43 / 100);
        rows = Math.max(1, (panelHeight - 116) / ROW_HEIGHT);
        catalog = PortalGunSkinCatalog.snapshot();
        skins = catalog.values().stream().sorted(Comparator
            .comparing((PortalGunSkinDefinition skin) -> !skin.id().equals(PortalGunSkin.DEFAULT))
            .thenComparing(PortalGunAppearanceScreen::name).thenComparing(PortalGunSkinDefinition::id)).toList();
        offset = Mth.clamp(offset, 0, Math.max(0, skins.size() - rows));
        for (int row = 0; row < rows && offset + row < skins.size(); row++) {
            var skin = skins.get(offset + row);
            String label = name(skin);
            if (skin.id().equals(session.selection().current())) label += " " + I18n.get("screen.riftgun.appearance.current");
            ThemedButton button = new ThemedButton(panelX + 10, panelY + 38 + row * ROW_HEIGHT,
                listWidth - 12, ROW_HEIGHT - 3, Component.literal(label), false,
                ignored -> { session.select(skin.id()); rebuildWidgets(); }).horizontalMarquee();
            if (skin.id().equals(session.selection().selected())) {
                button.accented(PortalTheme.PANEL_HOVER, PortalTheme.PANEL_HOVER, PortalTheme.TEXT);
            }
            button.active = session.selection().ready();
            addRenderableWidget(button);
        }
        int listFooter = panelY + 40 + rows * ROW_HEIGHT;
        var previous = addRenderableWidget(new ThemedButton(panelX + 10, listFooter, 28, 18,
            Component.literal("<"), false, ignored -> { offset = Math.max(0, offset - rows); rebuildWidgets(); }));
        previous.active = offset > 0;
        var next = addRenderableWidget(new ThemedButton(panelX + listWidth - 30, listFooter, 28, 18,
            Component.literal(">"), false, ignored -> { offset += rows; rebuildWidgets(); }));
        next.active = offset + rows < skins.size();
        addRenderableWidget(new ThemedButton(panelX + 10, panelY + panelHeight - 28, 90, 20,
            Component.translatable("screen.riftgun.appearance.back"), false, ignored -> onClose()));
        apply = addRenderableWidget(new ThemedButton(panelX + panelWidth - 100, panelY + panelHeight - 28, 90, 20,
            Component.translatable("screen.riftgun.appearance.apply"), false, ignored -> { session.apply(); apply.active = false; }));
        apply.active = session.canApply();
        session.open();
    }

    public void handleResponse(CompoundTag response) {
        if (session.receive(response)) rebuildWidgets();
    }

    @Override
    public void tick() {
        session.tick();
        apply.active = session.canApply();
        if (catalog != PortalGunSkinCatalog.snapshot()) rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, PortalTheme.SCRIM);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PortalTheme.PANEL);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PortalTheme.BORDER);
        graphics.drawString(font, title, panelX + 12, panelY + 12, PortalTheme.TEXT, false);
        int right = panelX + listWidth + 10;
        int available = panelWidth - listWidth - 20;
        var selected = PortalGunSkinCatalog.resolve(session.selection().selected());
        graphics.drawString(font, font.plainSubstrByWidth(name(selected), available),
            right, panelY + 38, PortalTheme.TEXT, false);
        renderPreview(graphics, selected, right, panelY + 55, available, panelHeight - 140);
        String detail = session.selection().ready() && !PortalGunSkinCatalog.contains(session.selection().current())
            ? I18n.get("screen.riftgun.appearance.missing", session.selection().current())
            : selected.flat() ? "" : I18n.get("screen.riftgun.appearance.drag");
        int detailY = panelY + panelHeight - 78;
        for (var line : font.split(Component.literal(detail), available)) {
            if (detailY > panelY + panelHeight - 60) break;
            graphics.drawString(font, line, right, detailY, PortalTheme.TEXT_MUTED, false);
            detailY += 9;
        }
        String status = !session.error().isEmpty() ? session.error()
            : session.loading() ? "screen.riftgun.appearance.waiting" : "";
        graphics.drawString(font, font.plainSubstrByWidth(status.isEmpty() ? "" : I18n.get(status), panelWidth - 20),
            panelX + 10, panelY + panelHeight - 44, PortalTheme.TEXT_MUTED, false);
        for (var widget : renderables) widget.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPreview(GuiGraphics graphics, PortalGunSkinDefinition skin,
                               int x, int y, int previewWidth, int previewHeight) {
        graphics.enableScissor(x, y, x + previewWidth, y + previewHeight);
        graphics.pose().pushPose();
        float scale = Math.min(previewWidth, previewHeight) * 0.9F;
        if (skin.flat()) {
            graphics.pose().translate(x + previewWidth / 2.0F, y + previewHeight / 2.0F, 0);
            graphics.pose().scale(scale / 16.0F, scale / 16.0F, 1);
            graphics.renderItem(session.preview(), -8, -8);
        } else {
            graphics.flush();
            graphics.pose().translate(x + previewWidth / 2.0F, y + previewHeight / 2.0F, 150);
            graphics.pose().scale(scale, -scale, scale);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(pitch));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(yaw));
            Lighting.setupForFlatItems();
            minecraft.getItemRenderer().renderStatic(session.preview(), ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, graphics.pose(), graphics.bufferSource(),
                minecraft.level, 0);
            graphics.flush();
            Lighting.setupFor3DItems();
        }
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inPreview(x, y)) {
            dragging = !PortalGunSkinCatalog.resolve(session.selection().selected()).flat();
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && button == 0) {
            yaw += (float) dx;
            pitch = Mth.clamp(pitch + (float) dy, -80, 80);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= panelX && x < panelX + listWidth && y >= panelY + 38 && y < panelY + panelHeight - 58) {
            offset = Mth.clamp(offset - (int) Math.signum(vertical), 0, Math.max(0, skins.size() - rows));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    private boolean inPreview(double x, double y) {
        return x >= panelX + listWidth + 10 && x < panelX + panelWidth - 10
            && y >= panelY + 55 && y < panelY + panelHeight - 85;
    }

    private static String name(PortalGunSkinDefinition skin) {
        return I18n.exists(skin.name()) ? I18n.get(skin.name()) : skin.id();
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
