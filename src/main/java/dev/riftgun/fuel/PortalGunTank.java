package dev.riftgun.fuel;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import dev.riftgun.module.PortalGunCapabilities;
import dev.riftgun.module.PortalGunModuleSettings;
import dev.riftgun.module.PortalModuleRules;

public final class PortalGunTank extends FluidHandlerItemStack {
    public static final int NOMINAL_CAPACITY = PortalModuleRules.DEFAULT_BASE_CAPACITY;
    public static final int WORLD_SOURCE_AMOUNT = 1000;

    public PortalGunTank(ItemStack container) {
        super(PortalGunComponents.FLUID, container,
            PortalGunCapabilities.resolve(container, PortalGunModuleSettings.DEFAULT_SMART_DISTANCE).nominalCapacity());
    }

    @Override
    public int getTankCapacity(int tank) {
        return Math.max(nominalCapacity(), getFluid().getAmount());
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return PortalFuelProfiles.accepts(stack);
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return PortalFuelProfiles.accepts(fluid);
    }

    public boolean tryFillWorldSource(FluidStack source, WorldFluidOverflowPolicy overflowPolicy) {
        if (!canFillWorldSource(source, overflowPolicy)) return false;
        FluidStack stored = getFluid();
        int accepted = overflowPolicy.acceptedAmount(stored.getAmount(), nominalCapacity(), source.getAmount());

        FluidStack result = stored.isEmpty() ? source.copy() : stored.copy();
        if (!stored.isEmpty()) result.grow(accepted);
        setFluid(result);
        return true;
    }

    public boolean canFillWorldSource(FluidStack source, WorldFluidOverflowPolicy overflowPolicy) {
        if (source.getAmount() != WORLD_SOURCE_AMOUNT || !canFillFluidType(source)) return false;
        FluidStack stored = getFluid();
        if (!stored.isEmpty() && !FluidStack.isSameFluidSameComponents(stored, source)) return false;
        return overflowPolicy.acceptedAmount(stored.getAmount(), nominalCapacity(), source.getAmount())
            == source.getAmount();
    }

    public int nominalCapacity() {
        return capacity;
    }

    public void truncateToNominalCapacity() {
        FluidStack stored = getFluid();
        if (stored.getAmount() <= nominalCapacity()) return;
        setFluid(stored.copyWithAmount(nominalCapacity()));
    }

    public void clear() {
        setContainerToEmpty();
    }
}
