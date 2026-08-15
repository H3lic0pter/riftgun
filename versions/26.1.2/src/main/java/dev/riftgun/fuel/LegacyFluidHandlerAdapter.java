package dev.riftgun.fuel;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 26.1.2-only adapter: exposes the legacy {@link IFluidHandlerItem} tank
 * through the new {@link ResourceHandler} capability API. The gun's tank
 * logic (PortalGunTank) stays on the legacy fluid API; this adapter is the
 * seam for the NeoForge 26.1 capability rework.
 */
public final class LegacyFluidHandlerAdapter implements ResourceHandler<FluidResource> {
    private final IFluidHandlerItem tank;

    public LegacyFluidHandlerAdapter(IFluidHandlerItem tank) {
        this.tank = tank;
    }

    @Override
    public int size() {
        return tank.getTanks();
    }

    @Override
    public FluidResource getResource(int slot) {
        return FluidResource.of(tank.getFluidInTank(slot));
    }

    @Override
    public long getAmountAsLong(int slot) {
        return tank.getFluidInTank(slot).getAmount();
    }

    @Override
    public long getCapacityAsLong(int slot, FluidResource resource) {
        return tank.getTankCapacity(slot);
    }

    @Override
    public boolean isValid(int slot, FluidResource resource) {
        return true;
    }

    @Override
    public int insert(int slot, FluidResource resource, int amount, TransactionContext transaction) {
        return tank.fill(new FluidStack(resource.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int slot, FluidResource resource, int amount, TransactionContext transaction) {
        return tank.drain(new FluidStack(resource.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE).getAmount();
    }
}
