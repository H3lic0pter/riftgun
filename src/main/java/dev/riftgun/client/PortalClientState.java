package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class PortalClientState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static CompoundTag gunReference = new CompoundTag();
    private static CompoundTag gun = new CompoundTag();

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

    private PortalClientState() {}
}
