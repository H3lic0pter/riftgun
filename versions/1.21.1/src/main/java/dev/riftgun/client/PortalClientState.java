package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.portal.PortalInstanceState;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.state.PortalGunViewState;
import dev.riftgun.state.PortalGunViewStateCodec;
import java.util.function.UnaryOperator;

public final class PortalClientState {
    private static PortalInstanceState instances = PortalInstanceState.EMPTY;

    public static PortalInstanceState instances() { return instances; }

    public static void clearInstances() { instances = PortalInstanceState.EMPTY; }

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
        if (envelope.contains("PortalInstances")) {
            instances = PortalInstanceState.load(dev.riftgun.core.nbt.Nbt.getCompound(envelope, "PortalInstances"));
        }
        String kind = envelope.getString("Kind");
        if (kind.equals("Appearance")) {
            dev.riftgun.client.appearance.SkinRecommendations.receive(envelope);
            if (Minecraft.getInstance().screen
                instanceof dev.riftgun.client.screen.PortalGunAppearanceScreen screen) {
                screen.handleResponse(envelope);
            }
            return;
        }
        if (kind.equals("GunPresentation")) {
            if (!dev.riftgun.client.appearance.SkinRecommendations.receive(envelope)) return;
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer(Set.of());
            }
            return;
        }
        if (kind.equals("Snapshot")) {
            DimensionLabelState.replace(envelope);
            data = PortalPlayerData.load(envelope.getCompound("Data"));
            boolean currentGun = dev.riftgun.client.appearance.SkinRecommendations.receive(envelope);
            if (currentGun || !gunEditorOpen()) {
                gunReference = envelope.contains("GunReference")
                    ? envelope.getCompound("GunReference").copy() : new CompoundTag();
                gun = envelope.contains("Gun")
                    ? PortalGunViewStateCodec.decode(envelope.getCompound("Gun"))
                    : PortalGunViewState.empty();
            }
            moduleRules = envelope.contains("ModuleRules")
                ? PortalModuleRules.load(envelope.getCompound("ModuleRules")) : PortalModuleRules.defaults();
            randomRift = envelope.contains("RandomRift")
                ? envelope.getCompound("RandomRift").copy() : new CompoundTag();
            randomRiftSnapshotNanos = System.nanoTime();
            if (envelope.getBoolean("OpenScreen")) {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PortalConfigScreen());
            } else if (envelope.getBoolean("OpenRadial")) {
                ModeRadialInput.openFromServer(envelope.getInt("RadialRequestId"));
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer(Set.of());
            } else if (Minecraft.getInstance().screen
                instanceof dev.riftgun.client.screen.DimensionalNavigationScreen screen) {
                screen.onServerSnapshot();
            }
        } else if (kind.equals("GunSnapshot")) {
            boolean currentGun = dev.riftgun.client.appearance.SkinRecommendations.receive(envelope);
            if (!currentGun && gunEditorOpen()) return;
            gunReference = envelope.getCompound("GunReference").copy();
            gun = PortalGunViewStateCodec.decode(envelope.getCompound("Gun"));
            if (envelope.getBoolean("Rollback")) refreshGunScreen();
        } else if (kind.equals("RadialUnavailable")) {
            ModeRadialInput.rejectFromServer(envelope.getInt("RadialRequestId"));
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

    /** Keep an open editor pinned while preserving ordinary background state updates outside editors. */
    private static boolean gunEditorOpen() {
        var screen = Minecraft.getInstance().screen;
        return screen instanceof dev.riftgun.client.screen.PortalConfigScreen
            || screen instanceof dev.riftgun.client.screen.PortalGunAppearanceScreen
            || screen instanceof dev.riftgun.client.screen.ModeRadialScreen
            || screen instanceof dev.riftgun.client.screen.DimensionalNavigationScreen
            || screen instanceof dev.riftgun.client.screen.DimensionSelectionScreen;
    }

    public static void writeGunReference(CompoundTag request) {
        // A delayed appearance ACK may target a different gun from the currently open screen.
        if (request.contains("GunReference")) return;
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
        int receivedTicks = randomRift.getInt("CooldownTicks");
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
