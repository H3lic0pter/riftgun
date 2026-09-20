package dev.riftgun.pairing;

import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Reads retired gun components once when a player first receives instance state. */
public final class PortalPairingLegacyMigration {
    public static @Nullable PortalPairingPendingEndpoint fromGuns(ServerPlayer owner) {
        PortalPairingPendingEndpoint newest = null;
        long newestTime = Long.MIN_VALUE;
        boolean changed = false;
        for (var located : PortalGunLocator.all(owner)) {
            var gun = located.stack();
            var tag = gun.get(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
            if (tag == null) continue;
            var marker = PortalPairingPendingEndpoint.load(tag);
            long startedAt = Nbt.getLong(tag, "StartedAt");
            if (marker != null && marker.belongsTo(owner.getUUID())
                && Nbt.hasUUID(tag, "Gun")
                && Nbt.getUUID(tag, "Gun").equals(PortalGunIdentity.existing(gun))
                && startedAt >= 0 && Nbt.getInt(tag, "DurationTicks") > 0
                && startedAt > newestTime) {
                newest = marker;
                newestTime = startedAt;
            }
            // Foreign markers must not become this holder's state or survive a later transfer.
            gun.remove(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
            changed = true;
        }
        if (changed) owner.getInventory().setChanged();
        return newest;
    }

    private PortalPairingLegacyMigration() {}
}
