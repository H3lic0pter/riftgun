package dev.riftgun.module;
import dev.riftgun.core.msg.Msg;
import dev.riftgun.core.nbt.Nbt;

import dev.riftgun.data.PortalDataStore;
import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.service.PortalGunLocator;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
//? if >=1.21.11 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else {
import net.minecraft.world.inventory.ClickType;
//?}
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

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
    private final Inventory inventory;
    private final PortalModuleRules syncedRules;

    public PortalModuleMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, new SimpleContainer(MODULE_SLOT_COUNT),
            readReference(buffer), readRules(buffer), new SimpleContainerData(DATA_COUNT));
    }

    public PortalModuleMenu(int containerId, Inventory inventory, PortalGunLocator.LocatedGun locatedGun) {
        this(containerId, inventory,
            new PortalGunModuleContainer((ServerPlayer) inventory.player, locatedGun,
                PortalDataStore.load(inventory.player).settings().smartDistance()),
            locatedGun.saveReference(), PortalModuleRules.current(), serverData(locatedGun.stack()));
    }

    private PortalModuleMenu(int containerId, Inventory inventory, Container modules,
                             CompoundTag gunReference, PortalModuleRules syncedRules,
                             ContainerData data) {
        super(PortalModuleMenus.MODULES.get(), containerId);
        checkContainerSize(modules, MODULE_SLOT_COUNT);
        checkContainerDataCount(data, DATA_COUNT);
        this.modules = modules;
        this.data = data;
        this.gunReference = gunReference;
        this.inventory = inventory;
        this.syncedRules = syncedRules;

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
            buffer -> {
                buffer.writeNbt(locatedGun.saveReference());
                buffer.writeNbt(PortalModuleRules.current().save());
            });
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
        if (index < MODULE_SLOT_COUNT) {
            int removable = PortalGunModules.maximumRemovableCount(modules, index, source.getCount());
            if (removable <= 0) return ItemStack.EMPTY;
            ItemStack moving = source.copyWithCount(removable);
            if (!moveItemStackTo(moving, MODULE_SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
            int moved = removable - moving.getCount();
            if (moved <= 0) return ItemStack.EMPTY;
            ItemStack result = source.copyWithCount(moved);
            source.shrink(moved);
            if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return result;
        } else {
            ItemStack original = source.copy();
            if (!PortalModuleRegistry.isModule(source)
                || !moveItemStackTo(source, 0, unlockedSlots(), false)) return ItemStack.EMPTY;
            if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            return original;
        }
    }

    @Override
    //? if >=1.21.11 {
    /*public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
    *///?} else {
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
    //?}
        //? if >=1.21.11 {
        /*if (clickType == ContainerInput.SWAP && slotId >= 0 && slotId < MODULE_SLOT_COUNT
        *///?} else {
        if (clickType == ClickType.SWAP && slotId >= 0 && slotId < MODULE_SLOT_COUNT
        //?}
            && button >= 0 && button < inventory.getContainerSize()) {
            ItemStack installed = modules.getItem(slotId);
            PortalModuleDefinition definition = PortalModuleRegistry.find(installed).orElse(null);
            if (definition != null && definition.kind() == PortalModuleKind.MODULE_BAY_EXPANSION) {
                ItemStack replacement = inventory.getItem(button);
                int replacementCount = PortalModuleRegistry.find(replacement)
                    .filter(candidate -> candidate.kind() == PortalModuleKind.MODULE_BAY_EXPANSION)
                    .map(ignored -> replacement.getCount()).orElse(0);
                int removed = Math.max(0, installed.getCount() - replacementCount);
                if (!PortalGunModules.canRemove(modules, slotId, removed)) {
                    displayExpansionRemovalBlocked(player);
                    return;
                }
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    private Slot playerSlot(Inventory inventory, int slot, int x, int y) {
        return isLockedInventorySlot(slot)
            ? new LockedGunSlot(inventory, slot, x, y)
            : new Slot(inventory, slot, x, y);
    }

    private boolean isLockedInventorySlot(int slot) {
        return "vanilla_inventory".equals(Nbt.getString(gunReference, "Locator"))
            && Nbt.getCompound(gunReference, "Token").contains("Slot")
            && Nbt.getInt(Nbt.getCompound(gunReference, "Token"), "Slot") == slot;
    }

    private static ContainerData serverData(ItemStack gun) {
        return new ContainerData() {
            private ItemContainerContents cachedComponent;
            private PortalModuleRules cachedRules;
            private int cachedInactiveSlots;
            private int cachedUsedSlots;
            private int cachedUnlockedSlots;

            @Override
            public int get(int index) {
                refresh();
                return switch (index) {
                    case DATA_INACTIVE_SLOTS -> cachedInactiveSlots;
                    case DATA_USED_SLOTS -> cachedUsedSlots;
                    case DATA_UNLOCKED_SLOTS -> cachedUnlockedSlots;
                    default -> 0;
                };
            }

            private void refresh() {
                ItemContainerContents component = gun.getOrDefault(
                    PortalGunComponents.MODULES, ItemContainerContents.EMPTY);
                PortalModuleRules rules = PortalModuleRules.current();
                if (component == cachedComponent && rules.equals(cachedRules)) return;
                cachedComponent = component;
                cachedRules = rules;
                var modules = PortalGunModules.load(gun);
                cachedInactiveSlots = PortalGunModules.inactiveSlots(modules, rules);
                cachedUsedSlots = modules.stream()
                    .mapToInt(stack -> stack.isEmpty() ? 0 : 1).sum();
                cachedUnlockedSlots = PortalGunModules.unlockedSlotCount(modules);
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

    private static PortalModuleRules readRules(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? PortalModuleRules.defaults() : PortalModuleRules.load(tag);
    }

    private PortalModuleRules moduleRules() {
        return modules instanceof PortalGunModuleContainer
            ? PortalModuleRules.current() : syncedRules;
    }

    private static void displayExpansionRemovalBlocked(Player player) {
        //? if >=1.21.11 {
        /*if (player.level().isClientSide()) {
        *///?} else {
        if (player.level().isClientSide) {
        //?}
            Msg.displayClientMessage(player, Component.translatable(
                "message.riftgun.modules.clear_expanded_slots"), true);
        }
    }

    private final class ModuleSlot extends Slot {
        private ModuleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!isActive() || !PortalModuleRegistry.isModule(stack)
                || !container.canPlaceItem(getContainerSlot(), stack)) return false;
            ItemStack installed = getItem();
            if (!installed.isEmpty()
                && !ItemStack.isSameItemSameComponents(installed, stack)) return false;
            return PortalGunModules.canGrowStack(installed.getCount(), getMaxStackSize(stack));
        }

        @Override
        public boolean mayPickup(Player player) {
            boolean allowed = super.mayPickup(player)
                && PortalGunModules.maximumRemovableCount(container, getContainerSlot(),
                    getItem().getCount()) > 0;
            if (!allowed) displayExpansionRemovalBlocked(player);
            return allowed;
        }

        @Override
        public Optional<ItemStack> tryRemove(int amount, int limit, Player player) {
            int requested = Math.min(amount, limit);
            int removable = PortalGunModules.maximumRemovableCount(
                container, getContainerSlot(), requested);
            if (removable <= 0) return Optional.empty();
            ItemStack removed = remove(removable);
            if (removed.isEmpty()) return Optional.empty();
            if (getItem().isEmpty()) setByPlayer(ItemStack.EMPTY, removed);
            return Optional.of(removed);
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < unlockedSlots();
        }

        @Override
        public int getMaxStackSize() {
            return hasItem() ? getMaxStackSize(getItem()) : super.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            PortalModuleDefinition definition = PortalModuleRegistry.find(stack).orElse(null);
            if (definition == null) return 0;
            int currentCount = ItemStack.isSameItemSameComponents(getItem(), stack)
                ? getItem().getCount() : 0;
            int remaining = PortalGunModules.remainingCapacity(
                modules, stack, moduleRules());
            return Math.min(stack.getMaxStackSize(), currentCount + remaining);
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
