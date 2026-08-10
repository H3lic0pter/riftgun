package dev.riftgun.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface PortalGunLocator {
    List<PortalGunLocator> LOCATORS = new CopyOnWriteArrayList<>();

    String id();

    Optional<LocatedGun> locate(ServerPlayer player);

    Optional<ItemStack> resolve(ServerPlayer player, CompoundTag token);

    static void register(PortalGunLocator locator) {
        LOCATORS.add(locator);
    }

    static Optional<LocatedGun> first(ServerPlayer player) {
        for (PortalGunLocator locator : LOCATORS) {
            Optional<LocatedGun> found = locator.locate(player);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    static Optional<LocatedGun> resolveReference(ServerPlayer player, CompoundTag reference) {
        String locatorId = PortalGunReference.locatorId(reference);
        CompoundTag token = PortalGunReference.token(reference);
        for (PortalGunLocator locator : LOCATORS) {
            if (!locator.id().equals(locatorId)) continue;
            return locator.resolve(player, token)
                .filter(stack -> !stack.isEmpty())
                .filter(stack -> PortalGunIdentity.matches(stack, reference))
                .map(stack -> new LocatedGun(locatorId, token.copy(), stack));
        }
        return Optional.empty();
    }

    static boolean anyHasPortalGun(ServerPlayer player) {
        return first(player).isPresent();
    }

    record LocatedGun(String locatorId, CompoundTag token, ItemStack stack) {
        public CompoundTag saveReference() {
            return PortalGunReference.capture(locatorId, token, PortalGunIdentity.ensure(stack));
        }
    }
}
