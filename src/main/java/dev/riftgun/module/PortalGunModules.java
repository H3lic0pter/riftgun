package dev.riftgun.module;

import dev.riftgun.fuel.PortalGunComponents;
import dev.riftgun.fuel.PortalGunVisualState;
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
    private static final PortalModuleKind[] MODULE_KINDS = PortalModuleKind.values();

    public static NonNullList<ItemStack> load(ItemStack gun) {
        PortalModules.bootstrap();
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        gun.getOrDefault(PortalGunComponents.MODULES, ItemContainerContents.EMPTY).copyInto(items);
        return items;
    }

    public static void save(ItemStack gun, NonNullList<ItemStack> items) {
        gun.set(PortalGunComponents.MODULES, ItemContainerContents.fromItems(items));
        PortalGunVisualState.refresh(gun);
    }

    public static int installedCount(ItemStack gun, PortalModuleKind kind) {
        return installedCount(load(gun), kind);
    }

    public static int installedCount(Iterable<ItemStack> items, PortalModuleKind kind) {
        PortalModules.bootstrap();
        int count = 0;
        for (ItemStack stack : items) {
            if (PortalModuleRegistry.find(stack).map(definition -> definition.kind() == kind).orElse(false)) {
                count += stack.getCount();
            }
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

    /** Resolves every active module count with one container load and one active-slot pass. */
    public static ActiveCounts activeCounts(ItemStack gun, PortalModuleRules rules) {
        return activeCounts(load(gun), rules);
    }

    static ActiveCounts activeCounts(NonNullList<ItemStack> items, PortalModuleRules rules) {
        PortalModules.bootstrap();
        boolean creative = false;
        int expansions = 0;
        for (ItemStack stack : items) {
            PortalModuleDefinition definition = PortalModuleRegistry.find(stack).orElse(null);
            if (definition == null) continue;
            if (definition.kind() == PortalModuleKind.CREATIVE) creative = true;
            if (definition.kind() == PortalModuleKind.MODULE_BAY_EXPANSION) {
                expansions += stack.getCount();
            }
        }
        int unlocked = creative ? items.size() : slotCountForExpansionModules(expansions);
        int[] installed = new int[MODULE_KINDS.length];
        for (int slot = 0; slot < Math.min(items.size(), unlocked); slot++) {
            PortalModuleDefinition definition = PortalModuleRegistry.find(items.get(slot)).orElse(null);
            if (definition != null) {
                installed[definition.kind().ordinal()] += items.get(slot).getCount();
            }
        }
        int[] active = new int[installed.length];
        for (PortalModuleKind kind : MODULE_KINDS) {
            PortalModuleDefinition definition = PortalModuleRegistry.find(kind).orElse(null);
            int maximum = definition == null ? 0 : definition.maximumCount(rules);
            active[kind.ordinal()] = kind != PortalModuleKind.CREATIVE && creative
                ? maximum : Math.min(installed[kind.ordinal()], maximum);
        }
        return new ActiveCounts(active);
    }

    public static final class ActiveCounts {
        private final int[] counts;

        private ActiveCounts(int[] counts) {
            this.counts = counts;
        }

        public int count(PortalModuleKind kind) {
            return counts[kind.ordinal()];
        }
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
        int count = slot >= 0 && slot < items.size() ? items.get(slot).getCount() : 0;
        return canRemove(items, slot, count);
    }

    public static boolean canRemove(NonNullList<ItemStack> items, int slot, int amount) {
        if (slot < 0 || slot >= items.size() || items.get(slot).isEmpty()) return true;
        if (!isKind(items.get(slot), PortalModuleKind.MODULE_BAY_EXPANSION)) return true;
        if (amount <= 0) return true;
        NonNullList<ItemStack> remaining = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int index = 0; index < items.size(); index++) {
            remaining.set(index, items.get(index).copy());
        }
        remaining.get(slot).shrink(Math.min(amount, remaining.get(slot).getCount()));
        int unlocked = unlockedSlotCount(remaining);
        for (int index = unlocked; index < remaining.size(); index++) {
            if (!remaining.get(index).isEmpty()) return false;
        }
        return true;
    }

    public static boolean canRemove(Container container, int slot) {
        int count = slot >= 0 && slot < container.getContainerSize()
            ? container.getItem(slot).getCount() : 0;
        return canRemove(container, slot, count);
    }

    public static boolean canRemove(Container container, int slot, int amount) {
        NonNullList<ItemStack> items = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int index = 0; index < container.getContainerSize(); index++) {
            items.set(index, container.getItem(index));
        }
        return canRemove(items, slot, amount);
    }

    public static int maximumRemovableCount(Container container, int slot, int requestedAmount) {
        if (slot < 0 || slot >= container.getContainerSize() || requestedAmount <= 0) return 0;
        ItemStack stack = container.getItem(slot);
        int requested = Math.min(requestedAmount, stack.getCount());
        if (!isKind(stack, PortalModuleKind.MODULE_BAY_EXPANSION)) return requested;
        for (int amount = requested; amount > 0; amount--) {
            if (canRemove(container, slot, amount)) return amount;
        }
        return 0;
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
                seen.put(definition.kind(), count + stack.getCount());
            }
            slot++;
        }
        return mask;
    }

    public static boolean canAdd(Iterable<ItemStack> items, ItemStack candidate, PortalModuleRules rules) {
        return remainingCapacity(items, candidate, rules) > 0;
    }

    public static int remainingCapacity(Iterable<ItemStack> items, ItemStack candidate,
                                        PortalModuleRules rules) {
        PortalModules.bootstrap();
        PortalModuleDefinition definition = PortalModuleRegistry.find(candidate).orElse(null);
        if (definition == null) return 0;
        if (definition.kind() == PortalModuleKind.DURATION_EXTENSION
            && installedCount(items, PortalModuleKind.DURATION_ETERNAL) > 0) return 0;
        if (definition.kind() == PortalModuleKind.DURATION_ETERNAL
            && installedCount(items, PortalModuleKind.DURATION_EXTENSION) > 0) return 0;
        return Math.max(0, definition.maximumCount(rules)
            - installedCount(items, definition.kind()));
    }

    public static int remainingCapacity(Container container, ItemStack candidate,
                                        PortalModuleRules rules) {
        NonNullList<ItemStack> items = NonNullList.withSize(
            container.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            items.set(slot, container.getItem(slot));
        }
        return remainingCapacity(items, candidate, rules);
    }

    static boolean canGrowStack(int currentCount, int maximumCount) {
        return maximumCount > currentCount;
    }

    private static Iterable<ItemStack> activeItems(NonNullList<ItemStack> items) {
        return items.subList(0, Math.min(items.size(), unlockedSlotCount(items)));
    }

    private static boolean isKind(ItemStack stack, PortalModuleKind kind) {
        return PortalModuleRegistry.find(stack).map(definition -> definition.kind() == kind).orElse(false);
    }

    private PortalGunModules() {}
}
