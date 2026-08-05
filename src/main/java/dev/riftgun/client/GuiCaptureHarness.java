package dev.riftgun.client;

import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
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
                minecraft.resizeDisplay();
            }
        }
        if (ticks == 40) openRepresentativeScreen();
        if (ticks == 70) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui"), minecraft.getMainRenderTarget(),
                message -> {});
        }
        if (ticks == 74 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            for (int step = 0; step < 8; step++) {
                screen.mouseScrolled(screen.width - 40.0, screen.height / 2.0, 0.0, -1.0);
            }
        }
        if (ticks == 76) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-details-bottom"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 82 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openCoordinateEditorForQa();
        }
        if (ticks == 88 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openGroupDropdownForQa();
        }
        if (ticks == 94) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-group-dropdown"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 96 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.closeGroupDropdownForQa();
        }
        if (ticks == 100) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-coordinate"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 126) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-safety-history"), minecraft.getMainRenderTarget(),
                message -> {});
        }
        if (ticks == 134 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openPlacementSettingsForQa();
        }
        if (ticks == 150) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-placement-settings"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 168) {
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
        PortalClientState.handle(envelope);
    }

    private GuiCaptureHarness() {}
}
