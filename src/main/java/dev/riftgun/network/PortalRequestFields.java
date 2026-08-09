package dev.riftgun.network;

import dev.riftgun.data.PortalPlayerData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

final class PortalRequestFields {
    static UUID id(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) throw error("message.riftgun.invalid_request");
        return tag.getUUID(key);
    }

    static UUID optionalGroupId(CompoundTag tag, String key) {
        return tag.hasUUID(key) ? tag.getUUID(key) : PortalPlayerData.DEFAULT_GROUP_ID;
    }

    static PortalRequestException error(String translationKey) {
        return new PortalRequestException(translationKey);
    }

    private PortalRequestFields() {}
}
