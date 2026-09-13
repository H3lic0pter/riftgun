package dev.riftgun.network;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.sound.PortalSoundSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Updates only sound preferences, independently of the gun's placement-mode capabilities. */
public final class PortalSoundRequests {
    public static void handle(ServerPlayer player, CompoundTag request) {
        var data = PortalDataStore.load(player);
        String error = "";
        if (player.isSpectator()) {
            error = "message.riftgun.spectator_denied";
        } else if (!request.contains("GunReference")
            || PortalGunLocator.resolveReference(player, Nbt.getCompound(request, "GunReference")).isEmpty()) {
            error = "screen.riftgun.appearance.invalid_gun";
        } else if (!Nbt.contains(request, "PortalSounds", Tag.TAG_COMPOUND)) {
            error = "message.riftgun.invalid_request";
        } else {
            data.settings(data.settings().withPortalSounds(
                PortalSoundSettings.load(Nbt.getCompound(request, "PortalSounds"))));
            PortalDataStore.save(player, data);
        }
        CompoundTag response = new CompoundTag();
        response.putString("Kind", "PortalSounds");
        response.putString("RequestId", Nbt.getString(request, "RequestId"));
        response.putString("Error", error);
        response.put("PortalSounds", data.settings().portalSounds().save());
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(response));
    }

    private PortalSoundRequests() {}
}
