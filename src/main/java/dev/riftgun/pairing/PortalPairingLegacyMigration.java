package dev.riftgun.pairing;

import dev.riftgun.portal.PortalEntity;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Converts legacy dormant pairing entities when their owning gun is available. */
public final class PortalPairingLegacyMigration {
    public static boolean tryMigrate(PortalEntity portal, UUID ownerId) {
        PortalPairingEndpoint endpoint = portal.pairingEndpoint();
        UUID gunId = portal.pairingGunId();
        if (!portal.pairingDormant() || gunId == null || ownerId == null
            || endpoint == PortalPairingEndpoint.NONE
            || !(portal.level() instanceof ServerLevel level)) return false;

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) return false;
        for (PortalGunLocator.LocatedGun located : PortalGunLocator.all(owner)) {
            var gun = located.stack();
            if (!gunId.equals(PortalGunIdentity.existing(gun))) continue;
            if (PortalPairingPendingEndpoints.get(gun) == null) {
                PortalPairingPendingEndpoints.save(
                    gun, ownerId, gunId, level.dimension(), portal.placement(), endpoint,
                    level.getServer().overworld().getGameTime(), portal.openDurationTicks());
            }
            portal.discard();
            return true;
        }
        return false;
    }

    private PortalPairingLegacyMigration() {}
}
