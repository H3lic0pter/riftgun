package dev.riftgun.core.fuel;

/** Loader-neutral storage operations for one Portal Gun stack. */
public interface PortalGunFuelStore {
    PortalFluidContent content();

    int capacity();

    int fill(PortalFluidContent input, boolean simulate);

    PortalFluidContent drain(int amount, boolean simulate);
}
