package dev.riftgun.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record FluidTransmutationInput(List<ItemStack> items) implements RecipeInput {
    public FluidTransmutationInput {
        items = List.copyOf(items);
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
