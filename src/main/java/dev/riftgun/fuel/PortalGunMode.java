package dev.riftgun.fuel;

import net.minecraft.world.item.ItemStack;

public final class PortalGunMode {
    public static boolean bucketMode(ItemStack stack) {
        return stack.getOrDefault(PortalGunComponents.BUCKET_MODE, false);
    }

    public static void bucketMode(ItemStack stack, boolean enabled) {
        if (enabled) stack.set(PortalGunComponents.BUCKET_MODE, true);
        else stack.remove(PortalGunComponents.BUCKET_MODE);
    }

    private PortalGunMode() {}
}
