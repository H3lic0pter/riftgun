package dev.riftgun.module;

import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.network.PortalNetworking;
import dev.riftgun.relocation.EntityRelocationRouting;
import dev.riftgun.service.PortalGunLocator;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PortalGunModuleContainer extends SimpleContainer {
    private final ServerPlayer owner;
    private final PortalGunLocator.LocatedGun locatedGun;
    private final int legacySmartDistance;
    private NonNullList<ItemStack> previous;

    public PortalGunModuleContainer(ServerPlayer owner, PortalGunLocator.LocatedGun locatedGun,
                                    int legacySmartDistance) {
        super(PortalGunModules.SLOT_COUNT);
        this.owner = owner;
        this.locatedGun = locatedGun;
        this.legacySmartDistance = legacySmartDistance;
        NonNullList<ItemStack> stored = PortalGunModules.load(locatedGun.stack());
        for (int slot = 0; slot < getContainerSize(); slot++) getItems().set(slot, stored.get(slot));
        PortalGunModuleSettings.ensure(locatedGun.stack(), legacySmartDistance);
        previous = copyItems();
    }

    public ItemStack gun() {
        return locatedGun.stack();
    }

    public PortalGunLocator.LocatedGun locatedGun() {
        return locatedGun;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= getContainerSize()) return false;
        if (slot >= PortalGunModules.unlockedSlotCount(getItems())) return false;
        if (!getItem(slot).isEmpty()) return false;
        return PortalGunModules.canAdd(getItems(), stack, PortalModuleRules.current());
    }

    public boolean canRemoveModule(int slot) {
        return PortalGunModules.canRemove(getItems(), slot);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player != owner || !player.isAlive()) return false;
        return PortalGunLocator.resolveReference(owner, locatedGun.saveReference())
            .map(resolved -> resolved.stack() == locatedGun.stack())
            .orElse(false);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (previous == null) return;

        PortalModuleRules rules = PortalModuleRules.current();
        int oldReservoirCount = activeCount(previous, PortalModuleKind.RESERVOIR_EXPANSION, rules);
        int newReservoirCount = activeCount(getItems(), PortalModuleKind.RESERVOIR_EXPANSION, rules);
        int oldActiveRange = activeCount(previous, PortalModuleKind.SURFACE_RANGE, rules);
        int newActiveRange = activeCount(getItems(), PortalModuleKind.SURFACE_RANGE, rules);
        int oldActiveRelocation = activeCount(previous, PortalModuleKind.ENTITY_RELOCATION, rules);
        int newActiveRelocation = activeCount(getItems(), PortalModuleKind.ENTITY_RELOCATION, rules);

        PortalGunModules.save(gun(), getItems());
        if (newActiveRange > oldActiveRange) {
            PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun(), legacySmartDistance);
            int oldMaximum = rules.maximumSurfaceRangeFor(oldActiveRange);
            if (settings.desiredSurfaceRange() == oldMaximum) {
                settings.withDesiredSurfaceRange(rules.maximumSurfaceRangeFor(newActiveRange)).save(gun());
            }
        }
        if (newReservoirCount < oldReservoirCount) new PortalGunTank(gun()).truncateToNominalCapacity();
        if (oldActiveRelocation > 0 && newActiveRelocation == 0) {
            var data = PortalDataStore.load(owner);
            var normalized = EntityRelocationRouting.normalizePlacementMode(
                data.settings().placementMode(), false);
            if (normalized != data.settings().placementMode()) {
                data.settings(data.settings().withPlacementMode(normalized));
                PortalDataStore.save(owner, data);
                PortalNetworking.sendSnapshot(owner, false, locatedGun);
            }
        }
        previous = copyItems();
    }

    private static int activeCount(Iterable<ItemStack> items, PortalModuleKind kind, PortalModuleRules rules) {
        return PortalGunModules.activeCount(items, kind, rules);
    }

    private NonNullList<ItemStack> copyItems() {
        NonNullList<ItemStack> copy = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < getContainerSize(); slot++) copy.set(slot, getItem(slot).copy());
        return copy;
    }
}
