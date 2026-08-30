package dev.riftgun.pairing;

import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalPlacement;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.service.PortalGunLocator;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** ItemStack component boundary for lightweight pairing markers. */
public final class PortalPairingPendingEndpoints {
    public static @Nullable PortalPairingPendingEndpoint get(ItemStack gun) {
        CompoundTag tag = gun.get(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
        return tag == null ? null : PortalPairingPendingEndpoint.load(tag);
    }

    public static @Nullable PortalPairingPendingEndpoint getValid(
        ItemStack gun, UUID ownerId, UUID gunId, long now
    ) {
        PortalPairingPendingEndpoint pending = get(gun);
        if (pending != null && pending.validFor(ownerId, gunId, now)) return pending;
        if (gun.get(PortalGunComponents.PENDING_PAIRING_ENDPOINT) != null) clear(gun);
        return null;
    }

    public static void save(ItemStack gun, UUID ownerId, UUID gunId,
                            ResourceKey<Level> dimension, PortalPlacement placement,
                            PortalPairingEndpoint endpoint, long startedAt, int durationTicks) {
        gun.set(PortalGunComponents.PENDING_PAIRING_ENDPOINT,
            new PortalPairingPendingEndpoint(ownerId, gunId, dimension, placement,
                endpoint, startedAt, durationTicks).save());
    }

    public static void save(ItemStack gun, PortalPairingPendingEndpoint pending) {
        gun.set(PortalGunComponents.PENDING_PAIRING_ENDPOINT, pending.save());
    }

    public static void clear(ItemStack gun) {
        gun.remove(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
    }

    /** Clears every pending pairing marker exposed by the player's registered gun locators. */
    public static int clearAll(ServerPlayer player) {
        int cleared = 0;
        for (PortalGunLocator.LocatedGun located : PortalGunLocator.all(player)) {
            ItemStack stack = located.stack();
            if (stack.get(PortalGunComponents.PENDING_PAIRING_ENDPOINT) == null) continue;
            clear(stack);
            cleared++;
        }
        if (cleared > 0) player.getInventory().setChanged();
        return cleared;
    }

    public static @Nullable PortalPairingPendingEndpoint getValid(
        ServerPlayer owner, PortalGunLocator.LocatedGun gun, long now
    ) {
        UUID gunId = PortalGunIdentity.ensure(gun.stack());
        return getValid(gun.stack(), owner.getUUID(), gunId, now);
    }

    private PortalPairingPendingEndpoints() {}
}
