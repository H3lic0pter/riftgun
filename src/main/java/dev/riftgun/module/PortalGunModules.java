package dev.riftgun.module;

import dev.riftgun.fuel.PortalGunComponents;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class PortalGunModules {
    public static final int BASE_SLOT_COUNT = 9;
    public static final int SLOTS_PER_EXPANSION = 3;
    public static final int MAXIMUM_EXPANSION_MODULES = 6;
    public static final int SLOT_COUNT = BASE_SLOT_COUNT
        + SLOTS_PER_EXPANSION * MAXIMUM_EXPANSION_MODULES;

    public static NonNullList<ItemStack> load(ItemStack gun) {
        PortalModules.bootstrap();
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        gun.getOrDefault(PortalGunComponents.MODULES, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void save(ItemStack gun, NonNullList<ItemStack> items) {
        gun.set(PortalGunComponents.MODULES, ItemContainerContents.fromItems(items));
    }

    public static int installedCount(ItemStack gun, PortalModuleKind kind) {
        return installedCount(load(gun), kind);
    }

    public static int installedCount(Iterable<ItemStack> items, PortalModuleKind kind) {
        PortalModules.bootstrap();
        int count = 0;
        for (ItemStack stack : items) {
            if (PortalModuleRegistry.find(stack).map(definition -> definition.kind() == kind).orElse(false)) count++;
        }
        return count;
    }

    public static int activeCount(ItemStack gun, PortalModuleKind kind, PortalModuleRules rules) {
        return activeCount(load(gun), kind, rules);
    }

    public static int activeCount(Iterable<ItemStack> items, PortalModuleKind kind, PortalModuleRules rules) {
        int maximum = PortalModuleRegistry.find(kind).map(definition -> definition.maximumCount(rules)).orElse(0);
        if (kind != PortalModuleKind.CREATIVE && hasCreativeModule(items)) return maximum;
        int installed = items instanceof NonNullList<ItemStack> list
            ? installedCount(activeItems(list), kind) : installedCount(items, kind);
        return Math.min(installed, maximum);
    }

    public static boolean hasCreativeModule(Iterable<ItemStack> items) {
        return installedCount(items, PortalModuleKind.CREATIVE) > 0;
    }

    public static int unlockedSlotCount(Iterable<ItemStack> items) {
        if (hasCreativeModule(items)) return SLOT_COUNT;
        int expansions = Math.min(MAXIMUM_EXPANSION_MODULES,
            installedCount(items, PortalModuleKind.MODULE_BAY_EXPANSION));
        return slotCountForExpansionModules(expansions);
    }

    public static int slotCountForExpansionModules(int expansionModules) {
        int sanitized = Math.max(0, Math.min(MAXIMUM_EXPANSION_MODULES, expansionModules));
        return BASE_SLOT_COUNT + sanitized * SLOTS_PER_EXPANSION;
    }

    public static boolean canRemove(NonNullList<ItemStack> items, int slot) {
        if (slot < 0 || slot >= items.size() || items.get(slot).isEmpty()) return true;
        if (!isKind(items.get(slot), PortalModuleKind.MODULE_BAY_EXPANSION)) return true;
        NonNullList<ItemStack> remaining = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int index = 0; index < items.size(); index++) {
            if (index != slot) remaining.set(index, items.get(index));
        }
        int unlocked = unlockedSlotCount(remaining);
        for (int index = unlocked; index < remaining.size(); index++) {
            if (!remaining.get(index).isEmpty()) return false;
        }
        return true;
    }

    public static boolean canRemove(Container container, int slot) {
        NonNullList<ItemStack> items = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int index = 0; index < container.getContainerSize(); index++) {
            items.set(index, container.getItem(index));
        }
        return canRemove(items, slot);
    }

    public static int inactiveSlots(Iterable<ItemStack> items, PortalModuleRules rules) {
        PortalModules.bootstrap();
        if (hasCreativeModule(items)) return 0;
        Map<PortalModuleKind, Integer> seen = new EnumMap<>(PortalModuleKind.class);
        int mask = 0;
        int slot = 0;
        for (ItemStack stack : items) {
            var definition = PortalModuleRegistry.find(stack).orElse(null);
            if (definition != null) {
                int count = seen.getOrDefault(definition.kind(), 0);
                if (count >= definition.maximumCount(rules)) mask |= 1 << slot;
                seen.put(definition.kind(), count + 1);
            }
            slot++;
        }
        return mask;
    }

    public static boolean canAdd(Iterable<ItemStack> items, ItemStack candidate, PortalModuleRules rules) {
        PortalModules.bootstrap();
        PortalModuleDefinition definition = PortalModuleRegistry.find(candidate).orElse(null);
        if (definition == null || installedCount(items, definition.kind()) >= definition.maximumCount(rules)) {
            return false;
        }
        if (definition.kind() == PortalModuleKind.DURATION_EXTENSION
            && installedCount(items, PortalModuleKind.DURATION_ETERNAL) > 0) return false;
        if (definition.kind() == PortalModuleKind.DURATION_ETERNAL
            && installedCount(items, PortalModuleKind.DURATION_EXTENSION) > 0) return false;
        return true;
    }

    private static Iterable<ItemStack> activeItems(NonNullList<ItemStack> items) {
        return items.subList(0, Math.min(items.size(), unlockedSlotCount(items)));
    }

    private static boolean isKind(ItemStack stack, PortalModuleKind kind) {
        return PortalModuleRegistry.find(stack).map(definition -> definition.kind() == kind).orElse(false);
    }

    private PortalGunModules() {}
}
