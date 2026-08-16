package dev.riftgun.recipe;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.module.PortalModules;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ZeroPointFuelModuleRecipe extends CustomRecipe {
    private static final ZeroPointFuelModuleRecipe INSTANCE = new ZeroPointFuelModuleRecipe();
    private static final com.mojang.serialization.MapCodec<ZeroPointFuelModuleRecipe> MAP_CODEC =
        com.mojang.serialization.MapCodec.unit(() -> INSTANCE);
    private static final net.minecraft.network.codec.StreamCodec<
        net.minecraft.network.RegistryFriendlyByteBuf, ZeroPointFuelModuleRecipe> STREAM_CODEC =
        net.minecraft.network.codec.StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ZeroPointFuelModuleRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public ZeroPointFuelModuleRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!RiftConfigs.server().modules().zeroPointFuelRecipeEnabled()
            || input.width() != 3 || input.height() != 3) return false;
        return input.getItem(0, 0).is(Items.NETHERITE_INGOT)
            && input.getItem(1, 0).is(Items.DEEPSLATE_EMERALD_ORE)
            && input.getItem(2, 0).is(Items.NETHERITE_INGOT)
            && input.getItem(0, 1).is(Items.HEAVY_CORE)
            && input.getItem(1, 1).is(PortalModules.ADVANCED_BASIC_MODULE.get())
            && input.getItem(2, 1).is(Items.HEAVY_CORE)
            && input.getItem(0, 2).is(Items.NETHERITE_INGOT)
            && input.getItem(1, 2).is(Items.NETHER_STAR)
            && input.getItem(2, 2).is(Items.NETHERITE_INGOT);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(PortalModules.ZERO_POINT_FUEL.item().get());
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(java.util.List.of(
            Ingredient.of(Items.NETHERITE_INGOT),
            Ingredient.of(Items.DEEPSLATE_EMERALD_ORE),
            Ingredient.of(Items.HEAVY_CORE),
            Ingredient.of(PortalModules.ADVANCED_BASIC_MODULE.get()),
            Ingredient.of(Items.NETHER_STAR)));
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
