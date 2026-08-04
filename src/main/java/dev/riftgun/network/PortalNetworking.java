package dev.riftgun.network;

import dev.riftgun.client.PortalClientPayloadHandler;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PortalNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(PortalRequestPayload.TYPE, PortalRequestPayload.STREAM_CODEC, PortalNetworking::handleRequest);
        registrar.playToClient(PortalResponsePayload.TYPE, PortalResponsePayload.STREAM_CODEC, PortalNetworking::handleResponse);
    }

    public static void sendRequest(PortalAction action) {
        sendRequest(action, tag -> {});
    }

    public static void sendRequest(PortalAction action, Consumer<CompoundTag> writer) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Action", action.name());
        writer.accept(tag);
        PacketDistributor.sendToServer(new PortalRequestPayload(tag));
    }

    public static void sendSnapshot(ServerPlayer player, boolean openScreen) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Snapshot");
        envelope.putBoolean("OpenScreen", openScreen);
        envelope.put("Data", dev.riftgun.data.PortalDataStore.snapshot(player));
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendSafety(ServerPlayer player, java.util.UUID destinationId, int flags, boolean confirmation) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Safety");
        envelope.putUUID("Destination", destinationId);
        envelope.putInt("Flags", flags);
        envelope.putBoolean("Confirmation", confirmation);
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendPortalOpened(ServerPlayer player) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "PortalOpened");
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    private static void handleRequest(PortalRequestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            PortalRequestHandler.handle(player, payload.data());
        }
    }

    private static void handleResponse(PortalResponsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PortalClientPayloadHandler.handle(payload.data()));
    }

    private PortalNetworking() {}
}
