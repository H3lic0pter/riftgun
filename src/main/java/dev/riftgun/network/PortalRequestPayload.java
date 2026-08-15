package dev.riftgun.network;

import dev.riftgun.core.RiftConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}

public record PortalRequestPayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PortalRequestPayload> TYPE = new Type<>(
//? if >=1.21.11 {
        /*Identifier.fromNamespaceAndPath(RiftConstants.MOD_ID, "portal_request")
*///?} else {
        ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, "portal_request")
//?}
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalRequestPayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, PortalRequestPayload::data, PortalRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

