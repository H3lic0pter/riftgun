package dev.riftgun.network;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.data.PortalPlayerData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

final class PortalRequestFields {
    static UUID id(CompoundTag tag, String key) {
        if (!Nbt.hasUUID(tag, key)) throw error("message.riftgun.invalid_request");
        return Nbt.getUUID(tag, key);
    }

    static UUID optionalGroupId(CompoundTag tag, String key) {
        return Nbt.hasUUID(tag, key) ? Nbt.getUUID(tag, key) : PortalPlayerData.DEFAULT_GROUP_ID;
    }

    static PortalRequestException error(String translationKey) {
        return new PortalRequestException(translationKey);
    }

    private PortalRequestFields() {}
}
