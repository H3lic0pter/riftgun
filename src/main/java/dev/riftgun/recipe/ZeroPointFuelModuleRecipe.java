package dev.riftgun.recipe;

import dev.riftgun.config.ServerConfig;
import dev.riftgun.module.PortalModules;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class ZeroPointFuelModuleRecipe extends CustomRecipe {
    public ZeroPointFuelModuleRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!ServerConfig.VALUES.enableZeroPointFuelRecipe.get()
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(PortalModules.ZERO_POINT_FUEL.item().get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(Items.NETHERITE_INGOT));
        ingredients.add(Ingredient.of(Items.DEEPSLATE_EMERALD_ORE));
        ingredients.add(Ingredient.of(Items.NETHERITE_INGOT));
        ingredients.add(Ingredient.of(Items.HEAVY_CORE));
        ingredients.add(Ingredient.of(PortalModules.ADVANCED_BASIC_MODULE.get()));
        ingredients.add(Ingredient.of(Items.HEAVY_CORE));
        ingredients.add(Ingredient.of(Items.NETHERITE_INGOT));
        ingredients.add(Ingredient.of(Items.NETHER_STAR));
        ingredients.add(Ingredient.of(Items.NETHERITE_INGOT));
        return ingredients;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(PortalModules.ZERO_POINT_FUEL.item().get());
    }

    @Override
    public boolean isSpecial() {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RiftGunRecipes.ZERO_POINT_FUEL_MODULE_SERIALIZER.get();
    }
}
