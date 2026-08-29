package dev.riftgun.network;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.data.PortalPlacementMode;
import dev.riftgun.external.ExternalDestinationSelection;
import dev.riftgun.service.ExternalDestinationSession;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalOpenCoordinator;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Server-side owner of untrusted, login-session-only map waypoint selections. */
public final class ExternalDestinationActions {
    private static final ExternalDestinationSession SESSION = new ExternalDestinationSession();

    static boolean select(ServerPlayer player, PortalPlayerData data, CompoundTag request) {
        ExternalDestinationRequest.DecodeResult decoded = decode(player, request);
        if (!decoded.accepted()) throw error(decoded.error());
        SESSION.select(player.getUUID(), decoded.selection());
        data.selectedDestinationId(null);
        data.selectedPlayerId(null);
        return true;
    }

    static boolean openSelected(
        ServerPlayer player,
        PortalPlayerData data,
        PortalPlacementMode mode,
        PortalGunLocator.LocatedGun gun,
        boolean fromGui
    ) {
        return openSelected(player, data, mode, gun, fromGui, null);
    }

    static boolean openSelectedSurfaceFace(
        ServerPlayer player, PortalPlayerData data, PortalPlacementMode mode,
        PortalGunLocator.LocatedGun gun, SurfaceFaceRequest request
    ) {
        return openSelected(player, data, mode, gun, false, request);
    }

    private static boolean openSelected(
        ServerPlayer player, PortalPlayerData data, PortalPlacementMode mode,
        PortalGunLocator.LocatedGun gun, boolean fromGui, SurfaceFaceRequest surfaceFaceRequest
    ) {
        ExternalDestinationSelection selection = SESSION.selected(player.getUUID()).orElse(null);
        if (selection == null) return false;
        if (!RiftConfigs.server().mapWaypointIntegration().enabled()
            || !knownDimension(player, selection.dimensionId())) {
            SESSION.playerLeft(player.getUUID());
            throw PortalRequestFields.error("message.riftgun.external_destination_unavailable");
        }
        Destination destination = destination(player, selection);
        boolean opened = surfaceFaceRequest == null
            ? PortalOpenCoordinator.openTransient(player, data, destination, mode, gun, fromGui)
            : PortalOpenCoordinator.openTransientSurfaceFace(
                player, data, destination, mode, gun, surfaceFaceRequest);
        if (opened) {
            PortalNetworking.sendSnapshot(player, false, gun);
            if (fromGui) PortalNetworking.sendPortalOpened(player);
        }
        return true;
    }

    public static void clearSelection(UUID playerId) {
        SESSION.playerLeft(playerId);
    }

    public static void reset() {
        SESSION.clear();
    }

    private static ExternalDestinationRequest.DecodeResult decode(
        ServerPlayer player,
        CompoundTag request
    ) {
        return ExternalDestinationRequest.decode(request,
            RiftConfigs.server().mapWaypointIntegration().enabled(),
            dimension -> knownDimension(player, dimension));
    }

    private static boolean knownDimension(ServerPlayer player, String dimensionId) {
//? if >=1.21.11 {
        /*Identifier parsed = Identifier.tryParse(dimensionId);
*///?} else {
        ResourceLocation parsed = ResourceLocation.tryParse(dimensionId);
//?}
        if (parsed == null) return false;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, parsed);
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        return server != null && server.getLevel(key) != null;
    }

    private static Destination destination(
        ServerPlayer player,
        ExternalDestinationSelection selection
    ) {
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.parse(selection.dimensionId());
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.parse(selection.dimensionId());
//?}
        return new Destination(UUID.randomUUID(), selection.name(), PortalPlayerData.DEFAULT_GROUP_ID,
            ResourceKey.create(Registries.DIMENSION, dimensionId), selection.x(), selection.y(),
            selection.z(), player.getYRot(), player.level().getGameTime(), 0L, false);
    }

    private static PortalRequestException error(ExternalDestinationRequest.Error error) {
        return PortalRequestFields.error(switch (error) {
            case DISABLED -> "message.riftgun.map_waypoint_integration_disabled";
            case UNKNOWN_DIMENSION -> "message.riftgun.dimension_unavailable";
            default -> "message.riftgun.invalid_request";
        });
    }

    private ExternalDestinationActions() {}
}
