package dev.riftgun.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    /** Enumerates every gun owned through this locator; legacy integrations expose their first. */
    default List<LocatedGun> locateAll(ServerPlayer player) {
        return locate(player).stream().toList();
    }

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

    static List<LocatedGun> all(ServerPlayer player) {
        LinkedHashMap<java.util.UUID, LocatedGun> found = new LinkedHashMap<>();
        for (PortalGunLocator locator : LOCATORS) {
            for (LocatedGun gun : locator.locateAll(player)) {
                if (gun == null || gun.stack().isEmpty()) continue;
                found.putIfAbsent(PortalGunIdentity.ensure(gun.stack()), gun);
            }
        }
        return new ArrayList<>(found.values());
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
