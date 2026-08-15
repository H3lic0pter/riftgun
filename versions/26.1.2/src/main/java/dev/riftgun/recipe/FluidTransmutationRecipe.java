package dev.riftgun.recipe;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidTransmutationRecipe(List<SizedIngredient> ingredients, FluidStack result)
    implements Recipe<FluidTransmutationInput> {
    public static final int SOURCE_AMOUNT = 1000;
    private static final int MAXIMUM_INGREDIENT_UNITS = 64;
    private static final com.mojang.serialization.Codec<List<SizedIngredient>> INGREDIENTS_CODEC =
        SizedIngredient.NESTED_CODEC.listOf().validate(FluidTransmutationRecipe::validateIngredients);

    public FluidTransmutationRecipe {
        ingredients = List.copyOf(ingredients);
        result = result.copy();
        validateIngredients(ingredients).getOrThrow();
        if (result.isEmpty() || result.getAmount() != SOURCE_AMOUNT) {
            throw new IllegalArgumentException("fluid transmutation result must be exactly 1000 mB");
        }
    }

    public Optional<int[]> consumptionPlan(List<ItemStack> stacks) {
        return FluidTransmutationMatcher.plan(ingredients, stacks);
    }

    public int totalIngredientCount() {
        return ingredients.stream().mapToInt(SizedIngredient::count).sum();
    }

    @Override
    public FluidStack result() {
        return result.copy();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public boolean matches(FluidTransmutationInput input, Level level) {
        return consumptionPlan(input.items()).isPresent();
    }

    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public ItemStack assemble(FluidTransmutationInput input) {
        return new ItemStack(result.getFluid().getBucket());
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<? extends Recipe<FluidTransmutationInput>> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<FluidTransmutationRecipe> getType() {
        return RiftGunRecipes.FLUID_TRANSMUTATION_TYPE.get();
    }

    private static DataResult<List<SizedIngredient>> validateIngredients(List<SizedIngredient> ingredients) {
        if (ingredients.isEmpty()) return DataResult.error(() -> "at least one ingredient is required");
        int total = ingredients.stream().mapToInt(SizedIngredient::count).sum();
        if (total > MAXIMUM_INGREDIENT_UNITS) {
            return DataResult.error(() -> "ingredient total exceeds " + MAXIMUM_INGREDIENT_UNITS);
        }
        return DataResult.success(ingredients);
    }

    private static final MapCodec<FluidTransmutationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(FluidTransmutationRecipe::ingredients),
            FluidStack.fixedAmountCodec(SOURCE_AMOUNT).fieldOf("result")
                .forGetter(FluidTransmutationRecipe::result)
        ).apply(instance, FluidTransmutationRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, FluidTransmutationRecipe> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public FluidTransmutationRecipe decode(RegistryFriendlyByteBuf buffer) {
                int size = buffer.readVarInt();
                if (size <= 0 || size > MAXIMUM_INGREDIENT_UNITS) {
                    throw new IllegalArgumentException("invalid fluid transmutation ingredient count: " + size);
                }
                List<SizedIngredient> ingredients = new java.util.ArrayList<>(size);
                for (int index = 0; index < size; index++) {
                    ingredients.add(SizedIngredient.STREAM_CODEC.decode(buffer));
                }
                return new FluidTransmutationRecipe(ingredients,
                    FluidStack.STREAM_CODEC.decode(buffer));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, FluidTransmutationRecipe recipe) {
                buffer.writeVarInt(recipe.ingredients.size());
                recipe.ingredients.forEach(ingredient ->
                    SizedIngredient.STREAM_CODEC.encode(buffer, ingredient));
                FluidStack.STREAM_CODEC.encode(buffer, recipe.result);
            }
        };
    public static final RecipeSerializer<FluidTransmutationRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);
}
