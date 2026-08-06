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
            if (envelope.getBoolean("OpenScreen")) {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PortalConfigScreen());
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer(Set.of());
            }
        } else if (kind.equals("PortalOpened")) {
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onPortalOpened();
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

    private PortalClientState() {}
}
