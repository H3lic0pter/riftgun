package dev.riftgun.recipe;

import dev.riftgun.fuel.PortalFluids;
import dev.riftgun.module.PortalModules;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
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
        // Non-special with a placeable placementInfo: the recipe book collects
        // the recipe and JEI displays it automatically, matching the 1.21.1 build.
        return false;
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
    public java.util.List<RecipeDisplay> display() {
        // JEI 26's crafting category only renders recipes that expose a shaped or
        // shapeless display; without this the recipe was hidden even when non-special.
        return java.util.List.of(new ShapedCraftingRecipeDisplay(3, 3, java.util.List.of(
            new SlotDisplay.ItemSlotDisplay(PortalFluids.DIMENSIONAL_BUCKET.get()),
            new SlotDisplay.ItemSlotDisplay(Items.DRAGON_EGG),
            new SlotDisplay.ItemSlotDisplay(PortalFluids.DIMENSIONAL_BUCKET.get()),
            new SlotDisplay.ItemSlotDisplay(Items.POWDER_SNOW_BUCKET),
            new SlotDisplay.ItemSlotDisplay(PortalModules.BASIC_MODULE.get()),
            new SlotDisplay.ItemSlotDisplay(Items.POWDER_SNOW_BUCKET),
            new SlotDisplay.ItemSlotDisplay(PortalFluids.DIMENSIONAL_BUCKET.get()),
            SlotDisplay.Empty.INSTANCE,
            new SlotDisplay.ItemSlotDisplay(PortalFluids.DIMENSIONAL_BUCKET.get())
        ), new SlotDisplay.ItemStackSlotDisplay(
            ItemStackTemplate.fromNonEmptyStack(new ItemStack(PortalModules.ADVANCED_BASIC_MODULE.get()))),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
