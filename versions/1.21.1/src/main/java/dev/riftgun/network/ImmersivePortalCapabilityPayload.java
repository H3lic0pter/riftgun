package dev.riftgun.network;

import dev.riftgun.RiftGun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImmersivePortalCapabilityPayload(boolean supported) implements CustomPacketPayload {
    public static final Type<ImmersivePortalCapabilityPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "immersive_portal_capability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImmersivePortalCapabilityPayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, ImmersivePortalCapabilityPayload::supported,
            ImmersivePortalCapabilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
