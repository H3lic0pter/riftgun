package dev.riftgun.service;

import dev.riftgun.api.CoordinateNoteRequest;
import dev.riftgun.api.CoordinateNoteResult;
import dev.riftgun.api.CoordinateNoteStatus;
import dev.riftgun.api.RiftGunCoordinateNoteApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/** Adapts external coordinate-note requests to Rift Gun's authoritative sharing service. */
public final class DefaultRiftGunCoordinateNoteApi implements RiftGunCoordinateNoteApi {
    public static final DefaultRiftGunCoordinateNoteApi INSTANCE = new DefaultRiftGunCoordinateNoteApi();

    @Override
    public CoordinateNoteResult create(CoordinateNoteRequest request) {
//? if >=1.21.11 {
        /*MinecraftServer server = request.player().level().getServer();
*///?} else {
        MinecraftServer server = request.player().getServer();
//?}
        if (server == null) return result(CoordinateNoteStatus.TARGET_DIMENSION_UNAVAILABLE,
            "message.riftgun.dimension_unavailable");
        if (!server.isSameThread()) return result(CoordinateNoteStatus.WRONG_THREAD,
            "message.riftgun.api_wrong_thread");
        return CoordinateSharingService.createExternalNote(request);
    }

    private static CoordinateNoteResult result(CoordinateNoteStatus status, String key) {
        return new CoordinateNoteResult(status, Component.translatable(key));
    }

    private DefaultRiftGunCoordinateNoteApi() {}
}
