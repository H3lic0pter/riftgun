package dev.riftgun.core.fuel;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;

/** Once-installed factory for the active loader's saved-item fuel store. */
public final class RiftFuelStores {
    private static volatile Factory factory;

    public static synchronized void install(Factory installed) {
        if (factory != null) throw new IllegalStateException("fuel store factory already installed");
        factory = Objects.requireNonNull(installed, "installed");
    }

    public static PortalGunFuelStore open(ItemStack gun) {
        Factory installed = Objects.requireNonNull(factory, "fuel store factory has not been installed");
        return installed.open(gun);
    }

    @FunctionalInterface
    public interface Factory {
        PortalGunFuelStore open(ItemStack gun);
    }

    private RiftFuelStores() {}
}
