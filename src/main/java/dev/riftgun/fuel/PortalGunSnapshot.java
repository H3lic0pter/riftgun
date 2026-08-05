package dev.riftgun.fuel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class PortalGunSnapshot {
    public static CompoundTag create(ItemStack gun) {
        CompoundTag tag = new CompoundTag();
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack fluid = tank.getFluid();
        tag.putBoolean("BucketMode", PortalGunMode.bucketMode(gun));
        tag.putInt("Amount", fluid.getAmount());
        tag.putInt("Capacity", PortalGunTank.NOMINAL_CAPACITY);
        tag.putBoolean("Overfilled", fluid.getAmount() > PortalGunTank.NOMINAL_CAPACITY);
        PortalFuelProfiles.resolve(fluid).ifPresent(profile -> {
            tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
            tag.putInt("Rgb", profile.rgb());
            tag.putBoolean("CrossDimension", profile.crossDimension());
        });
        return tag;
    }

    private PortalGunSnapshot() {}
}
