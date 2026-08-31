package dev.riftgun.module;

import dev.riftgun.core.RiftConstants;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PortalModules {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RiftConstants.MOD_ID);

    //? if >=1.21.11 {
    /*public static final DeferredHolder<Item, Item> BASIC_MODULE = ITEMS.register(
        "basic_module", id -> new Item(new Item.Properties().stacksTo(64)
            .setId(ResourceKey.create(Registries.ITEM, id))));
    public static final DeferredHolder<Item, Item> ADVANCED_BASIC_MODULE = ITEMS.register(
        "advanced_basic_module", id -> new Item(new Item.Properties().stacksTo(64)
            .setId(ResourceKey.create(Registries.ITEM, id))));
    *///?} else {
    public static final DeferredHolder<Item, Item> BASIC_MODULE = ITEMS.register(
        "basic_module", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredHolder<Item, Item> ADVANCED_BASIC_MODULE = ITEMS.register(
        "advanced_basic_module", () -> new Item(new Item.Properties().stacksTo(64)));
    //?}

    public static final ModuleEntry COORDINATE_OVERRIDE = register(
        "coordinate_override_module", PortalModuleKind.COORDINATE_OVERRIDE, ignored -> 1, 0x74D9E8);
    public static final ModuleEntry DIMENSIONAL_TRAVERSAL = register(
        "dimensional_traversal_module", PortalModuleKind.DIMENSIONAL_TRAVERSAL, ignored -> 1, 0x8E79E8);
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
    public static final ModuleEntry APERTURE_EXPANSION = register(
        "portal_aperture_module", PortalModuleKind.APERTURE_EXPANSION, ignored -> 1, 0x70D8A8);
    public static final ModuleEntry MODULE_BAY_EXPANSION = register(
        "module_bay_expansion", PortalModuleKind.MODULE_BAY_EXPANSION,
        ignored -> PortalGunModules.MAXIMUM_EXPANSION_MODULES, 0x7CCED8);
    public static final ModuleEntry PLAYER_TARGET = register(
        "player_target_module", PortalModuleKind.PLAYER_TARGET, ignored -> 1, 0x5CC8D9);
    public static final ModuleEntry DURATION_EXTENSION = register(
        "duration_extension_module", PortalModuleKind.DURATION_EXTENSION,
        PortalModuleRules::maximumDurationExtensionModules, 0x8FC7E8);
    public static final ModuleEntry DURATION_ETERNAL = register(
        "duration_eternal_module", PortalModuleKind.DURATION_ETERNAL, ignored -> 1, 0xD8C77F);
    public static final ModuleEntry FALL_GUARD = register(
        "fall_guard_module", PortalModuleKind.FALL_GUARD, ignored -> 1, 0x6FBF73);
    public static final ModuleEntry ENTITY_RELOCATION = register(
        "entity_relocation_module", PortalModuleKind.ENTITY_RELOCATION, ignored -> 1, 0x75D7C8);
    public static final ModuleEntry REMOTE = register(
        "remote_module", PortalModuleKind.REMOTE, ignored -> 1, 0x59DCE8);
    public static final ModuleEntry PRECISION_PLACEMENT = register(
        "precision_placement_module", PortalModuleKind.PRECISION_PLACEMENT, ignored -> 1, 0x8EC9DB);
    public static final ModuleEntry PORTAL_PAIRING = register(
        "portal_pairing_module", PortalModuleKind.PORTAL_PAIRING, ignored -> 1, 0xE19A52);
    public static final ModuleEntry MATTER_ANCHOR = register(
        "matter_anchor_module", PortalModuleKind.MATTER_ANCHOR, ignored -> 1, 0x8A86A8);
    public static final ModuleEntry PROJECTILE_TRANSIT = register(
        "projectile_transit_module", PortalModuleKind.PROJECTILE_TRANSIT, ignored -> 1, 0xD7C65C);
    public static final ModuleEntry ZERO_POINT_FUEL = register(
        "zero_point_fuel_module", PortalModuleKind.ZERO_POINT_FUEL, ignored -> 1, 0x4FCB72);
    public static final ModuleEntry CREATIVE = register(
        "creative_module", PortalModuleKind.CREATIVE, ignored -> 1, 0xE6C85C);

    private static ModuleEntry register(String name, PortalModuleKind kind,
                                        ToIntFunction<PortalModuleRules> maximumCount, int accentRgb) {
        //? if >=1.21.11 {
        /*DeferredHolder<Item, Item> item = ITEMS.register(name, id -> new Item(
            new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, id))));
        *///?} else {
        DeferredHolder<Item, Item> item = ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
        //?}
        PortalModuleDefinition definition = PortalModuleRegistry.register(new PortalModuleDefinition(
//? if >=1.21.11 {
            /*Identifier.fromNamespaceAndPath(RiftConstants.MOD_ID, name), kind, item, maximumCount, accentRgb));
*///?} else {
            ResourceLocation.fromNamespaceAndPath(RiftConstants.MOD_ID, name), kind, item, maximumCount, accentRgb));
//?}
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
