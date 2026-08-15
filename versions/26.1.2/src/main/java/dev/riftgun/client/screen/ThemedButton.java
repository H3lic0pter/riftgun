package dev.riftgun.client.screen;

import dev.riftgun.client.PortalClientState;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

final class ThemedButton extends AbstractButton {
    private final Consumer<ThemedButton> press;
    private final boolean portalAction;
    private float hoverProgress;

    ThemedButton(int x, int y, int width, int height, Component message,
                 boolean portalAction, Consumer<ThemedButton> press) {
        super(x, y, width, height, message);
        this.press = press;
        this.portalAction = portalAction;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        press.accept(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float target = isHoveredOrFocused() && active ? 1.0F : 0.0F;
        float speed = PortalClientState.data().settings().animationsEnabled() ? 0.22F : 1.0F;
        hoverProgress = Mth.lerp(speed, hoverProgress, target);
        int base = portalAction ? 0xFF315D38 : PortalTheme.PANEL_RAISED;
        int hover = portalAction ? PortalTheme.PORTAL : PortalTheme.PANEL_HOVER;
        int color = active ? lerpColor(base, hover, hoverProgress) : 0xFF202126;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, color);
        graphics.outline(getX(), getY(), width, height, PortalTheme.BORDER);
        int textColor = active ? PortalTheme.TEXT : 0xFF777777;
        graphics.centeredText(Minecraft.getInstance().font, getMessage(),
            getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (PortalClientState.data().settings().soundsEnabled()) super.playDownSound(soundManager);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private static int lerpColor(int from, int to, float amount) {
        int a = Mth.lerpInt(amount, from >>> 24, to >>> 24);
        int r = Mth.lerpInt(amount, from >> 16 & 255, to >> 16 & 255);
        int g = Mth.lerpInt(amount, from >> 8 & 255, to >> 8 & 255);
        int b = Mth.lerpInt(amount, from & 255, to & 255);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
