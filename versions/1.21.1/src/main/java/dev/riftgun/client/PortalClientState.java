package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import dev.riftgun.module.PortalModuleRules;

public final class PortalClientState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static CompoundTag gunReference = new CompoundTag();
    private static CompoundTag gun = new CompoundTag();
    private static PortalModuleRules moduleRules = PortalModuleRules.defaults();
    private static CompoundTag randomRift = new CompoundTag();
    private static long randomRiftSnapshotNanos;

    public static PortalPlayerData data() {
        return data;
    }

    public static void handle(CompoundTag envelope) {
        String kind = envelope.getString("Kind");
        if (kind.equals("Snapshot")) {
            data = PortalPlayerData.load(envelope.getCompound("Data"));
            gunReference = envelope.contains("GunReference")
                ? envelope.getCompound("GunReference").copy() : new CompoundTag();
            gun = envelope.contains("Gun") ? envelope.getCompound("Gun").copy() : new CompoundTag();
            moduleRules = envelope.contains("ModuleRules")
                ? PortalModuleRules.load(envelope.getCompound("ModuleRules")) : PortalModuleRules.defaults();
            randomRift = envelope.contains("RandomRift")
                ? envelope.getCompound("RandomRift").copy() : new CompoundTag();
            randomRiftSnapshotNanos = System.nanoTime();
            if (envelope.getBoolean("OpenScreen")) {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PortalConfigScreen());
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer(Set.of());
            }
        } else if (kind.equals("PortalOpened")) {
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onPortalOpened();
            }
        } else if (kind.equals("GunReferenceInvalid")) {
            gunReference = new CompoundTag();
            gun = new CompoundTag();
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen) {
                Minecraft.getInstance().setScreen(null);
            }
        } else if (kind.equals("PlayerList")) {
            PlayerListState.handle(envelope);
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onPlayerListRefresh();
            }
        } else if (kind.equals("PrivacyTerminal")) {
            PrivacyTerminalState.handle(envelope);
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PrivacyTerminalScreen screen) {
                screen.refreshFromServer();
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PrivacyPermissionDetailScreen screen) {
                screen.refreshFromServer();
            } else {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PrivacyTerminalScreen());
            }
        }
    }

    public static void writeGunReference(CompoundTag request) {
        if (!(Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen)
            || gunReference.isEmpty()) return;
        request.put("GunReference", gunReference.copy());
    }

    public static CompoundTag gun() {
        return gun;
    }

    public static PortalModuleRules moduleRules() {
        return moduleRules;
    }

    public static CompoundTag randomRift() {
        return randomRift;
    }

    public static int randomRiftCooldownTicks() {
        int receivedTicks = randomRift.getInt("CooldownTicks");
        long elapsedTicks = Math.max(0L, System.nanoTime() - randomRiftSnapshotNanos) / 50_000_000L;
        return (int) Math.max(0L, receivedTicks - elapsedTicks);
    }

    private PortalClientState() {}
}
