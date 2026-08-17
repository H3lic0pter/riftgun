package dev.riftgun.recipe;

import dev.riftgun.core.config.RiftConfigs;
import dev.riftgun.module.PortalModules;
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
        // Non-special with a placeable placementInfo: the recipe book collects
        // the recipe and JEI displays it automatically, matching the 1.21.1 build.
        return false;
    }

    @Override
    public PlacementInfo placementInfo() {
        // createFromOptionals maps each ingredient to its true 3x3 grid slot so
        // the recipe book's auto-place fills the correct positions.
        return PlacementInfo.createFromOptionals(gridIngredients());
    }

    @Override
    public java.util.List<RecipeDisplay> display() {
        // JEI 26's crafting category only renders recipes that expose a shaped or
        // shapeless display; without this the recipe was hidden even when non-special.
        return java.util.List.of(new ShapedCraftingRecipeDisplay(3, 3,
            gridIngredients().stream()
                .map(ingredient -> ingredient.map(Ingredient::display)
                    .orElse(SlotDisplay.Empty.INSTANCE))
                .toList(),
            new SlotDisplay.ItemStackSlotDisplay(
                ItemStackTemplate.fromNonEmptyStack(new ItemStack(PortalModules.ZERO_POINT_FUEL.item().get()))),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }

    /** Single source for the 3x3 grid shared by the recipe book and the JEI display. */
    private static java.util.List<java.util.Optional<Ingredient>> gridIngredients() {
        return java.util.List.of(
            java.util.Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
            java.util.Optional.of(Ingredient.of(Items.DEEPSLATE_EMERALD_ORE)),
            java.util.Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
            java.util.Optional.of(Ingredient.of(Items.HEAVY_CORE)),
            java.util.Optional.of(Ingredient.of(PortalModules.ADVANCED_BASIC_MODULE.get())),
            java.util.Optional.of(Ingredient.of(Items.HEAVY_CORE)),
            java.util.Optional.of(Ingredient.of(Items.NETHERITE_INGOT)),
            java.util.Optional.of(Ingredient.of(Items.NETHER_STAR)),
            java.util.Optional.of(Ingredient.of(Items.NETHERITE_INGOT)));
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
