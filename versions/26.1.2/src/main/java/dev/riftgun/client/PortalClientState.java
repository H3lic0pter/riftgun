package dev.riftgun.client;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.data.PortalPlayerData;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.state.PortalGunViewState;
import dev.riftgun.state.PortalGunViewStateCodec;
import java.util.function.UnaryOperator;

public final class PortalClientState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static CompoundTag gunReference = new CompoundTag();
    private static PortalGunViewState gun = PortalGunViewState.empty();
    private static PortalModuleRules moduleRules = PortalModuleRules.defaults();
    private static CompoundTag randomRift = new CompoundTag();
    private static long randomRiftSnapshotNanos;

    public static PortalPlayerData data() {
        return data;
    }

    public static void handle(CompoundTag envelope) {
        String kind = Nbt.getString(envelope, "Kind");
        if (kind.equals("Snapshot")) {
            DimensionLabelState.replace(envelope);
            data = PortalPlayerData.load(Nbt.getCompound(envelope, "Data"));
            gunReference = envelope.contains("GunReference")
                ? Nbt.getCompound(envelope, "GunReference").copy() : new CompoundTag();
            gun = envelope.contains("Gun")
                ? PortalGunViewStateCodec.decode(Nbt.getCompound(envelope, "Gun"))
                : PortalGunViewState.empty();
            moduleRules = envelope.contains("ModuleRules")
                ? PortalModuleRules.load(Nbt.getCompound(envelope, "ModuleRules")) : PortalModuleRules.defaults();
            randomRift = envelope.contains("RandomRift")
                ? Nbt.getCompound(envelope, "RandomRift").copy() : new CompoundTag();
            randomRiftSnapshotNanos = System.nanoTime();
            if (Nbt.getBoolean(envelope, "OpenScreen")) {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PortalConfigScreen());
            } else if (Nbt.getBoolean(envelope, "OpenRadial")) {
                ModeRadialInput.openFromServer(Nbt.getInt(envelope, "RadialRequestId"));
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer(Set.of());
            } else if (Minecraft.getInstance().screen
                instanceof dev.riftgun.client.screen.DimensionalNavigationScreen screen) {
                screen.onServerSnapshot();
            }
        } else if (kind.equals("GunSnapshot")) {
            gunReference = Nbt.getCompound(envelope, "GunReference").copy();
            gun = PortalGunViewStateCodec.decode(Nbt.getCompound(envelope, "Gun"));
            if (Nbt.getBoolean(envelope, "Rollback")) refreshGunScreen();
        } else if (kind.equals("RadialUnavailable")) {
            ModeRadialInput.rejectFromServer(Nbt.getInt(envelope, "RadialRequestId"));
        } else if (kind.equals("PortalOpened")) {
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onPortalOpened();
            }
        } else if (kind.equals("GunReferenceInvalid")) {
            gunReference = new CompoundTag();
            gun = PortalGunViewState.empty();
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen
                || Minecraft.getInstance().screen
                    instanceof dev.riftgun.client.screen.DimensionalNavigationScreen
                || Minecraft.getInstance().screen
                    instanceof dev.riftgun.client.screen.DimensionSelectionScreen) {
                Minecraft.getInstance().setScreen(null);
            }
        } else if (kind.equals("PlayerList")) {
            DimensionLabelState.merge(envelope);
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
        boolean gunScreen = Minecraft.getInstance().screen
            instanceof dev.riftgun.client.screen.PortalConfigScreen
            || Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.ModeRadialScreen
            || Minecraft.getInstance().screen
                instanceof dev.riftgun.client.screen.DimensionalNavigationScreen
            || Minecraft.getInstance().screen
                instanceof dev.riftgun.client.screen.DimensionSelectionScreen;
        if (!gunScreen || gunReference.isEmpty()) return;
        request.put("GunReference", gunReference.copy());
    }

    public static PortalGunViewState gun() {
        return gun;
    }

    public static void updateGun(UnaryOperator<PortalGunViewState> update) {
        gun = update.apply(gun);
    }

    public static PortalModuleRules moduleRules() {
        return moduleRules;
    }

    public static CompoundTag randomRift() {
        return randomRift;
    }

    public static int randomRiftCooldownTicks() {
        int receivedTicks = Nbt.getInt(randomRift, "CooldownTicks");
        long elapsedTicks = Math.max(0L, System.nanoTime() - randomRiftSnapshotNanos) / 50_000_000L;
        return (int) Math.max(0L, receivedTicks - elapsedTicks);
    }

    private static void refreshGunScreen() {
        if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
            screen.refreshFromServer(Set.of());
        } else if (Minecraft.getInstance().screen
            instanceof dev.riftgun.client.screen.DimensionalNavigationScreen screen) {
            screen.onGunSnapshot();
        } else if (Minecraft.getInstance().screen
            instanceof dev.riftgun.client.screen.ModeRadialScreen screen) {
            screen.refreshFromServer();
        }
    }

    private PortalClientState() {}
}
