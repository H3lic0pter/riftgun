package dev.riftgun.client.recipe;

import dev.riftgun.recipe.FluidTransmutationRecipe;
import dev.riftgun.recipe.RiftGunRecipes;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Client-side copy of the fluid-transmutation recipes from the server's synced
 * recipe map ({@link RecipesReceivedEvent}). The server advertises the recipe
 * type via {@code OnDatapackSyncEvent}, so the synced map carries the recipes;
 * this mirrors AE2's approach of keeping the synced map gated on the type.
 */
public final class FluidRecipeCache {
    private static volatile List<RecipeHolder<FluidTransmutationRecipe>> recipes;

    /** @return number of fluid recipes found in the synced map. */
    public static int setFrom(RecipeMap recipeMap, Set<RecipeType<?>> recipeTypes) {
        if (!recipeTypes.contains(RiftGunRecipes.FLUID_TRANSMUTATION_TYPE.get())) {
            recipes = null;
            return 0;
        }
        List<RecipeHolder<FluidTransmutationRecipe>> matched = recipeMap.values().stream()
            .filter(holder -> holder.value() instanceof FluidTransmutationRecipe)
            .map(holder -> (RecipeHolder<FluidTransmutationRecipe>) (RecipeHolder<?>) holder)
            .toList();
        recipes = List.copyOf(matched);
        return matched.size();
    }

    /** @return the synced recipes, or null before the server has synced them. */
    public static List<RecipeHolder<FluidTransmutationRecipe>> get() {
        return recipes;
    }

    private FluidRecipeCache() {}
}
