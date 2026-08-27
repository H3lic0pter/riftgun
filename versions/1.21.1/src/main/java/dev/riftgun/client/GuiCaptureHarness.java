package dev.riftgun.client;

import dev.riftgun.data.Destination;
import dev.riftgun.data.DestinationGroup;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalModuleRules;
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
        if (Boolean.getBoolean("riftgun.radialCapture")) {
            tickRadialCapture(minecraft);
            return;
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
        if (ticks == 156 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openVisualSettingsForQa();
        }
        if (ticks == 158 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.selectSwirlVisualForQa();
        }
        if (ticks == 164) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-visual-settings"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 166 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openSwirlAnimationSettingsForQa();
        }
        if (ticks == 174) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-swirl-animation-settings"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 178 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openVisualSettingsForQa();
            screen.openVisualDropdownForQa();
        }
        if (ticks == 186) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-visual-dropdown"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 192 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openGunSettingsForQa();
        }
        if (ticks == 198) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-gun-settings"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 202 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openPortalDurationSettingsForQa();
        }
        if (ticks == 208) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-portal-duration"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 212 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openSurfaceRangeSettingsForQa();
        }
        if (ticks == 218) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-surface-range"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 222 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openEntityTransitSettingsForQa();
        }
        if (ticks == 228) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-entity-transit"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 232 && minecraft.screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.openApertureSettingsForQa();
        }
        if (ticks == 238) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-gui-aperture"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 242) {
            minecraft.setScreen(new dev.riftgun.client.screen.ModeRadialScreen());
        }
        if (ticks == 248) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-mode-radial"),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 260) {
            completed = true;
            minecraft.stop();
        }
    }

    private static void tickRadialCapture(Minecraft minecraft) {
        if (ticks == 40) {
            openRepresentativeState(false);
            minecraft.setScreen(new dev.riftgun.client.screen.ModeRadialScreen());
        }
        if (ticks == 45 || ticks == 70 || ticks == 120) {
            Screenshot.grab(minecraft.gameDirectory, screenshotName("riftgun-mode-radial-tick-" + ticks),
                minecraft.getMainRenderTarget(), message -> {});
        }
        if (ticks == 130) {
            completed = true;
            minecraft.stop();
        }
    }

    private static String screenshotName(String prefix) {
        int scale = Integer.getInteger("riftgun.guiCaptureScale", 0);
        return prefix + (scale > 0 ? "-scale-" + scale : "-qa") + ".png";
    }

    private static void openRepresentativeScreen() {
        openRepresentativeState(true);
    }

    private static void openRepresentativeState(boolean openScreen) {
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
        envelope.putBoolean("OpenScreen", openScreen);
        envelope.put("Data", sample.save());
        CompoundTag gun = new CompoundTag();
        gun.putInt("Amount", 15000);
        gun.putInt("Capacity", 24000);
        gun.putBoolean("CoordinateOverride", true);
        gun.putInt("MaximumSurfaceRange", 80);
        gun.putInt("SurfaceRange", 64);
        gun.putInt("SmartDistance", 12);
        gun.putInt("PortalDurationSeconds", 8);
        gun.putInt("MaximumPortalDurationSeconds", 15);
        gun.putBoolean("EternalDurationInstalled", false);
        gun.putBoolean("ExpandedApertureEnabled", true);
        gun.putBoolean("PassiveTransitEnabled", true);
        gun.putBoolean("HostileTransitEnabled", false);
        gun.putBoolean("BossTransitEnabled", true);
        gun.putBoolean("ProjectileTransitEnabled", true);
        gun.putBoolean("PlayerTargetEnabled", true);
        gun.putInt("PlayerExcludeMode", 1);
        gun.putBoolean("PlayerTargetInstalled", true);
        gun.putBoolean("FallGuardInstalled", true);
        gun.putBoolean("FallGuardEnabled", true);
        gun.putBoolean("FallGuardEntitiesEnabled", false);
        gun.putBoolean("EntityRelocationEnabled", true);
        gun.putBoolean("PortalPairingInstalled", true);
        gun.putString("FunctionMode", "PORTAL_PAIRING");
        gun.putString("CoordinateSmartFallback", "FRONT");
        gun.putString("PairingSmartFallback", "REMOTE");
        CompoundTag modules = new CompoundTag();
        modules.putInt("COORDINATE_OVERRIDE", 1);
        modules.putInt("RESERVOIR_EXPANSION", 2);
        modules.putInt("PASSIVE_TRANSIT", 1);
        modules.putInt("HOSTILE_TRANSIT", 1);
        modules.putInt("BOSS_TRANSIT", 1);
        modules.putInt("PROJECTILE_TRANSIT", 1);
        modules.putInt("SURFACE_RANGE", 3);
        modules.putInt("APERTURE_EXPANSION", 1);
        modules.putInt("PLAYER_TARGET", 1);
        modules.putInt("PORTAL_PAIRING", 1);
        gun.put("Modules", modules);
        envelope.put("Gun", gun);
        envelope.put("ModuleRules", PortalModuleRules.defaults().save());
        PortalClientState.handle(envelope);
    }

    private GuiCaptureHarness() {}
}
