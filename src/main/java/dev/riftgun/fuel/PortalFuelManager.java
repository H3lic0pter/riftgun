package dev.riftgun.fuel;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
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
        boolean crossDimension = !player.level().dimension().equals(destinationDimension);
        if (resolved.isPresent()) {
            PortalFuelProfile profile = resolved.get();
            if (crossDimension && !profile.crossDimension()) {
                return Plan.failure("message.riftgun.fuel_dimension_denied");
            }
            if (hasInfiniteFuel(gun)) {
                return Plan.success(selectLoadedFuel(profile, 0, true));
            }
            int rolled = PortalFuelCost.choose(profile.minimumConsumption(), profile.maximumConsumption(),
                RiftConfigs.server().fuel().randomConsumption(), player.getRandom()::nextInt);
            int affordable = PortalFuelCost.affordableCost(
                stored.getAmount(), profile.minimumConsumption(), rolled);
            return affordable == 0
                ? Plan.failure("message.riftgun.fuel_insufficient")
                : Plan.success(selectLoadedFuel(profile, affordable, false));
        }
        if (hasInfiniteFuel(gun)) return Plan.success(virtualUse(PortalFuelProfiles.dimensional(), 0));
        return Plan.failure("message.riftgun.fuel_empty");
    }

    public static boolean consume(ItemStack gun, PortalFuelUse use) {
        if (use.virtual()) return canConsume(gun, use);
        PortalGunTank tank = new PortalGunTank(gun);
        FluidStack stored = tank.getFluid();
        Optional<PortalFuelProfile> current = PortalFuelProfiles.resolve(stored);
        if (current.isEmpty() || !current.get().id().equals(use.profile().id())
            || stored.getAmount() < use.amount()) return false;
        if (use.amount() == 0) return true;
        return tank.drain(use.amount(), IFluidHandler.FluidAction.EXECUTE).getAmount() == use.amount();
    }

    public static boolean canConsume(ItemStack gun, PortalFuelUse use) {
        PortalGunTank tank = new PortalGunTank(gun);
        if (use.virtual()) {
            if (!hasInfiniteFuel(gun)) return false;
            return PortalFuelProfiles.resolve(tank.getFluid())
                .map(profile -> profile.id().equals(use.profile().id()))
                .orElseGet(() -> PortalFuelProfiles.dimensional().id().equals(use.profile().id()));
        }
        return PortalFuelProfiles.resolve(tank.getFluid())
            .filter(profile -> profile.id().equals(use.profile().id())).isPresent()
            && tank.getFluid().getAmount() >= use.amount();
    }

    public static boolean hasInfiniteFuel(ItemStack gun) {
        return PortalGunModules.activeCount(
            gun, PortalModuleKind.ZERO_POINT_FUEL, PortalModuleRules.current()) > 0;
    }

    public static PortalFuelUse virtualUse(PortalFuelProfile profile, int amount) {
        return new PortalFuelUse(profile, amount, true);
    }

    static PortalFuelUse selectLoadedFuel(PortalFuelProfile profile, int amount, boolean infiniteFuel) {
        return infiniteFuel ? virtualUse(profile, 0) : new PortalFuelUse(profile, amount);
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
