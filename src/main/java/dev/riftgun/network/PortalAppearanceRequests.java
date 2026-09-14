package dev.riftgun.network;

import dev.riftgun.appearance.PortalGunSkin;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.fuel.PortalGunVisualState;
import dev.riftgun.service.PortalGunLocator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Appearance requests always resolve the supplied instance reference, never another held gun. */
public final class PortalAppearanceRequests {
    public static void handle(ServerPlayer player, CompoundTag request, boolean apply) {
        CompoundTag response = new CompoundTag();
        response.putString("Kind", "Appearance");
        response.putString("RequestId", Nbt.getString(request, "RequestId"));
        response.putBoolean("Applied", apply);
        response.put("GunReference", Nbt.getCompound(request, "GunReference").copy());
        String error = "";
        var gun = request.contains("GunReference")
            ? PortalGunLocator.resolveReference(player, Nbt.getCompound(request, "GunReference")).orElse(null)
            : null;
        if (player.isSpectator()) {
            error = "message.riftgun.spectator_denied";
        } else if (gun == null) {
            error = "screen.riftgun.appearance.invalid_gun";
        } else if (apply && !PortalGunSkin.validId(Nbt.getString(request, "Skin"))) {
            error = "screen.riftgun.appearance.invalid_skin";
        } else if (!dev.riftgun.appearance.GunPresentationDefaults.initialize(player, gun.stack())) {
            error = "message.riftgun.invalid_request";
        } else {
            if (apply) {
                if (request.contains("Presentation")) {
                    var selected = dev.riftgun.appearance.GunPresentation.load(Nbt.getCompound(request, "Presentation"));
                    gun.stack().set(dev.riftgun.fuel.PortalGunComponents.PRESENTATION,
                        new dev.riftgun.appearance.GunPresentation(
                            selected.visual(), selected.animation(), selected.sounds(), true));
                }
                PortalGunSkin.set(gun.stack(), Nbt.getString(request, "Skin"));
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
            }
            response.putString("Skin", PortalGunSkin.current(gun.stack()));
            response.put("Presentation", dev.riftgun.appearance.GunPresentation.current(gun.stack()).save());
            response.putBoolean("Foil", gun.stack().hasFoil());
            response.put("GunReference", gun.saveReference());
            PortalGunVisualState visual = PortalGunVisualState.current(gun.stack());
            response.putInt("LiquidTint", visual.liquidTint());
            response.putBoolean("CoreVisible", visual.coreVisible());
            response.putInt("FuelRgb", visual.fuelRgb());
            response.putBoolean("PairingMode", visual.pairingMode());
        }
        response.putString("Error", error);
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(response));
    }

    private PortalAppearanceRequests() {}
}
