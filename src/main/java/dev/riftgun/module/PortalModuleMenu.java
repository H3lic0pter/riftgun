package dev.riftgun.module;

import dev.riftgun.data.PortalDataStore;
import dev.riftgun.service.PortalGunLocator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PortalModuleMenu extends AbstractContainerMenu {
    public static final int MODULE_SLOT_COUNT = PortalGunModules.SLOT_COUNT;
    public static final int MODULE_COLUMNS = 9;
    public static final int MODULE_ROWS = 3;
    public static final int MODULE_START_X = 8;
    public static final int MODULE_START_Y = 35;
    public static final int PLAYER_INVENTORY_Y = 108;
    public static final int HOTBAR_Y = 166;
    private static final int DATA_INACTIVE_SLOTS = 0;
    private static final int DATA_USED_SLOTS = 1;
    private static final int DATA_UNLOCKED_SLOTS = 2;
    private static final int DATA_COUNT = 3;

    private final Container modules;
    private final ContainerData data;
    private final CompoundTag gunReference;

    public PortalModuleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, new SimpleContainer(MODULE_SLOT_COUNT),
            readReference(buffer), new SimpleContainerData(DATA_COUNT));
    }

    public PortalModuleMenu(int containerId, Inventory inventory, PortalGunLocator.LocatedGun locatedGun) {
        this(containerId, inventory,
            new PortalGunModuleContainer((ServerPlayer) inventory.player, locatedGun,
                PortalDataStore.load(inventory.player).settings().smartDistance()),
            locatedGun.saveReference(), serverData(locatedGun.stack()));
    }

    private PortalModuleMenu(int containerId, Inventory inventory, Container modules,
                             CompoundTag gunReference, ContainerData data) {
        super(PortalModuleMenus.MODULES.get(), containerId);
        checkContainerSize(modules, MODULE_SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.modules = modules;
        this.data = data;
        this.gunReference = gunReference;

        for (int slot = 0; slot < MODULE_SLOT_COUNT; slot++) {
            int column = slot % MODULE_COLUMNS;
            int row = slot / MODULE_COLUMNS;
            addSlot(new ModuleSlot(modules, slot,
                MODULE_START_X + column * 18, MODULE_START_Y + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = column + row * 9 + 9;
                addSlot(playerSlot(inventory, inventorySlot, 8 + column * 18,
                    PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(playerSlot(inventory, column, 8 + column * 18, HOTBAR_Y));
        }
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, PortalGunLocator.LocatedGun locatedGun) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (containerId, inventory, ignored) -> new PortalModuleMenu(containerId, inventory, locatedGun),
            Component.translatable("screen.riftgun.modules.title")),
            buffer -> buffer.writeNbt(locatedGun.saveReference()));
    }

    public CompoundTag gunReference() {
        return gunReference.copy();
    }

    public int inactiveSlots() {
        return data.get(DATA_INACTIVE_SLOTS);
    }

    public int usedSlots() {
        return data.get(DATA_USED_SLOTS);
    }

    public int unlockedSlots() {
        return Math.max(PortalGunModules.BASE_SLOT_COUNT,
            Math.min(MODULE_SLOT_COUNT, data.get(DATA_UNLOCKED_SLOTS)));
    }

    @Override
    public boolean stillValid(Player player) {
        if (modules instanceof PortalGunModuleContainer container) return container.stillValid(player);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < MODULE_SLOT_COUNT) {
            if (!moveItemStackTo(source, MODULE_SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!PortalModuleRegistry.isModule(source)
                || !moveItemStackTo(source, 0, unlockedSlots(), false)) return ItemStack.EMPTY;
        }
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private Slot playerSlot(Inventory inventory, int slot, int x, int y) {
        return isLockedInventorySlot(slot)
            ? new LockedGunSlot(inventory, slot, x, y)
            : new Slot(inventory, slot, x, y);
    }

    private boolean isLockedInventorySlot(int slot) {
        return "vanilla_inventory".equals(gunReference.getString("Locator"))
            && gunReference.getCompound("Token").contains("Slot")
            && gunReference.getCompound("Token").getInt("Slot") == slot;
    }

    private static ContainerData serverData(ItemStack gun) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                var modules = PortalGunModules.load(gun);
                return switch (index) {
                    case DATA_INACTIVE_SLOTS -> PortalGunModules.inactiveSlots(modules,
                        PortalModuleRules.current());
                    case DATA_USED_SLOTS -> modules.stream()
                        .mapToInt(stack -> stack.isEmpty() ? 0 : 1).sum();
                    case DATA_UNLOCKED_SLOTS -> PortalGunModules.unlockedSlotCount(modules);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static CompoundTag readReference(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? new CompoundTag() : tag;
    }

    private final class ModuleSlot extends Slot {
        private ModuleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && PortalModuleRegistry.isModule(stack)
                && container.canPlaceItem(getContainerSlot(), stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            boolean allowed = super.mayPickup(player)
                && PortalGunModules.canRemove(container, getContainerSlot());
            if (!allowed && player.level().isClientSide) {
                player.displayClientMessage(Component.translatable(
                    "message.riftgun.modules.clear_expanded_slots"), true);
            }
            return allowed;
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < unlockedSlots();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class LockedGunSlot extends Slot {
        private LockedGunSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
