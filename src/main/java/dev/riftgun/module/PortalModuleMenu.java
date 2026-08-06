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
    public static final int DATA_CAPACITY = 0;
    public static final int DATA_CONFIGURED_RANGE = 1;
    public static final int DATA_MAXIMUM_RANGE = 2;
    public static final int DATA_ENTITY_MASK = 3;
    public static final int DATA_COORDINATE = 4;
    public static final int DATA_INACTIVE_SLOTS = 5;
    public static final int DATA_USED_SLOTS = 6;
    private static final int DATA_COUNT = 7;

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
            locatedGun.saveReference(), serverData(locatedGun.stack(), inventory.player));
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
            addSlot(new ModuleSlot(modules, slot, 8 + slot * 18, 35));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int inventorySlot = column + row * 9 + 9;
                addSlot(playerSlot(inventory, inventorySlot, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(playerSlot(inventory, column, 8 + column * 18, 142));
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

    public int capacity() {
        return data.get(DATA_CAPACITY);
    }

    public int configuredRange() {
        return data.get(DATA_CONFIGURED_RANGE);
    }

    public int maximumRange() {
        return data.get(DATA_MAXIMUM_RANGE);
    }

    public int entityMask() {
        return data.get(DATA_ENTITY_MASK);
    }

    public boolean coordinateUnlocked() {
        return data.get(DATA_COORDINATE) != 0;
    }

    public int inactiveSlots() {
        return data.get(DATA_INACTIVE_SLOTS);
    }

    public int usedSlots() {
        return data.get(DATA_USED_SLOTS);
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
                || !moveItemStackTo(source, 0, MODULE_SLOT_COUNT, false)) return ItemStack.EMPTY;
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

    private static ContainerData serverData(ItemStack gun, Player player) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                PortalGunCapabilities capabilities = PortalGunCapabilities.resolve(
                    gun, PortalDataStore.load(player).settings().smartDistance());
                return switch (index) {
                    case DATA_CAPACITY -> capabilities.nominalCapacity();
                    case DATA_CONFIGURED_RANGE -> capabilities.configuredSurfaceRange();
                    case DATA_MAXIMUM_RANGE -> capabilities.maximumSurfaceRange();
                    case DATA_ENTITY_MASK -> capabilities.entityAccess().mask();
                    case DATA_COORDINATE -> capabilities.coordinateOverride() ? 1 : 0;
                    case DATA_INACTIVE_SLOTS -> PortalGunModules.inactiveSlots(
                        PortalGunModules.load(gun), PortalModuleRules.current());
                    case DATA_USED_SLOTS -> PortalGunModules.load(gun).stream()
                        .mapToInt(stack -> stack.isEmpty() ? 0 : 1).sum();
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

    private static final class ModuleSlot extends Slot {
        private ModuleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
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
