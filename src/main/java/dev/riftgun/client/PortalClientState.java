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
    private static final long SAFETY_TTL_MILLIS = 5_000L;
    private static final Map<UUID, SafetyEntry> SAFETY = new HashMap<>();
    private static final Set<UUID> CHECKING = new HashSet<>();
    private static final Set<UUID> SAFETY_UNLOADED = new HashSet<>();
    private static CompoundTag gunReference = new CompoundTag();
    private static CompoundTag gun = new CompoundTag();

    public static PortalPlayerData data() {
        return data;
    }

    public static Integer safety(UUID id) {
        SafetyEntry entry = SAFETY.get(id);
        if (entry == null) return null;
        if (System.currentTimeMillis() >= entry.expiresAtMillis) {
            SAFETY.remove(id);
            return null;
        }
        return entry.flags;
    }

    public static boolean checkingSafety(UUID id) {
        return CHECKING.contains(id);
    }

    public static void beginSafetyCheck(UUID id) {
        if (!data.settings().safetyCheckEnabled()) return;
        SAFETY.remove(id);
        SAFETY_UNLOADED.remove(id);
        CHECKING.add(id);
    }

    public static boolean safetyUnloaded(UUID id) {
        return SAFETY_UNLOADED.contains(id);
    }

    public static void clearSafety() {
        SAFETY.clear();
        CHECKING.clear();
        SAFETY_UNLOADED.clear();
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
                SAFETY_UNLOADED.remove(id);
            });
            data = next;
            gunReference = envelope.contains("GunReference")
                ? envelope.getCompound("GunReference").copy() : new CompoundTag();
            gun = envelope.contains("Gun") ? envelope.getCompound("Gun").copy() : new CompoundTag();
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
            boolean loaded = envelope.getBoolean("Loaded");
            if (loaded) {
                SAFETY_UNLOADED.remove(id);
                SAFETY.put(id, new SafetyEntry(envelope.getInt("Flags"),
                    System.currentTimeMillis() + SAFETY_TTL_MILLIS));
            } else {
                SAFETY.remove(id);
                SAFETY_UNLOADED.add(id);
            }
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onSafetyResult(id, loaded ? envelope.getInt("Flags") : 0,
                    envelope.getBoolean("Confirmation") && loaded);
            }
        } else if (kind.equals("PortalPending") && envelope.hasUUID("Destination")) {
            if (Minecraft.getInstance().screen instanceof dev.riftgun.client.screen.PortalConfigScreen screen) {
                screen.onPortalPending(envelope.getUUID("Destination"), envelope.getString("State"));
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
        SAFETY_UNLOADED.removeIf(id -> next.destination(id).isEmpty());
        return result;
    }

    private record SafetyEntry(int flags, long expiresAtMillis) {}

    private PortalClientState() {}
}
