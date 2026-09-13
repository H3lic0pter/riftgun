package dev.riftgun.appearance;

import dev.riftgun.api.RiftResourceId;
import dev.riftgun.fuel.PortalGunComponents;
import net.minecraft.world.item.ItemStack;

/** Stored identity is independent of whichever resources this client has installed. */
public final class PortalGunSkin {
    public static final String DEFAULT = "riftgun:default";
    public static final int MAX_ID_LENGTH = 256;

    public static boolean validId(String id) {
        if (id == null || id.length() > MAX_ID_LENGTH) return false;
        try {
            RiftResourceId.parse(id);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static String current(ItemStack stack) {
        return stack.getOrDefault(PortalGunComponents.SKIN, DEFAULT);
    }

    public static void set(ItemStack stack, String id) {
        if (!validId(id)) throw new IllegalArgumentException("Invalid portal gun skin ID");
        stack.set(PortalGunComponents.SKIN, id);
    }

    private PortalGunSkin() {}
}
