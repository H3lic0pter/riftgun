package dev.riftgun.recipe;

import dev.riftgun.fuel.PortalFluids;
import dev.riftgun.module.PortalModules;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class AdvancedBasicModuleRecipe extends CustomRecipe {
    private static final AdvancedBasicModuleRecipe INSTANCE = new AdvancedBasicModuleRecipe();
    private static final com.mojang.serialization.MapCodec<AdvancedBasicModuleRecipe> MAP_CODEC =
        com.mojang.serialization.MapCodec.unit(() -> INSTANCE);
    private static final net.minecraft.network.codec.StreamCodec<
        net.minecraft.network.RegistryFriendlyByteBuf, AdvancedBasicModuleRecipe> STREAM_CODEC =
        net.minecraft.network.codec.StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<AdvancedBasicModuleRecipe> SERIALIZER =
        new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public AdvancedBasicModuleRecipe() {
        super();
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        return input.getItem(0, 0).is(PortalFluids.DIMENSIONAL_BUCKET.get())
            && input.getItem(1, 0).is(Items.DRAGON_EGG)
            && input.getItem(2, 0).is(PortalFluids.DIMENSIONAL_BUCKET.get())
            && input.getItem(0, 1).is(Items.POWDER_SNOW_BUCKET)
            && input.getItem(1, 1).is(PortalModules.BASIC_MODULE.get())
            && input.getItem(2, 1).is(Items.POWDER_SNOW_BUCKET)
            && input.getItem(0, 2).is(PortalFluids.DIMENSIONAL_BUCKET.get())
            && input.getItem(1, 2).isEmpty()
            && input.getItem(2, 2).is(PortalFluids.DIMENSIONAL_BUCKET.get());
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return new ItemStack(PortalModules.ADVANCED_BASIC_MODULE.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = super.getRemainingItems(input);
        remaining.set(1, new ItemStack(Items.DRAGON_EGG));
        return remaining;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(java.util.List.of(
            Ingredient.of(PortalFluids.DIMENSIONAL_BUCKET.get()),
            Ingredient.of(Items.DRAGON_EGG),
            Ingredient.of(Items.POWDER_SNOW_BUCKET),
            Ingredient.of(PortalModules.BASIC_MODULE.get())));
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
