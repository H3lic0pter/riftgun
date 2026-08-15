package dev.riftgun.data;
import dev.riftgun.core.nbt.Nbt;

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
        Nbt.putUUID(tag, "Id", id);
        tag.putString("Name", name);
        tag.putInt("Order", order);
        return tag;
    }

    public static DestinationGroup load(CompoundTag tag) {
        return new DestinationGroup(
            Nbt.hasUUID(tag, "Id") ? Nbt.getUUID(tag, "Id") : UUID.randomUUID(),
            Nbt.getString(tag, "Name"),
            Nbt.getInt(tag, "Order")
        );
    }
}

