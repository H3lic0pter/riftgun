package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Client-side state backing the Privacy Terminal screen. */
public final class PrivacyTerminalState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static final List<PlayerRef> players = new ArrayList<>();

    private PrivacyTerminalState() {}

    public static PortalPlayerData data() {
        return data;
    }

    public static List<PlayerRef> players() {
        return players;
    }

    public static void handle(CompoundTag envelope) {
        if (envelope.contains("Data")) {
            data = PortalPlayerData.load(envelope.getCompound("Data"));
        }
        if (envelope.contains("Players")) {
            players.clear();
            ListTag tags = envelope.getList("Players", Tag.TAG_COMPOUND);
            for (Tag raw : tags) {
                CompoundTag tag = (CompoundTag) raw;
                players.add(new PlayerRef(tag.getUUID("Id"), tag.getString("Name")));
            }
            players.sort(java.util.Comparator.comparing(PlayerRef::name));
        }
    }

    public record PlayerRef(UUID id, String name) {}
}
