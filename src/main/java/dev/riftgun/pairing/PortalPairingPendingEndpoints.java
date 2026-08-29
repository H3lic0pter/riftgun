package dev.riftgun.pairing;

import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.portal.PortalPlacement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** ItemStack component boundary for the pure pending-endpoint value object. */
public final class PortalPairingPendingEndpoints {
    public static @Nullable PortalPairingPendingEndpoint get(ItemStack gun) {
        CompoundTag tag = gun.get(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
        return tag == null ? null : PortalPairingPendingEndpoint.load(tag);
    }

    public static void save(ItemStack gun, ResourceKey<Level> dimension,
                            PortalPlacement placement, PortalPairingEndpoint endpoint) {
        gun.set(PortalGunComponents.PENDING_PAIRING_ENDPOINT,
            new PortalPairingPendingEndpoint(dimension, placement, endpoint).save());
    }

    public static void clear(ItemStack gun) {
        gun.remove(PortalGunComponents.PENDING_PAIRING_ENDPOINT);
    }

    private PortalPairingPendingEndpoints() {}
}
