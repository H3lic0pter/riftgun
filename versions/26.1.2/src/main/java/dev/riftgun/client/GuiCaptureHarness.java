package dev.riftgun.client;

import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.state.PortalGunViewStateCodec;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** Opt-in visual QA harness. Enable only with Gradle property {@code -PguiCapture=true}. */
final class GuiCaptureHarness {
    private static int ticks;
    private static boolean completed;

    static void tick(Minecraft minecraft) {
        if (!Boolean.getBoolean("riftgun.guiCapture") || completed) return;
        ticks++;
        if (ticks == 20) {
            int scale = Integer.getInteger("riftgun.guiCaptureScale", 0);
            if (scale > 0) {
                minecraft.options.guiScale().set(scale);
                minecraft.resizeGui();
            }
        }
        if (ticks == 40) openRepresentativeScreen();
        if (ticks == 70) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui"), minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 74 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            for (int step = 0; step < 8; step++) {
                screen.mouseScrolled(screen.width - 40.0, screen.height / 2.0, 0.0, -1.0);
            }
        }
        if (ticks == 76) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-details-bottom"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 82 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openCoordinateEditorForQa();
        }
        if (ticks == 88 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openGroupDropdownForQa();
        }
        if (ticks == 94) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-group-dropdown"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 96 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.closeGroupDropdownForQa();
        }
        if (ticks == 100) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-coordinate"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 126) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-safety-history"), minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 134 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openPlacementSettingsForQa();
        }
        if (ticks == 150) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-placement-settings"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 156 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openVisualSettingsForQa();
        }
        if (ticks == 158 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.selectSwirlVisualForQa();
        }
        if (ticks == 164) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-visual-settings"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 166 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openSwirlAnimationSettingsForQa();
        }
        if (ticks == 174) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-swirl-animation-settings"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 178 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openVisualSettingsForQa();
            screen.openVisualDropdownForQa();
        }
        if (ticks == 186) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-visual-dropdown"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 192 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openGunSettingsForQa();
        }
        if (ticks == 198) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-gun-settings"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 202 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openPortalDurationSettingsForQa();
        }
        if (ticks == 208) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-portal-duration"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 212 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openSmartRangeSettingsForQa();
        }
        if (ticks == 218) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-surface-range"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 222 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openRemoteSettingsForQa();
        }
        if (ticks == 228) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-remote-settings"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 229 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.toggleRemoteRadialSliderForQa();
        }
        if (ticks == 231) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-remote-settings-toggled"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 232 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openPairingSettingsForQa();
        }
        if (ticks == 238) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-pairing-settings"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 240 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.toggleRemoteRadialSliderForQa();
        }
        if (ticks == 242 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openApertureSettingsForQa();
        }
        if (ticks == 248) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-aperture"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 252) {
            minecraft.setScreen(new dev.riftgun.client.screen.ModeRadialScreen());
        }
        if (ticks == 258) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-mode-radial"),
                minecraft.getMainRenderTarget(), 1, message -> {});
        }
        if (ticks == 270) {
            completed = true;
            minecraft.stop();
        }
    }

    private static String screenshotName(String prefix) {
        int scale = Integer.getInteger("riftgun.guiCaptureScale", 0);
        return prefix + (scale > 0 ? "-scale-" + scale : "-qa") + ".png";
    }

    private static void openRepresentativeScreen() {
        PortalPlayerData sample = new PortalPlayerData();
        UUID labs = UUID.randomUUID();
        sample.groups().add(new DestinationGroup(labs, "Citadel Labs", 0));
        sample.expandedGroups().add(labs);
        long now = 12_000L;
        Destination home = new Destination(UUID.randomUUID(), "Garage Workshop",
            PortalPlayerData.DEFAULT_GROUP_ID, Level.OVERWORLD, 18.5, 64.0, -32.5, 90.0F,
            now - 900, now - 20, true);
        Destination mine = new Destination(UUID.randomUUID(), "Deep Slate Mine",
            PortalPlayerData.DEFAULT_GROUP_ID, Level.OVERWORLD, -245.25, -42.0, 811.75, 180.0F,
            now - 500, now - 240, false);
        Destination lab = new Destination(UUID.randomUUID(), "Specimen Vault",
            labs, Level.OVERWORLD, 1024.0, 72.5, -488.0, -90.0F,
            now - 300, 0L, false);
        sample.destinations().add(home);
        sample.destinations().add(mine);
        sample.destinations().add(lab);
        sample.selectedDestinationId(home.id());
        sample.lastViewedDestinationId(home.id());
        sample.recordSafetyResult(home.id(), false);
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Snapshot");
        envelope.putBoolean("OpenScreen", true);
        envelope.put("Data", sample.save());
        envelope.put("Gun", PortalGunViewStateCodec.encode(GuiCapturePortalGunState.create()));
        envelope.put("ModuleRules", PortalModuleRules.defaults().save());
        PortalClientState.handle(envelope);
    }

    private GuiCaptureHarness() {}
}
