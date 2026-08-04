package dev.riftgun.service;

import dev.riftgun.RiftGun;
import net.minecraft.server.level.ServerPlayer;

public final class VanillaInventoryPortalGunLocator implements PortalGunLocator {
    @Override
    public boolean hasPortalGun(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(RiftGun.PORTAL_GUN.get())) return true;
        }
        return player.getOffhandItem().is(RiftGun.PORTAL_GUN.get());
    }
}

