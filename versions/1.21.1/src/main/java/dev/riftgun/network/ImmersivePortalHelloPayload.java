package dev.riftgun.network;

import dev.riftgun.RiftGun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImmersivePortalHelloPayload(boolean selected) implements CustomPacketPayload {
    public static final Type<ImmersivePortalHelloPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, "immersive_portal_hello"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImmersivePortalHelloPayload> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, ImmersivePortalHelloPayload::selected,
            ImmersivePortalHelloPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
