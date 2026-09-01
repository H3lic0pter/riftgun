package dev.riftgun.module;

import dev.riftgun.fuel.PortalGunTank;
import dev.riftgun.data.PortalDataStore;
import dev.riftgun.service.PortalClientSync;
import dev.riftgun.relocation.EntityRelocationRouting;
import dev.riftgun.service.PortalGunLocator;
import dev.riftgun.service.PortalGunIdentity;
import dev.riftgun.portal.PortalOwnerIndex;
import dev.riftgun.pairing.PortalPairingPendingEndpoints;
import java.util.UUID;
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
        if (!getItem(slot).isEmpty()) {
            return ItemStack.isSameItemSameComponents(getItem(slot), stack);
        }
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
        PortalGunModules.ActiveCounts oldCounts = PortalGunModules.activeCounts(previous, rules);
        PortalGunModules.ActiveCounts newCounts = PortalGunModules.activeCounts(getItems(), rules);
        int oldReservoirCount = oldCounts.count(PortalModuleKind.RESERVOIR_EXPANSION);
        int newReservoirCount = newCounts.count(PortalModuleKind.RESERVOIR_EXPANSION);
        int oldActiveRange = oldCounts.count(PortalModuleKind.SURFACE_RANGE);
        int newActiveRange = newCounts.count(PortalModuleKind.SURFACE_RANGE);
        int oldActiveRelocation = oldCounts.count(PortalModuleKind.ENTITY_RELOCATION);
        int newActiveRelocation = newCounts.count(PortalModuleKind.ENTITY_RELOCATION);
        int oldActivePairing = oldCounts.count(PortalModuleKind.PORTAL_PAIRING);
        int newActivePairing = newCounts.count(PortalModuleKind.PORTAL_PAIRING);
        int oldActiveRemote = oldCounts.count(PortalModuleKind.REMOTE);
        int newActiveRemote = newCounts.count(PortalModuleKind.REMOTE);

        PortalGunModules.save(gun(), getItems());
        if (newActiveRange > oldActiveRange) {
            PortalGunModuleSettings settings = PortalGunModuleSettings.ensure(gun(), legacySmartDistance);
            int oldMaximum = rules.maximumSurfaceRangeFor(oldActiveRange);
            if (settings.desiredRemoteDistance() == oldMaximum) {
                settings.withDesiredRemoteDistance(rules.maximumSurfaceRangeFor(newActiveRange)).save(gun());
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
                PortalClientSync.snapshot(owner, false, locatedGun);
            }
        }
        if (oldActivePairing > 0 && newActivePairing == 0) {
            UUID gunId = PortalGunIdentity.ensure(gun());
            PortalPairingPendingEndpoints.clear(gun());
//? if >=1.21.11 {
            /*var server = owner.level().getServer();
*///?} else {
            var server = owner.getServer();
//?}
            if (server != null) {
                PortalOwnerIndex.closeOwnedMatching(server, owner.getUUID(),
                    portal -> gunId.equals(portal.pairingGunId()));
            }
            PortalClientSync.snapshot(owner, false, locatedGun);
        }
        if (oldActiveRemote > 0 && newActiveRemote == 0) {
            // Preserve REMOTE as the saved preference. Capability resolution supplies the
            // effective FRONT fallback until the module is installed again.
            PortalClientSync.snapshot(owner, false, locatedGun);
        }
        previous = copyItems();
    }

    private NonNullList<ItemStack> copyItems() {
        NonNullList<ItemStack> copy = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < getContainerSize(); slot++) copy.set(slot, getItem(slot).copy());
        return copy;
    }
}
