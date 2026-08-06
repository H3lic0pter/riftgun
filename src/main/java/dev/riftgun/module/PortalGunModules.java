package dev.riftgun.module;

import dev.riftgun.fuel.PortalGunComponents;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class PortalGunModules {
    public static final int SLOT_COUNT = 9;

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
        int installed = installedCount(gun, kind);
        int maximum = PortalModuleRegistry.find(kind).map(definition -> definition.maximumCount(rules)).orElse(0);
        return Math.min(installed, maximum);
    }

    public static int inactiveSlots(Iterable<ItemStack> items, PortalModuleRules rules) {
        PortalModules.bootstrap();
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
        return definition != null
            && installedCount(items, definition.kind()) < definition.maximumCount(rules);
    }

    private PortalGunModules() {}
}
