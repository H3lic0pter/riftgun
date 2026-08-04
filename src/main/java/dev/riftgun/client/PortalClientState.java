package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class PortalClientState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static final Map<UUID, Integer> SAFETY = new HashMap<>();

    public static PortalPlayerData data() {
        return data;
    }

    public static Integer safety(UUID id) {
        return SAFETY.get(id);
    }

    public static void handle(CompoundTag envelope) {
        String kind = envelope.getString("Kind");
        if (kind.equals("Snapshot")) {
            data = PortalPlayerData.load(envelope.getCompound("Data"));
            if (envelope.getBoolean("OpenScreen")) {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PortalConfigScreen());
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer();
            }
        } else if (kind.equals("Safety") && envelope.hasUUID("Destination")) {
            UUID id = envelope.getUUID("Destination");
            SAFETY.put(id, envelope.getInt("Flags"));
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onSafetyResult(id, envelope.getInt("Flags"), envelope.getBoolean("Confirmation"));
            }
        }
    }

    private PortalClientState() {}
}
