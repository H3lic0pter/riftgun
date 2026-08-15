package dev.riftgun.network;

import dev.riftgun.client.PortalClientPayloadHandler;
import dev.riftgun.core.network.NetworkTransport;
import dev.riftgun.core.network.RiftNetwork;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** NeoForge registration and packet-distribution adapter. */
public final class NeoForgeNetworkAdapter implements NetworkTransport {
    public static void register(RegisterPayloadHandlersEvent event) {
        NeoForgeNetworkAdapter adapter = new NeoForgeNetworkAdapter();
        RiftNetwork.install(adapter);
        var registrar = event.registrar("1");
        registrar.playToServer(PortalRequestPayload.TYPE, PortalRequestPayload.STREAM_CODEC,
            (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    PortalNetworking.handleRequest(player, payload);
                }
            });
        registrar.playToClient(PortalResponsePayload.TYPE, PortalResponsePayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> PortalClientPayloadHandler.handle(payload.data())));
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
