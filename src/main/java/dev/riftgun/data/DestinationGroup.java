package dev.riftgun.data;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public record DestinationGroup(UUID id, String name, int order) {
    public DestinationGroup withName(String nextName) {
        return new DestinationGroup(id, nextName, order);
    }

    public DestinationGroup withOrder(int nextOrder) {
        return new DestinationGroup(id, name, nextOrder);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putInt("Order", order);
        return tag;
    }

    public static DestinationGroup load(CompoundTag tag) {
        return new DestinationGroup(
            tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID(),
            tag.getString("Name"),
            tag.getInt("Order")
        );
    }
}

