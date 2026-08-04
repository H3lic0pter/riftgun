package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.Destination;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public final class PortalClientState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static final Map<UUID, Integer> SAFETY = new HashMap<>();
    private static final Set<UUID> CHECKING = new HashSet<>();

    public static PortalPlayerData data() {
        return data;
    }

    public static Integer safety(UUID id) {
        return SAFETY.get(id);
    }

    public static boolean checkingSafety(UUID id) {
        return CHECKING.contains(id);
    }

    public static void beginSafetyCheck(UUID id) {
        if (!data.settings().safetyCheckEnabled()) return;
        SAFETY.remove(id);
        CHECKING.add(id);
    }

    public static void clearSafety() {
        SAFETY.clear();
        CHECKING.clear();
    }

    public static void handle(CompoundTag envelope) {
        String kind = envelope.getString("Kind");
        if (kind.equals("Snapshot")) {
            PortalPlayerData previous = data;
            PortalPlayerData next = PortalPlayerData.load(envelope.getCompound("Data"));
            Set<UUID> invalidated = changedPositions(previous, next);
            invalidated.forEach(id -> {
                SAFETY.remove(id);
                CHECKING.remove(id);
            });
            data = next;
            if (!data.settings().safetyCheckEnabled()) clearSafety();
            if (envelope.getBoolean("OpenScreen")) {
                Minecraft.getInstance().setScreen(new dev.riftgun.client.screen.PortalConfigScreen());
            } else if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.refreshFromServer(invalidated);
            }
        } else if (kind.equals("Safety") && envelope.hasUUID("Destination")) {
            UUID id = envelope.getUUID("Destination");
            CHECKING.remove(id);
            if (!data.settings().safetyCheckEnabled()) return;
            SAFETY.put(id, envelope.getInt("Flags"));
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onSafetyResult(id, envelope.getInt("Flags"), envelope.getBoolean("Confirmation"));
            }
        } else if (kind.equals("PortalOpened")) {
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onPortalOpened();
            }
        }
    }

    private static Set<UUID> changedPositions(PortalPlayerData previous, PortalPlayerData next) {
        Set<UUID> result = new HashSet<>();
        for (Destination destination : next.destinations()) {
            Destination old = previous.destination(destination.id()).orElse(null);
            if (old != null && (!old.dimension().equals(destination.dimension())
                || Double.compare(old.x(), destination.x()) != 0
                || Double.compare(old.y(), destination.y()) != 0
                || Double.compare(old.z(), destination.z()) != 0)) {
                result.add(destination.id());
            }
        }
        SAFETY.keySet().removeIf(id -> next.destination(id).isEmpty());
        CHECKING.removeIf(id -> next.destination(id).isEmpty());
        return result;
    }

    private PortalClientState() {}
}
