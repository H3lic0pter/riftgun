package dev.riftgun.network;

import dev.riftgun.appearance.GunPresentation;
import dev.riftgun.appearance.GunPresentationDefaults;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.network.RiftNetwork;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.service.PortalGunLocator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Validates the exact gun reference and applies one category, preserving the other choices. */
public final class GunPresentationRequests {
    public static void handle(ServerPlayer player, CompoundTag request) {
        var gun = PortalGunLocator.resolveReference(player, Nbt.getCompound(request, "GunReference")).orElse(null);
        String error = player.isSpectator() ? "message.riftgun.spectator_denied"
            : gun == null ? "screen.riftgun.appearance.invalid_gun" : "";
        if (error.isEmpty() && (!Nbt.contains(request, "Presentation", net.minecraft.nbt.Tag.TAG_COMPOUND)
                || !java.util.Set.of("PORTAL_VISUAL", "SHOT_ANIMATION", "SOUNDS")
                    .contains(Nbt.getString(request, "Category")))) {
            error = "message.riftgun.invalid_request";
        }
        if (error.isEmpty() && !GunPresentationDefaults.initialize(player, gun.stack())) {
            error = "message.riftgun.invalid_request";
        }
        if (error.isEmpty()) {
            var current = GunPresentation.current(gun.stack());
            var requested = GunPresentation.load(Nbt.getCompound(request, "Presentation"));
            var changed = switch (Nbt.getString(request, "Category")) {
                case "PORTAL_VISUAL" -> current.withVisual(requested.visual());
                case "SHOT_ANIMATION" -> current.withAnimation(requested.animation());
                case "SOUNDS" -> current.withSounds(requested.sounds());
                default -> current;
            };
            gun.stack().set(PortalGunComponents.PRESENTATION, changed);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        CompoundTag response = new CompoundTag();
        response.putString("Kind", "GunPresentation");
        response.putString("RequestId", Nbt.getString(request, "RequestId"));
        response.put("GunReference", Nbt.getCompound(request, "GunReference").copy());
        response.putString("Error", error);
        if (gun != null) response.put("Presentation", GunPresentation.current(gun.stack()).save());
        RiftNetwork.sendToPlayer(player, new PortalResponsePayload(response));
    }
    private GunPresentationRequests() {}
}
