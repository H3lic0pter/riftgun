package dev.riftgun.client;

import dev.riftgun.core.nbt.Nbt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** Client-side cached online player roster for the Player destination category. */
public final class PlayerListState {
    private static final List<PlayerEntry> players = new ArrayList<>();
    private static final Map<UUID, PlayerEntry> byId = new HashMap<>();

    private PlayerListState() {}

    public static void handle(CompoundTag envelope) {
        players.clear();
        byId.clear();
        ListTag tags = Nbt.getList(envelope, "Players");
        for (net.minecraft.nbt.Tag raw : tags) {
            CompoundTag tag = (CompoundTag) raw;
            PlayerEntry entry = new PlayerEntry(
                Nbt.getUUID(tag, "Id"),
                Nbt.getString(tag, "Name"),
                Nbt.getString(tag, "Dimension"),
                Nbt.getBoolean(tag, "Pinned"),
                Nbt.getLong(tag, "LastUse"),
                Nbt.getBoolean(tag, "Self"),
                Nbt.getInt(tag, "Order")
            );
            players.add(entry);
            byId.put(entry.id(), entry);
        }
    }

    public static List<PlayerEntry> players() {
        return players;
    }

    public static PlayerEntry player(UUID id) {
        return byId.get(id);
    }

    public static void markPinned(UUID id, boolean pinned) {
        PlayerEntry entry = byId.get(id);
        if (entry != null) byId.put(id, entry.withPinned(pinned));
        for (int index = 0; index < players.size(); index++) {
            if (players.get(index).id().equals(id)) {
                players.set(index, entry == null ? players.get(index) : entry.withPinned(pinned));
                return;
            }
        }
    }

    public record PlayerEntry(UUID id, String name, String dimension,
                              boolean pinned, long lastUse, boolean self,
                              int serverOrder) {
        public PlayerEntry withPinned(boolean nextPinned) {
            return new PlayerEntry(id, name, dimension, nextPinned, lastUse, self, serverOrder);
        }
    }
}
