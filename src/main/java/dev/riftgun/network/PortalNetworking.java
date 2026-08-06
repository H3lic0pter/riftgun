package dev.riftgun.network;

import dev.riftgun.client.PortalClientPayloadHandler;
import java.util.function.Consumer;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.fuel.PortalGunSnapshot;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.module.PortalModuleRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PortalNetworking {
    private static Consumer<CompoundTag> clientContextWriter = ignored -> {};

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
        clientContextWriter.accept(tag);
        PacketDistributor.sendToServer(new PortalRequestPayload(tag));
    }

    public static void setClientContextWriter(Consumer<CompoundTag> writer) {
        clientContextWriter = writer;
    }

    public static void sendSnapshot(ServerPlayer player, boolean openScreen) {
        sendSnapshot(player, openScreen, PortalGunLocator.first(player).orElse(null));
    }

    public static void sendSnapshot(ServerPlayer player, boolean openScreen,
                                    PortalGunLocator.LocatedGun locatedGun) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Snapshot");
        envelope.putBoolean("OpenScreen", openScreen);
        PortalPlayerData data = PortalDataStore.load(player);
        envelope.put("Data", data.save());
        envelope.put("ModuleRules", PortalModuleRules.current().save());
        if (locatedGun != null) {
            envelope.put("GunReference", locatedGun.saveReference());
            envelope.put("Gun", PortalGunSnapshot.create(locatedGun.stack(), data.settings().smartDistance()));
        }
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendSelectionAccepted(ServerPlayer player, java.util.UUID destinationId) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Selection");
        envelope.putUUID("Destination", destinationId);
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
