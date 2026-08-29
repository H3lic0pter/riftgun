package dev.riftgun.service;

import dev.riftgun.fuel.PortalGunComponents;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Owns the persistent identity used to bind an open GUI to its exact gun stack. */
public final class PortalGunIdentity {
    public static UUID ensure(ItemStack gun) {
        UUID existing = existing(gun);
        if (existing != null) return existing;
        UUID created = UUID.randomUUID();
        gun.set(PortalGunComponents.INSTANCE_ID, created);
        return created;
    }

    public static boolean matches(ItemStack gun, CompoundTag reference) {
        UUID actual = existing(gun);
        return actual != null && PortalGunReference.matches(reference, actual);
    }

    public static @Nullable UUID existing(ItemStack gun) {
        return gun.get(PortalGunComponents.INSTANCE_ID);
    }

    private PortalGunIdentity() {}
}
