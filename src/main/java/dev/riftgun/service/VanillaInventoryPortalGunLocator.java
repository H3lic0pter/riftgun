package dev.riftgun.service;
import dev.riftgun.core.nbt.Nbt;
import dev.riftgun.core.RiftConstants;
import dev.riftgun.core.registry.RiftContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class VanillaInventoryPortalGunLocator implements PortalGunLocator {
    private static final String ID = "vanilla_inventory";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<LocatedGun> locate(ServerPlayer player) {
        Optional<LocatedGun> held = locateHeld(player);
        if (held.isPresent()) return held;
        Inventory inventory = player.getInventory();
                //? if >=1.21.11 {
        /*int selected = inventory.getSelectedSlot();
        *///?} else {
        int selected = inventory.selected;
        //?}
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot != selected && slot != Inventory.SLOT_OFFHAND && isGun(inventory.getItem(slot))) {
                return Optional.of(located(inventory, slot));
            }
        }
        return Optional.empty();
    }

    static Optional<LocatedGun> locateHeld(ServerPlayer player) {
        Inventory inventory = player.getInventory();
                //? if >=1.21.11 {
        /*int selected = inventory.getSelectedSlot();
        *///?} else {
        int selected = inventory.selected;
        //?}
        return PortalShortcutGunSelection.preferMainHand(
            () -> isGun(inventory.getItem(selected))
                ? Optional.of(located(inventory, selected)) : Optional.empty(),
            () -> isGun(inventory.getItem(Inventory.SLOT_OFFHAND))
                ? Optional.of(located(inventory, Inventory.SLOT_OFFHAND)) : Optional.empty());
    }

    @Override
    public List<LocatedGun> locateAll(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<LocatedGun> guns = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isGun(inventory.getItem(slot))) guns.add(located(inventory, slot));
        }
        return List.copyOf(guns);
    }

    public static Optional<LocatedGun> locateMainHand(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        //? if >=1.21.11 {
        /*int selected = inventory.getSelectedSlot();
        *///?} else {
        int selected = inventory.selected;
        //?}
        return isGun(inventory.getItem(selected))
            ? Optional.of(located(inventory, selected)) : Optional.empty();
    }

    @Override
    public Optional<ItemStack> resolve(ServerPlayer player, CompoundTag token) {
        if (!token.contains("Slot")) return Optional.empty();
        int slot = Nbt.getInt(token, "Slot");
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= inventory.getContainerSize()) return Optional.empty();
        ItemStack stack = inventory.getItem(slot);
        return isGun(stack) ? Optional.of(stack) : Optional.empty();
    }

    private static LocatedGun located(Inventory inventory, int slot) {
        CompoundTag token = new CompoundTag();
        token.putInt("Slot", slot);
        return new LocatedGun(ID, token, inventory.getItem(slot));
    }

    private static boolean isGun(ItemStack stack) {
        return stack.is(RiftContent.PORTAL_GUN.get());
    }
}
