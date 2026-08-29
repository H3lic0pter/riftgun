package dev.riftgun.pairing;

import dev.riftgun.core.registry.RiftContent;
import dev.riftgun.portal.PortalEntity;
import dev.riftgun.service.PortalGunIdentity;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Converts pre-lightweight dormant A/B entities when their owning gun is available. */
public final class PortalPairingLegacyMigration {
    public static boolean tryMigrate(PortalEntity portal, UUID ownerId) {
        PortalPairingEndpoint endpoint = portal.pairingEndpoint();
        UUID gunId = portal.pairingGunId();
        if (!portal.pairingDormant() || gunId == null || ownerId == null
            || endpoint != PortalPairingEndpoint.A && endpoint != PortalPairingEndpoint.B
            || !(portal.level() instanceof ServerLevel level)) return false;

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return false;
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack gun = owner.getInventory().getItem(slot);
            if (!gun.is(RiftContent.PORTAL_GUN.get())
                || !gunId.equals(PortalGunIdentity.existing(gun))) continue;
            if (PortalPairingPendingEndpoints.get(gun) == null) {
                PortalPairingPendingEndpoints.save(
                    gun, level.dimension(), portal.placement(), endpoint);
            }
            portal.discard();
            return true;
        }
        return false;
    }

    private PortalPairingLegacyMigration() {}
}
