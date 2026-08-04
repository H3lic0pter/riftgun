package dev.riftgun.client;

import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.service.SafetyReport;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** Opt-in visual QA harness. Enable only with Gradle property {@code -PguiCapture=true}. */
final class GuiCaptureHarness {
    private static int ticks;
    private static boolean completed;
    private static UUID sampleTarget;

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
        if (ticks == 78 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.onSafetyResult(sampleTarget,
                SafetyReport.COLLISION | SafetyReport.NO_SUPPORT | SafetyReport.HAZARD, true);
        }
        if (ticks == 96) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-modal"), minecraft.getMainRenderTarget(),
                message -> {});
        }
        if (ticks == 120) {
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
        sampleTarget = home.id();

        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Snapshot");
        envelope.putBoolean("OpenScreen", true);
        envelope.put("Data", sample.save());
        PortalClientState.handle(envelope);
    }

    private GuiCaptureHarness() {}
}
