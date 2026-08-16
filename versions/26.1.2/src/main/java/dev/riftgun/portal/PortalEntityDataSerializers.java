package dev.riftgun.portal;

import dev.riftgun.core.RiftConstants;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 26.1.2 entity-data serializers used by portal entities. Modded serializers
 * must be registered to {@link NeoForgeRegistries#ENTITY_DATA_SERIALIZERS} so
 * the client and server agree on serializer ids.
 */
public final class PortalEntityDataSerializers {
    private static final DeferredRegister<EntityDataSerializer<?>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, RiftConstants.MOD_ID);

    public static final DeferredHolder<EntityDataSerializer<?>, EntityDataSerializer<Optional<UUID>>> OPTIONAL_UUID =
        SERIALIZERS.register("optional_uuid", () ->
            EntityDataSerializer.forValueType(ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC)));

    private PortalEntityDataSerializers() {}

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
