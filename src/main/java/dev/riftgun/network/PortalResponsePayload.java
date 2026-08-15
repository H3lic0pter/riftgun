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

public record PortalResponsePayload(CompoundTag data) implements CustomPacketPayload {
    public static final Type<PortalResponsePayload> TYPE = new Type<>(
//? if >=1.21.11 {
        /*Identifier.fromNamespaceAndPath(RiftConstants.MOD_ID, "portal_response")
*///?} else {
        ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, "portal_response")
//?}
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PortalResponsePayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, PortalResponsePayload::data, PortalResponsePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

