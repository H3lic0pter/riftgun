package dev.riftgun.fuel;

import dev.riftgun.config.ServerConfig;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class PortalFuelManager {
    public static Plan plan(ServerPlayer player, ItemStack gun, ResourceKey<Level> destinationDimension) {
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack stored = tank.getFluid();
        Optional<PortalFuelProfile> resolved = PortalFuelProfiles.resolve(stored);
        if (resolved.isEmpty()) return Plan.failure("message.riftgun.fuel_empty");

        PortalFuelProfile profile = resolved.get();
        boolean crossDimension = !player.level().dimension().equals(destinationDimension);
        if (crossDimension && !profile.crossDimension()) {
            return Plan.failure("message.riftgun.fuel_dimension_denied");
        }

        int rolled = PortalFuelCost.choose(profile.minimumConsumption(), profile.maximumConsumption(),
            ServerConfig.VALUES.randomConsumption.get(), player.getRandom()::nextInt);
        int affordable = PortalFuelCost.affordableCost(stored.getAmount(), profile.minimumConsumption(), rolled);
        if (affordable == 0) return Plan.failure("message.riftgun.fuel_insufficient");
        return Plan.success(new PortalFuelUse(profile, affordable));
    }

    public static boolean consume(ItemStack gun, PortalFuelUse use) {
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack stored = tank.getFluid();
        Optional<PortalFuelProfile> current = PortalFuelProfiles.resolve(stored);
        if (current.isEmpty() || !current.get().id().equals(use.profile().id())
            || stored.getAmount() < use.amount()) return false;
        if (use.amount() == 0) return true;
        return tank.drain(use.amount(), IFluidHandler.FluidAction.EXECUTE).getAmount() == use.amount();
    }

    public record Plan(PortalFuelUse use, String errorKey) {
        public static Plan success(PortalFuelUse use) {
            return new Plan(use, null);
        }

        public static Plan failure(String errorKey) {
            return new Plan(null, errorKey);
        }

        public boolean successful() {
            return use != null;
        }
    }

    private PortalFuelManager() {}
}
