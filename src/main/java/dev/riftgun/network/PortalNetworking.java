package dev.riftgun.network;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.api.RiftGunDimensionLabels;
import dev.riftgun.api.RiftResourceId;
import dev.riftgun.core.network.RiftNetwork;
import java.util.function.Consumer;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalClientSync;
import dev.riftgun.service.RandomRiftManager;
import dev.riftgun.fuel.PortalGunSnapshot;
import dev.riftgun.state.PortalGunViewStateCodec;
import dev.riftgun.state.PortalGunViewState;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.DestinationSort;
import dev.riftgun.module.PortalModuleRules;
import dev.riftgun.navigation.DimensionalTraversalTargets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.riftgun.core.config.RiftConfigs;

public final class PortalNetworking {
    private static Consumer<CompoundTag> clientContextWriter = ignored -> {};

    public static void installClientSyncAdapter() {
        PortalClientSync.install(new PortalClientSync.Adapter() {
            @Override
            public void snapshot(ServerPlayer player, boolean openScreen,
                                 PortalGunLocator.LocatedGun gun) {
                if (gun == null) sendSnapshot(player, openScreen);
                else sendSnapshot(player, openScreen, gun);
            }

            @Override
            public void portalOpened(ServerPlayer player) {
                sendPortalOpened(player);
            }
        });
    }

    public static void sendRequest(PortalAction action) {
        sendRequest(action, tag -> {});
    }

    public static void sendRequest(PortalAction action, Consumer<CompoundTag> writer) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Action", action.name());
        writer.accept(tag);
        clientContextWriter.accept(tag);
        RiftNetwork.sendToServer(new PortalRequestPayload(tag));
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
        sendSnapshot(player, openScreen, false, locatedGun);
    }

    public static void sendRadialSnapshot(ServerPlayer player,
                                          PortalGunLocator.LocatedGun locatedGun,
                                          int requestId) {
        sendSnapshot(player, false, true, locatedGun, requestId);
    }

    private static void sendSnapshot(ServerPlayer player, boolean openScreen, boolean openRadial,
                                     PortalGunLocator.LocatedGun locatedGun) {
        sendSnapshot(player, openScreen, openRadial, locatedGun, 0);
    }

    private static void sendSnapshot(ServerPlayer player, boolean openScreen, boolean openRadial,
                                     PortalGunLocator.LocatedGun locatedGun, int radialRequestId) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Snapshot");
        envelope.putBoolean("OpenScreen", openScreen);
        envelope.putBoolean("OpenRadial", openRadial);
        if (openRadial) envelope.putInt("RadialRequestId", radialRequestId);
        PortalPlayerData data = PortalDataStore.load(player);
        envelope.put("Data", data.save());
        putDimensionLabels(envelope, player, data.destinations().stream().map(destination -> {
//? if >=1.21.11 {
            /*return destination.dimension().identifier().toString();
*///?} else {
            return destination.dimension().location().toString();
//?}
        }).distinct().toList());
        envelope.put("ModuleRules", PortalModuleRules.current().save());
        RandomRiftManager.Snapshot randomRift = RandomRiftManager.snapshot(player);
        CompoundTag randomRiftTag = new CompoundTag();
        randomRiftTag.putBoolean("Enabled", randomRift.enabled());
        randomRiftTag.putBoolean("Searching", randomRift.searching());
        randomRiftTag.putInt("CooldownTicks", randomRift.cooldownTicks());
        envelope.put("RandomRift", randomRiftTag);
        if (locatedGun != null) {
            var gun = gunSnapshotState(player, data, locatedGun);
            envelope.put("GunReference", locatedGun.saveReference());
            envelope.put("Gun", PortalGunViewStateCodec.encode(gun));
            if (openScreen && gun.dimensionalTraversalInstalled()
                && gun.dimensionalTraversalEnabled()) {
                putDimensionCatalog(envelope, player);
            }
        }
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    /** A focused acknowledgement for high-frequency per-gun controls such as radial sliders. */
    public static void sendGunSnapshot(ServerPlayer player, PortalPlayerData data,
                                       PortalGunLocator.LocatedGun locatedGun) {
        sendGunSnapshot(player, data, locatedGun, false);
    }

    /** Restores optimistic client state after a rejected per-gun mutation. */
    public static void sendGunRollback(ServerPlayer player, PortalPlayerData data,
                                       PortalGunLocator.LocatedGun locatedGun) {
        sendGunSnapshot(player, data, locatedGun, true);
    }

    private static void sendGunSnapshot(ServerPlayer player, PortalPlayerData data,
                                        PortalGunLocator.LocatedGun locatedGun,
                                        boolean rollback) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "GunSnapshot");
        envelope.putBoolean("Rollback", rollback);
        envelope.put("GunReference", locatedGun.saveReference());
        envelope.put("Gun", PortalGunViewStateCodec.encode(
            gunSnapshotState(player, data, locatedGun)));
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendRadialUnavailable(ServerPlayer player, int requestId) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "RadialUnavailable");
        envelope.putInt("RadialRequestId", requestId);
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendSelectionAccepted(ServerPlayer player, java.util.UUID destinationId) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "Selection");
        Nbt.putUUID(envelope, "Destination", destinationId);
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendPortalOpened(ServerPlayer player) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "PortalOpened");
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    public static void sendGunReferenceInvalid(ServerPlayer player) {
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "GunReferenceInvalid");
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    /** Pushes the online player roster plus persisted player-target data for the GUI list. */
    public static void sendPlayerList(ServerPlayer player) {
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        if (server == null) return;
        PortalPlayerData data = PortalDataStore.load(player);
        ListTag entries = new ListTag();
        List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
        online.sort(Comparator.comparing((ServerPlayer candidate) -> data.isPlayerPinned(candidate.getUUID()))
            .reversed().thenComparing(playerComparator(player, data)));
        int order = 0;
        LinkedHashSet<String> dimensions = new LinkedHashSet<>();
        for (ServerPlayer candidate : online) {
            CompoundTag entry = new CompoundTag();
            Nbt.putUUID(entry, "Id", candidate.getUUID());
//? if >=1.21.11 {
            /*entry.putString("Name", candidate.getGameProfile().name());
*///?} else {
            entry.putString("Name", candidate.getGameProfile().getName());
//?}
//? if >=1.21.11 {
            /*entry.putString("Dimension", candidate.level().dimension().identifier().toString());
*///?} else {
            entry.putString("Dimension", candidate.level().dimension().location().toString());
//?}
            dimensions.add(Nbt.getString(entry, "Dimension"));
            entry.putBoolean("Pinned", data.isPlayerPinned(candidate.getUUID()));
            entry.putLong("LastUse", data.playerLastUseAt(candidate.getUUID()));
            entry.putBoolean("Self", candidate.getUUID().equals(player.getUUID()));
            entry.putInt("Order", order++);
            entries.add(entry);
        }
        CompoundTag envelope = new CompoundTag();
        envelope.putString("Kind", "PlayerList");
        envelope.put("Players", entries);
        putDimensionLabels(envelope, player, dimensions);
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    private static void putDimensionLabels(
        CompoundTag envelope, ServerPlayer viewer, Iterable<String> dimensionIds
    ) {
        ListTag labels = new ListTag();
        for (String dimensionId : dimensionIds) {
            try {
                RiftGunDimensionLabels.label(viewer, RiftResourceId.parse(dimensionId)).ifPresent(label -> {
                    CompoundTag entry = new CompoundTag();
                    entry.putString("Id", dimensionId);
                    entry.putString("Label", label.getString());
                    labels.add(entry);
                });
            } catch (IllegalArgumentException ignored) { }
        }
        envelope.put("DimensionLabels", labels);
    }

    private static void putDimensionCatalog(CompoundTag envelope, ServerPlayer player) {
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        if (server == null) return;
        List<ServerLevel> levels = new ArrayList<>();
        server.getAllLevels().forEach(levels::add);
        levels.sort(Comparator.comparingInt((ServerLevel level) -> dimensionOrder(
                DimensionalTraversalTargets.id(level)))
            .thenComparing(DimensionalTraversalTargets::id));
        ListTag entries = new ListTag();
        ArrayList<String> ids = new ArrayList<>();
        for (ServerLevel level : levels) {
            String id = DimensionalTraversalTargets.id(level);
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", id);
            entry.putDouble("Scale", level.dimensionType().coordinateScale());
            entries.add(entry);
            ids.add(id);
        }
        envelope.put("Dimensions", entries);
        putDimensionLabels(envelope, player, ids);
    }

    private static int dimensionOrder(String id) {
        return switch (id) {
            case "minecraft:overworld" -> 0;
            case "minecraft:the_nether" -> 1;
            case "minecraft:the_end" -> 2;
            default -> 3;
        };
    }

    private static PortalGunViewState gunSnapshotState(
        ServerPlayer player, PortalPlayerData data, PortalGunLocator.LocatedGun locatedGun
    ) {
        var state = PortalGunSnapshot.createState(
            locatedGun.stack(), data.settings().smartDistance());
        String selected = state.navigation().targetDimension();
        if (selected.isBlank() || DimensionalTraversalTargets.resolve(player, selected).isEmpty()) {
            state = state.withNavigation(state.navigation().withTargetDimension(
                DimensionalTraversalTargets.id(player.level())));
        }
        return state;
    }

    private static Comparator<ServerPlayer> playerComparator(ServerPlayer viewer, PortalPlayerData data) {
        Comparator<ServerPlayer> byName = Comparator.comparing(
//? if >=1.21.11 {
            /*candidate -> candidate.getGameProfile().name(), String.CASE_INSENSITIVE_ORDER);
*///?} else {
            candidate -> candidate.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER);
//?}
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
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
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
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(envelope));
    }

    private static ListTag privacyRoster(ServerPlayer player) {
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        ListTag entries = new ListTag();
        if (server == null) return entries;
        List<ServerPlayer> online = new ArrayList<>(server.getPlayerList().getPlayers());
//? if >=1.21.11 {
        /*online.sort(Comparator.comparing(value -> value.getGameProfile().name()));
*///?} else {
        online.sort(Comparator.comparing(value -> value.getGameProfile().getName()));
//?}
        for (ServerPlayer candidate : online) {
            CompoundTag entry = new CompoundTag();
            Nbt.putUUID(entry, "Id", candidate.getUUID());
//? if >=1.21.11 {
            /*entry.putString("Name", candidate.getGameProfile().name());
*///?} else {
            entry.putString("Name", candidate.getGameProfile().getName());
//?}
            entries.add(entry);
        }
        return entries;
    }

    static void handleRequest(ServerPlayer player, PortalRequestPayload payload) {
        PortalRequestHandler.handle(player, payload.data());
    }

    private PortalNetworking() {}
}
