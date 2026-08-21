package dev.riftgun.fuel;

import dev.riftgun.core.RiftConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier;
*///?} else {
//?}
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PortalFluids {
    private static final DeferredRegister<FluidType> TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RiftConstants.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RiftConstants.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, RiftConstants.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RiftConstants.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> UNSTABLE_TYPE = type("unstable_portal_fluid");
    public static final DeferredHolder<FluidType, FluidType> PORTAL_TYPE = type("portal_fluid");
    public static final DeferredHolder<FluidType, FluidType> DIMENSIONAL_TYPE = type("dimensional_portal_fluid");

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> UNSTABLE =
        FLUIDS.register("unstable_portal_fluid", () -> new BaseFlowingFluid.Source(unstableProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_UNSTABLE =
        FLUIDS.register("flowing_unstable_portal_fluid", () -> new BaseFlowingFluid.Flowing(unstableProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PORTAL =
        FLUIDS.register("portal_fluid", () -> new BaseFlowingFluid.Source(portalProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_PORTAL =
        FLUIDS.register("flowing_portal_fluid", () -> new BaseFlowingFluid.Flowing(portalProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> DIMENSIONAL =
        FLUIDS.register("dimensional_portal_fluid", () -> new BaseFlowingFluid.Source(dimensionalProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_DIMENSIONAL =
        FLUIDS.register("flowing_dimensional_portal_fluid", () -> new BaseFlowingFluid.Flowing(dimensionalProperties()));

    //? if >=1.21.11 {
    /*public static final DeferredHolder<Block, LiquidBlock> UNSTABLE_BLOCK = BLOCKS.register(
        "unstable_portal_fluid", id -> liquidBlock(UNSTABLE.get(), id));
    public static final DeferredHolder<Block, LiquidBlock> PORTAL_BLOCK = BLOCKS.register(
        "portal_fluid", id -> liquidBlock(PORTAL.get(), id));
    public static final DeferredHolder<Block, LiquidBlock> DIMENSIONAL_BLOCK = BLOCKS.register(
        "dimensional_portal_fluid", id -> liquidBlock(DIMENSIONAL.get(), id));
    *///?} else {
    public static final DeferredHolder<Block, LiquidBlock> UNSTABLE_BLOCK = BLOCKS.register(
        "unstable_portal_fluid", () -> liquidBlock(UNSTABLE.get()));
    public static final DeferredHolder<Block, LiquidBlock> PORTAL_BLOCK = BLOCKS.register(
        "portal_fluid", () -> liquidBlock(PORTAL.get()));
    public static final DeferredHolder<Block, LiquidBlock> DIMENSIONAL_BLOCK = BLOCKS.register(
        "dimensional_portal_fluid", () -> liquidBlock(DIMENSIONAL.get()));
    //?}

    //? if >=1.21.11 {
    /*public static final DeferredHolder<Item, BucketItem> UNSTABLE_BUCKET = ITEMS.register(
        "unstable_portal_fluid_bucket", id -> bucket(UNSTABLE.get(), id));
    public static final DeferredHolder<Item, BucketItem> PORTAL_BUCKET = ITEMS.register(
        "portal_fluid_bucket", id -> bucket(PORTAL.get(), id));
    public static final DeferredHolder<Item, BucketItem> DIMENSIONAL_BUCKET = ITEMS.register(
        "dimensional_portal_fluid_bucket", id -> bucket(DIMENSIONAL.get(), id));
    *///?} else {
    public static final DeferredHolder<Item, BucketItem> UNSTABLE_BUCKET = ITEMS.register(
        "unstable_portal_fluid_bucket", () -> bucket(UNSTABLE.get()));
    public static final DeferredHolder<Item, BucketItem> PORTAL_BUCKET = ITEMS.register(
        "portal_fluid_bucket", () -> bucket(PORTAL.get()));
    public static final DeferredHolder<Item, BucketItem> DIMENSIONAL_BUCKET = ITEMS.register(
        "dimensional_portal_fluid_bucket", () -> bucket(DIMENSIONAL.get()));
    //?}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    private static DeferredHolder<FluidType, FluidType> type(String name) {
        return TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid." + RiftConstants.MOD_ID + "." + name)
            //? if >=1.21.11 {
            /*.isWaterLike(true)
            *///?}
            .canConvertToSource(false)
            .canDrown(false)
            .supportsBoating(false)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    }

    private static BaseFlowingFluid.Properties unstableProperties() {
        return properties(UNSTABLE_TYPE, UNSTABLE, FLOWING_UNSTABLE, UNSTABLE_BUCKET, UNSTABLE_BLOCK);
    }

    private static BaseFlowingFluid.Properties portalProperties() {
        return properties(PORTAL_TYPE, PORTAL, FLOWING_PORTAL, PORTAL_BUCKET, PORTAL_BLOCK);
    }

    private static BaseFlowingFluid.Properties dimensionalProperties() {
        return properties(DIMENSIONAL_TYPE, DIMENSIONAL, FLOWING_DIMENSIONAL,
            DIMENSIONAL_BUCKET, DIMENSIONAL_BLOCK);
    }

    private static BaseFlowingFluid.Properties properties(
        DeferredHolder<FluidType, FluidType> type,
        DeferredHolder<Fluid, ? extends Fluid> source,
        DeferredHolder<Fluid, ? extends Fluid> flowing,
        DeferredHolder<Item, ? extends Item> bucket,
        DeferredHolder<Block, ? extends LiquidBlock> block
    ) {
        return new BaseFlowingFluid.Properties(type::get, source::get, flowing::get)
            .bucket(bucket::get)
            .block(block::get)
            .slopeFindDistance(4)
            .levelDecreasePerBlock(1)
            .tickRate(5);
    }

    //? if >=1.21.11 {
    /*private static LiquidBlock liquidBlock(Fluid fluid, Identifier id) {
        return new LiquidBlock((BaseFlowingFluid) fluid,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()
                .setId(ResourceKey.create(Registries.BLOCK, id)));
    }
    *///?} else {
    private static LiquidBlock liquidBlock(Fluid fluid) {
        return new LiquidBlock((BaseFlowingFluid) fluid,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable());
    }
    //?}

    //? if >=1.21.11 {
    /*private static BucketItem bucket(Fluid fluid, Identifier id) {
        return new BucketItem(fluid, new Item.Properties()
            .craftRemainder(Items.BUCKET).stacksTo(1)
            .setId(ResourceKey.create(Registries.ITEM, id)));
    }
    *///?} else {
    private static BucketItem bucket(Fluid fluid) {
        return new BucketItem(fluid, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
    }
    //?}

    private PortalFluids() {}
}
