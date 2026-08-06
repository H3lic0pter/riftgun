package dev.riftgun.module;

import dev.riftgun.RiftGun;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PortalModules {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RiftGun.MOD_ID);

    public static final ModuleEntry COORDINATE_OVERRIDE = register(
        "coordinate_override_module", PortalModuleKind.COORDINATE_OVERRIDE, ignored -> 1, 0x74D9E8);
    public static final ModuleEntry RESERVOIR_EXPANSION = register(
        "reservoir_expansion_module", PortalModuleKind.RESERVOIR_EXPANSION,
        PortalModuleRules::maximumReservoirModules, 0x5BADEB);
    public static final ModuleEntry PASSIVE_TRANSIT = register(
        "passive_transit_module", PortalModuleKind.PASSIVE_TRANSIT, ignored -> 1, 0x8ED081);
    public static final ModuleEntry HOSTILE_TRANSIT = register(
        "hostile_transit_module", PortalModuleKind.HOSTILE_TRANSIT, ignored -> 1, 0xDC765F);
    public static final ModuleEntry BOSS_TRANSIT = register(
        "boss_transit_module", PortalModuleKind.BOSS_TRANSIT, ignored -> 1, 0xA77BD6);
    public static final ModuleEntry SURFACE_RANGE = register(
        "surface_range_amplifier", PortalModuleKind.SURFACE_RANGE,
        PortalModuleRules::maximumSurfaceRangeModules, 0xE3B75C);
    public static final ModuleEntry MODULE_BAY_EXPANSION = register(
        "module_bay_expansion", PortalModuleKind.MODULE_BAY_EXPANSION,
        ignored -> PortalGunModules.MAXIMUM_EXPANSION_MODULES, 0x7CCED8);

    private static ModuleEntry register(String name, PortalModuleKind kind,
                                        ToIntFunction<PortalModuleRules> maximumCount, int accentRgb) {
        DeferredHolder<Item, Item> item = ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
        PortalModuleDefinition definition = PortalModuleRegistry.register(new PortalModuleDefinition(
            ResourceLocation.fromNamespaceAndPath(RiftGun.MOD_ID, name), kind, item, maximumCount, accentRgb));
        return new ModuleEntry(item, definition);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    /** Forces static module definitions to be registered before capability queries. */
    public static void bootstrap() {}

    public record ModuleEntry(DeferredHolder<Item, Item> item, PortalModuleDefinition definition) {}

    private PortalModules() {}
}
