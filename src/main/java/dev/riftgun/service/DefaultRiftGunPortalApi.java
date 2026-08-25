package dev.riftgun.service;

import dev.riftgun.api.PortalDestination;
import dev.riftgun.api.PortalOpenRequest;
import dev.riftgun.api.PortalOpenResult;
import dev.riftgun.api.PortalOpenStatus;
import dev.riftgun.api.RiftGunPortalApi;
import dev.riftgun.data.Destination;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.data.PortalPlayerData;
import dev.riftgun.network.PortalNetworking;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Adapts the stable public API to Rift Gun's existing portal-opening workflow. */
public final class DefaultRiftGunPortalApi implements RiftGunPortalApi {
    public static final DefaultRiftGunPortalApi INSTANCE = new DefaultRiftGunPortalApi();

    @Override
    public PortalOpenResult openPortal(PortalOpenRequest request) {
        ServerPlayer player = request.opener();
//? if >=1.21.11 {
        /*MinecraftServer server = player.level().getServer();
*///?} else {
        MinecraftServer server = player.getServer();
//?}
        if (server == null) {
            return rejected(PortalOpenStatus.TARGET_DIMENSION_UNAVAILABLE,
                "message.riftgun.dimension_unavailable");
        }
        if (!server.isSameThread()) {
            return rejected(PortalOpenStatus.WRONG_THREAD, "message.riftgun.api_wrong_thread");
        }

        PortalGunLocator.LocatedGun locatedGun = PortalGunLocator.first(player).orElse(null);
        if (locatedGun == null) {
            return rejected(PortalOpenStatus.NO_PORTAL_GUN, "message.riftgun.no_portal_gun");
        }

        PortalDestination target = request.destination();
//? if >=1.21.11 {
        /*Identifier dimensionId = Identifier.tryParse(target.dimensionId().toString());
*///?} else {
        ResourceLocation dimensionId = ResourceLocation.tryParse(target.dimensionId().toString());
//?}
        if (dimensionId == null) {
            return rejected(PortalOpenStatus.TARGET_DIMENSION_UNAVAILABLE,
                "message.riftgun.dimension_unavailable");
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        long gameTime = player.level().getGameTime();
        Destination destination = new Destination(
            UUID.randomUUID(), request.sourceId().toString(), PortalPlayerData.DEFAULT_GROUP_ID,
            dimension, target.x(), target.y(), target.z(), target.yaw(), gameTime, 0L, false);
        PortalPlayerData data = PortalDataStore.load(player);
        PortalOpenResult result = PortalOpenCoordinator.openTransientResult(
            player, data, destination, data.settings().placementMode(), locatedGun, false,
            request.transitAuthorization());
        if (result.opened()) PortalNetworking.sendSnapshot(player, false, locatedGun);
        return result;
    }

    private static PortalOpenResult rejected(PortalOpenStatus status, String messageKey) {
        return PortalOpenResult.rejected(status, Component.translatable(messageKey));
    }

    private DefaultRiftGunPortalApi() {}
}
