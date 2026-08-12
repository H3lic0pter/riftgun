package dev.riftgun.network;

import dev.riftgun.client.PortalClientPayloadHandler;
import java.util.function.Consumer;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.fuel.PortalGunSnapshot;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.module.PortalModuleRules;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

    public static void sendShortcutRequest(PortalAction action) {
        sendShortcutRequest(action, ignored -> {});
    }

    public static void sendShortcutRequest(PortalAction action, Consumer<CompoundTag> writer) {
        sendRequest(action, tag -> {
            tag.putBoolean("KeyboardShortcut", true);
            writer.accept(tag);
        });
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

    public static void sendGunReferenceInvalid(ServerPlayer player) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "GunReferenceInvalid");
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    /** Pushes the online player roster plus persisted player-target data for the GUI list. */
    public static void sendPlayerList(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        PortalPlayerData data = PortalDataStore.load(player);
        ListTag entries = new ListTag();
        List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
        online.sort(Comparator.comparing((ServerPlayer candidate) -> data.isPlayerPinned(candidate.getUUID()))
            .reversed().thenComparing(playerComparator(player, data)));
        int order = 0;
        for (ServerPlayer candidate : online) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", candidate.getUUID());
            entry.putString("Name", candidate.getGameProfile().getName());
            entry.putString("Dimension", candidate.level().dimension().location().toString());
            entry.putBoolean("Pinned", data.isPlayerPinned(candidate.getUUID()));
            entry.putLong("LastUse", data.playerLastUseAt(candidate.getUUID()));
            entry.putBoolean("Self", candidate.getUUID().equals(player.getUUID()));
            entry.putInt("Order", order++);
            entries.add(entry);
        }
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "PlayerList");
        envelope.put("Players", entries);
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    private static Comparator<ServerPlayer> playerComparator(ServerPlayer viewer, PortalPlayerData data) {
        Comparator<ServerPlayer> byName = Comparator.comparing(
            candidate -> candidate.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER);
        DestinationSort sort = data.settings().sort();
        return switch (sort) {
            case NAME, CREATED -> byName;
            case RECENT -> Comparator.comparingLong(
                    (ServerPlayer candidate) -> data.playerLastUseAt(candidate.getUUID()))
                .reversed().thenComparing(byName);
            case DISTANCE -> Comparator.comparingDouble(
                    (ServerPlayer candidate) -> playerDistanceSquared(viewer, candidate))
                .thenComparing(byName);
        };
    }

    private static double playerDistanceSquared(ServerPlayer viewer, ServerPlayer candidate) {
        if (!viewer.level().dimension().equals(candidate.level().dimension())) {
            return Double.POSITIVE_INFINITY;
        }
        return viewer.distanceToSqr(candidate);
    }

    /** Opens or refreshes the Privacy Terminal screen with the viewer's privacy data and full roster. */
    public static void sendPrivacyTerminal(ServerPlayer player) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "PrivacyTerminal");
        envelope.put("Data", PortalDataStore.load(player).save());
        envelope.put("Players", privacyRoster(player));
        envelope.put("Permissions", privacyPermissions());
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    private static ListTag privacyPermissions() {
        ListTag entries = new ListTag();
        for (dev.riftgun.data.PortalPermissionDefinition definition
                : dev.riftgun.data.PortalPermissions.definitions()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", definition.id().toString());
            entry.putBoolean("SupportsAsk", definition.supportsAsk());
            entry.putString("TranslationKey", definition.translationKey());
            entries.add(entry);
        }
        return entries;
    }

    /** Refreshes only the online roster of the Privacy Terminal screen. */
    public static void sendPrivacyPlayers(ServerPlayer player) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "PrivacyTerminal");
        envelope.put("Players", privacyRoster(player));
        PacketDistributor.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    private static ListTag privacyRoster(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ListTag entries = new ListTag();
        if (server == null) return entries;
        List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
        online.sort(Comparator.comparing(value -> value.getGameProfile().getName()));
        for (ServerPlayer candidate : online) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", candidate.getUUID());
            entry.putString("Name", candidate.getGameProfile().getName());
            entries.add(entry);
        }
        return entries;
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
