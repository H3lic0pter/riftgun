package dev.riftgun.client;

import dev.riftgun.data.PortalPlayerData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

/** Client-side state backing the Privacy Terminal screen. */
public final class PrivacyTerminalState {
    private static PortalPlayerData data = new PortalPlayerData();
    private static final List<PlayerRef> players = new ArrayList<>();
    private static final List<PermissionRef> permissions = new ArrayList<>();

    private PrivacyTerminalState() {}

    public static PortalPlayerData data() {
        return data;
    }

    public static List<PlayerRef> players() {
        return players;
    }

    public static List<PermissionRef> permissions() {
        return permissions;
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
        if (envelope.contains("Permissions")) {
            permissions.clear();
            ListTag tags = envelope.getList("Permissions", Tag.TAG_COMPOUND);
            for (Tag raw : tags) {
                CompoundTag tag = (CompoundTag) raw;
                Identifier id = Identifier.tryParse(tag.getString("Id"));
                if (id != null) permissions.add(new PermissionRef(
                    id, tag.getBoolean("SupportsAsk"), tag.getString("TranslationKey")));
            }
        }
    }

    public record PlayerRef(UUID id, String name) {}
    public record PermissionRef(Identifier id, boolean supportsAsk, String translationKey) {}
}
