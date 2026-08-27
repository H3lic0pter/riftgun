package dev.riftgun.fuel;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.core.fuel.PortalFluidContent;
import dev.riftgun.core.fuel.PortalGunFuelStore;
import dev.riftgun.core.fuel.RiftFuelStores;
import dev.riftgun.module.PortalGunModules;
import dev.riftgun.module.PortalModuleKind;
import dev.riftgun.module.PortalModuleRules;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PortalFuelManager {
    /** Resolves the gun's current profile without requiring or reserving one full portal charge. */
    public static Plan recognizedProfile(ItemStack gun) {
        PortalFluidContent stored = RiftFuelStores.open(gun).content();
        Optional<PortalFuelProfile> resolved = PortalFuelProfiles.resolve(stored.fluid());
        if (resolved.isPresent()) {
            return Plan.success(selectRecognizedFuel(resolved.get(), hasInfiniteFuel(gun)));
        }
        if (hasInfiniteFuel(gun)) return Plan.success(virtualUse(PortalFuelProfiles.dimensional(), 0));
        return Plan.failure("message.riftgun.fuel_empty");
    }

    public static Plan plan(ServerPlayer player, ItemStack gun, ResourceKey<Level> destinationDimension) {
        PortalGunFuelStore store = RiftFuelStores.open(gun);
        PortalFluidContent stored = store.content();
        Optional<PortalFuelProfile> resolved = PortalFuelProfiles.resolve(stored.fluid());
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
                stored.amount(), profile.minimumConsumption(), rolled);
            return affordable == 0
                ? Plan.failure("message.riftgun.fuel_insufficient")
                : Plan.success(selectLoadedFuel(profile, affordable, false));
        }
        if (hasInfiniteFuel(gun)) return Plan.success(virtualUse(PortalFuelProfiles.dimensional(), 0));
        return Plan.failure("message.riftgun.fuel_empty");
    }

    public static boolean consume(ItemStack gun, PortalFuelUse use) {
        if (use.virtual()) return canConsume(gun, use);
        PortalGunFuelStore store = RiftFuelStores.open(gun);
        PortalFluidContent stored = store.content();
        Optional<PortalFuelProfile> current = PortalFuelProfiles.resolve(stored.fluid());
        if (current.isEmpty() || !current.get().id().equals(use.profile().id())
            || stored.amount() < use.amount()) return false;
        if (use.amount() == 0) return true;
        return store.drain(use.amount(), false).amount() == use.amount();
    }

    public static boolean canConsume(ItemStack gun, PortalFuelUse use) {
        PortalGunFuelStore store = RiftFuelStores.open(gun);
        PortalFluidContent stored = store.content();
        if (use.virtual()) {
            if (!hasInfiniteFuel(gun)) return false;
            return PortalFuelProfiles.resolve(stored.fluid())
                .map(profile -> profile.id().equals(use.profile().id()))
                .orElseGet(() -> PortalFuelProfiles.dimensional().id().equals(use.profile().id()));
        }
        return PortalFuelProfiles.resolve(stored.fluid())
            .filter(profile -> profile.id().equals(use.profile().id())).isPresent()
            && stored.amount() >= use.amount();
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

    static PortalFuelUse selectRecognizedFuel(PortalFuelProfile profile, boolean infiniteFuel) {
        return selectLoadedFuel(profile, 0, infiniteFuel);
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
